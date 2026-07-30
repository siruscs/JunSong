SET NAMES utf8mb4;

-- ============================================================
-- 高风险业务 DB 唯一键兜底迁移（可重复执行、租户隔离、非破坏）
--
-- 目的：为销售/库存/付款/过账/冲销等高风险业务添加数据库层唯一约束，
--      即使幂等框架（AOP + sys_idempotency_record）失效，DB 唯一索引仍能
--      阻止重复写入，实现"幂等键 + 业务状态机 + DB 唯一索引"三层兜底。
--
-- 设计原则：
--   1. 所有 ALTER 使用 information_schema 守卫，可重复执行；
--   2. 唯一索引组合 (tenant_id, idempotency_key)：MySQL 唯一索引允许多个 NULL，
--      旧数据 idempotency_key 为 NULL 不会冲突；
--   3. 新写入必须由 Service 层填充 idempotency_key，否则 DB 约束无法生效；
--   4. 唯一约束冲突时 Service 层应捕获 DuplicateKeyException 并返回原结果或友好错误。
--
-- 涉及表：
--   - fin_stock_ledger         库存流水（销售出库/采购入库/盘点调整/冲销）
--   - fin_accounting_period    会计期间结转/回退
--   - fin_cost_accounting      成本核算
--   - fin_investor_payment     投资人返款
--   - finance_stocktake        盘点整单冲销（补齐 reverse_idempotency_key 列与约束）
-- ============================================================


-- ==========================================================================
-- 1. fin_stock_ledger：库存流水幂等键
--    业务来源：销售出库/采购入库/盘点调整/退货入库/冲销
--    幂等键来源：reference_type + reference_id（同一单据同一类型只能生成一条流水）
--    兜底约束：(tenant_id, idempotency_key) 唯一
-- ==========================================================================

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_stock_ledger'
    AND COLUMN_NAME = 'idempotency_key');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE fin_stock_ledger ADD COLUMN idempotency_key VARCHAR(96) DEFAULT NULL COMMENT ''幂等键（租户内唯一，由 Service 层填充：reference_type+reference_id）''',
    'SELECT ''fin_stock_ledger.idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_stock_ledger'
    AND INDEX_NAME = 'uk_stock_ledger_idempotency_key');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE fin_stock_ledger ADD UNIQUE KEY uk_stock_ledger_idempotency_key (tenant_id, idempotency_key)',
    'SELECT ''uk_stock_ledger_idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ==========================================================================
-- 2. fin_accounting_period：会计期间结转幂等键
--    业务来源：期间结转/期间回退（每个 dept_id 同一动作只能执行一次）
--    幂等键来源：dept_id + 动作（CARRY_FORWARD / ROLLBACK）+ periodId
--    兜底约束：(tenant_id, dept_id, carry_forward_idempotency_key) 唯一
--    注：fin_accounting_period 无 tenant_id 列时使用 (dept_id, carry_forward_idempotency_key)
-- ==========================================================================

-- 2.1 若 fin_accounting_period 缺 tenant_id 列，补齐（多租户隔离必需）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_accounting_period'
    AND COLUMN_NAME = 'tenant_id');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE fin_accounting_period ADD COLUMN tenant_id BIGINT DEFAULT NULL COMMENT ''租户ID（多租户隔离）''',
    'SELECT ''fin_accounting_period.tenant_id already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 添加 carry_forward_idempotency_key 列
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_accounting_period'
    AND COLUMN_NAME = 'carry_forward_idempotency_key');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE fin_accounting_period ADD COLUMN carry_forward_idempotency_key VARCHAR(96) DEFAULT NULL COMMENT ''结转/回退幂等键（租户+门店+动作+期间内唯一）''',
    'SELECT ''fin_accounting_period.carry_forward_idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.3 添加唯一约束
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_accounting_period'
    AND INDEX_NAME = 'uk_accounting_period_carry_forward_key');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE fin_accounting_period ADD UNIQUE KEY uk_accounting_period_carry_forward_key (tenant_id, dept_id, carry_forward_idempotency_key)',
    'SELECT ''uk_accounting_period_carry_forward_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ==========================================================================
-- 3. fin_cost_accounting：成本核算幂等键
--    业务来源：成本核算单创建/更新
--    幂等键来源：核算单创建请求的幂等键
--    兜底约束：(tenant_id, idempotency_key) 唯一
-- ==========================================================================

-- 3.1 若 fin_cost_accounting 缺 tenant_id 列，补齐
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_cost_accounting'
    AND COLUMN_NAME = 'tenant_id');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE fin_cost_accounting ADD COLUMN tenant_id BIGINT DEFAULT NULL COMMENT ''租户ID（多租户隔离）''',
    'SELECT ''fin_cost_accounting.tenant_id already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3.2 添加 idempotency_key 列
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_cost_accounting'
    AND COLUMN_NAME = 'idempotency_key');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE fin_cost_accounting ADD COLUMN idempotency_key VARCHAR(96) DEFAULT NULL COMMENT ''幂等键（租户内唯一）''',
    'SELECT ''fin_cost_accounting.idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3.3 添加唯一约束
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_cost_accounting'
    AND INDEX_NAME = 'uk_cost_accounting_idempotency_key');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE fin_cost_accounting ADD UNIQUE KEY uk_cost_accounting_idempotency_key (tenant_id, idempotency_key)',
    'SELECT ''uk_cost_accounting_idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ==========================================================================
-- 4. fin_investor_payment：投资人返款幂等键
--    业务来源：投资人返款单创建/更新
--    幂等键来源：返款单创建请求的幂等键
--    兜底约束：(tenant_id, idempotency_key) 唯一
-- ==========================================================================

-- 4.1 若 fin_investor_payment 缺 tenant_id 列，补齐
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_investor_payment'
    AND COLUMN_NAME = 'tenant_id');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE fin_investor_payment ADD COLUMN tenant_id BIGINT DEFAULT NULL COMMENT ''租户ID（多租户隔离）''',
    'SELECT ''fin_investor_payment.tenant_id already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.2 添加 idempotency_key 列
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_investor_payment'
    AND COLUMN_NAME = 'idempotency_key');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE fin_investor_payment ADD COLUMN idempotency_key VARCHAR(96) DEFAULT NULL COMMENT ''幂等键（租户内唯一）''',
    'SELECT ''fin_investor_payment.idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.3 添加唯一约束
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_investor_payment'
    AND INDEX_NAME = 'uk_investor_payment_idempotency_key');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE fin_investor_payment ADD UNIQUE KEY uk_investor_payment_idempotency_key (tenant_id, idempotency_key)',
    'SELECT ''uk_investor_payment_idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ==========================================================================
-- 5. finance_stocktake：盘点整单冲销幂等键
--    业务来源：盘点任务整单冲销（status: POSTED → REVERSED）
--    幂等键来源：冲销请求的 idempotencyKey（StocktakeReverseRequest）
--    兜底约束：(tenant_id, reverse_idempotency_key) 唯一
--
--    修复历史缺陷：
--    - Java domain/mapper 自 reverse_idempotency_key 字段，但 finance_stocktake_closure.sql
--      建表 SQL 未包含此列，finance_stocktake_workflow_columns.sql 中 ALTER 使用
--      AFTER reverse_idempotency_key 假设列已存在——存在 schema 缺陷。
--    - 本迁移补齐 reverse_idempotency_key 列与唯一约束。
-- ==========================================================================

-- 5.1 添加 reverse_idempotency_key 列（如果不存在）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_stocktake'
    AND COLUMN_NAME = 'reverse_idempotency_key');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE finance_stocktake ADD COLUMN reverse_idempotency_key VARCHAR(96) DEFAULT NULL COMMENT ''冲销幂等键（租户内唯一）''',
    'SELECT ''finance_stocktake.reverse_idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5.2 添加唯一约束
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_stocktake'
    AND INDEX_NAME = 'uk_reverse_idempotency_key');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE finance_stocktake ADD UNIQUE KEY uk_reverse_idempotency_key (tenant_id, reverse_idempotency_key)',
    'SELECT ''uk_reverse_idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ==========================================================================
-- 6. 对账输出：列与约束汇总
-- ==========================================================================

SELECT '高风险表幂等列汇总' AS reconciliation_type, TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND (
        (TABLE_NAME = 'fin_stock_ledger' AND COLUMN_NAME = 'idempotency_key')
        OR (TABLE_NAME = 'fin_accounting_period' AND COLUMN_NAME IN ('tenant_id', 'carry_forward_idempotency_key'))
        OR (TABLE_NAME = 'fin_cost_accounting' AND COLUMN_NAME IN ('tenant_id', 'idempotency_key'))
        OR (TABLE_NAME = 'fin_investor_payment' AND COLUMN_NAME IN ('tenant_id', 'idempotency_key'))
        OR (TABLE_NAME = 'finance_stocktake' AND COLUMN_NAME = 'reverse_idempotency_key')
    )
    ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT '高风险表幂等唯一约束汇总' AS reconciliation_type, TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND INDEX_NAME IN (
        'uk_stock_ledger_idempotency_key',
        'uk_accounting_period_carry_forward_key',
        'uk_cost_accounting_idempotency_key',
        'uk_investor_payment_idempotency_key',
        'uk_reverse_idempotency_key'
    )
    AND NON_UNIQUE = 0
    GROUP BY TABLE_NAME, INDEX_NAME
    ORDER BY TABLE_NAME;


-- ==========================================================================
-- 7. 重复数据预扫描（迁移前应人工核查）
--    若存在重复，需先清理数据再加约束，否则 ALTER 会失败。
-- ==========================================================================

SELECT 'fin_stock_ledger 幂等键重复检测' AS reconciliation_type, COUNT(*) AS duplicate_count
    FROM (SELECT idempotency_key FROM fin_stock_ledger
          WHERE idempotency_key IS NOT NULL
          GROUP BY tenant_id, idempotency_key HAVING COUNT(*) > 1) t;

SELECT 'fin_accounting_period 结转幂等键重复检测' AS reconciliation_type, COUNT(*) AS duplicate_count
    FROM (SELECT carry_forward_idempotency_key FROM fin_accounting_period
          WHERE carry_forward_idempotency_key IS NOT NULL
          GROUP BY tenant_id, dept_id, carry_forward_idempotency_key HAVING COUNT(*) > 1) t;

SELECT 'fin_cost_accounting 幂等键重复检测' AS reconciliation_type, COUNT(*) AS duplicate_count
    FROM (SELECT idempotency_key FROM fin_cost_accounting
          WHERE idempotency_key IS NOT NULL
          GROUP BY tenant_id, idempotency_key HAVING COUNT(*) > 1) t;

SELECT 'fin_investor_payment 幂等键重复检测' AS reconciliation_type, COUNT(*) AS duplicate_count
    FROM (SELECT idempotency_key FROM fin_investor_payment
          WHERE idempotency_key IS NOT NULL
          GROUP BY tenant_id, idempotency_key HAVING COUNT(*) > 1) t;

SELECT 'finance_stocktake 冲销幂等键重复检测' AS reconciliation_type, COUNT(*) AS duplicate_count
    FROM (SELECT reverse_idempotency_key FROM finance_stocktake
          WHERE reverse_idempotency_key IS NOT NULL
          GROUP BY tenant_id, reverse_idempotency_key HAVING COUNT(*) > 1) t;


-- ==========================================================================
-- 回滚说明（不执行，仅备查）：
--   ALTER TABLE fin_stock_ledger         DROP INDEX uk_stock_ledger_idempotency_key;
--   ALTER TABLE fin_stock_ledger         DROP COLUMN idempotency_key;
--   ALTER TABLE fin_accounting_period    DROP INDEX uk_accounting_period_carry_forward_key;
--   ALTER TABLE fin_accounting_period    DROP COLUMN carry_forward_idempotency_key;
--   ALTER TABLE fin_cost_accounting      DROP INDEX uk_cost_accounting_idempotency_key;
--   ALTER TABLE fin_cost_accounting      DROP COLUMN idempotency_key;
--   ALTER TABLE fin_investor_payment     DROP INDEX uk_investor_payment_idempotency_key;
--   ALTER TABLE fin_investor_payment     DROP COLUMN idempotency_key;
--   ALTER TABLE finance_stocktake        DROP INDEX uk_reverse_idempotency_key;
--   ALTER TABLE finance_stocktake        DROP COLUMN reverse_idempotency_key;
-- ==========================================================================
