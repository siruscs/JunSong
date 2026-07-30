SET NAMES utf8mb4;

-- ============================================================
-- 全系统幂等治理列迁移（可重复执行、租户隔离、非破坏）
-- 为财务模块写接口表添加 idempotency_key 字段和唯一约束
-- 已有幂等键的表（finance_stocktake/fin_expense_verify_batch/fin_stock_init_batch）不重复添加
-- ============================================================

-- ---------- 辅助存储过程：幂等添加列 ----------
-- 语法兼容 MySQL 5.7/8.0
-- 用法：CALL add_idempotency_column_if_missing('table_name');

-- ---------- 1. fin_sale_record ----------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_sale_record' AND COLUMN_NAME = 'idempotency_key');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE fin_sale_record ADD COLUMN idempotency_key VARCHAR(96) DEFAULT NULL COMMENT ''幂等键（租户内唯一）''',
    'SELECT ''fin_sale_record.idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_sale_record'
    AND INDEX_NAME = 'uk_sale_idempotency_key');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE fin_sale_record ADD UNIQUE KEY uk_sale_idempotency_key (tenant_id, idempotency_key)',
    'SELECT ''uk_sale_idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 2. fin_sale_payment ----------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_sale_payment' AND COLUMN_NAME = 'idempotency_key');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE fin_sale_payment ADD COLUMN idempotency_key VARCHAR(96) DEFAULT NULL COMMENT ''幂等键（租户内唯一）''',
    'SELECT ''fin_sale_payment.idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_sale_payment'
    AND INDEX_NAME = 'uk_sale_payment_idempotency_key');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE fin_sale_payment ADD UNIQUE KEY uk_sale_payment_idempotency_key (tenant_id, idempotency_key)',
    'SELECT ''uk_sale_payment_idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 3. fin_purchase ----------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_purchase' AND COLUMN_NAME = 'idempotency_key');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE fin_purchase ADD COLUMN idempotency_key VARCHAR(96) DEFAULT NULL COMMENT ''幂等键（租户内唯一）''',
    'SELECT ''fin_purchase.idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_purchase'
    AND INDEX_NAME = 'uk_purchase_idempotency_key');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE fin_purchase ADD UNIQUE KEY uk_purchase_idempotency_key (tenant_id, idempotency_key)',
    'SELECT ''uk_purchase_idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 4. fin_expense ----------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_expense' AND COLUMN_NAME = 'idempotency_key');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE fin_expense ADD COLUMN idempotency_key VARCHAR(96) DEFAULT NULL COMMENT ''幂等键（租户内唯一）''',
    'SELECT ''fin_expense.idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_expense'
    AND INDEX_NAME = 'uk_expense_idempotency_key');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE fin_expense ADD UNIQUE KEY uk_expense_idempotency_key (tenant_id, idempotency_key)',
    'SELECT ''uk_expense_idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 5. fin_advance ----------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_advance' AND COLUMN_NAME = 'idempotency_key');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE fin_advance ADD COLUMN idempotency_key VARCHAR(96) DEFAULT NULL COMMENT ''幂等键（租户内唯一）''',
    'SELECT ''fin_advance.idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_advance'
    AND INDEX_NAME = 'uk_advance_idempotency_key');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE fin_advance ADD UNIQUE KEY uk_advance_idempotency_key (tenant_id, idempotency_key)',
    'SELECT ''uk_advance_idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 6. fin_invest_record ----------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_invest_record' AND COLUMN_NAME = 'idempotency_key');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE fin_invest_record ADD COLUMN idempotency_key VARCHAR(96) DEFAULT NULL COMMENT ''幂等键（租户内唯一）''',
    'SELECT ''fin_invest_record.idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_invest_record'
    AND INDEX_NAME = 'uk_invest_idempotency_key');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE fin_invest_record ADD UNIQUE KEY uk_invest_idempotency_key (tenant_id, idempotency_key)',
    'SELECT ''uk_invest_idempotency_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 7. 重复数据扫描（幂等键冲突检测） ----------
SELECT 'fin_sale_record 幂等键重复检测' AS reconciliation_type, COUNT(*) AS duplicate_count
    FROM (SELECT idempotency_key FROM fin_sale_record WHERE idempotency_key IS NOT NULL
          GROUP BY tenant_id, idempotency_key HAVING COUNT(*) > 1) t;

SELECT 'fin_sale_payment 幂等键重复检测' AS reconciliation_type, COUNT(*) AS duplicate_count
    FROM (SELECT idempotency_key FROM fin_sale_payment WHERE idempotency_key IS NOT NULL
          GROUP BY tenant_id, idempotency_key HAVING COUNT(*) > 1) t;

SELECT 'fin_purchase 幂等键重复检测' AS reconciliation_type, COUNT(*) AS duplicate_count
    FROM (SELECT idempotency_key FROM fin_purchase WHERE idempotency_key IS NOT NULL
          GROUP BY tenant_id, idempotency_key HAVING COUNT(*) > 1) t;

SELECT 'fin_expense 幂等键重复检测' AS reconciliation_type, COUNT(*) AS duplicate_count
    FROM (SELECT idempotency_key FROM fin_expense WHERE idempotency_key IS NOT NULL
          GROUP BY tenant_id, idempotency_key HAVING COUNT(*) > 1) t;

SELECT 'fin_advance 幂等键重复检测' AS reconciliation_type, COUNT(*) AS duplicate_count
    FROM (SELECT idempotency_key FROM fin_advance WHERE idempotency_key IS NOT NULL
          GROUP BY tenant_id, idempotency_key HAVING COUNT(*) > 1) t;

-- ---------- 8. 幂等列添加汇总 ----------
SELECT '幂等列添加汇总' AS reconciliation_type, TABLE_NAME, COLUMN_NAME
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND COLUMN_NAME = 'idempotency_key'
    AND TABLE_NAME IN ('fin_sale_record', 'fin_sale_payment', 'fin_purchase',
        'fin_expense', 'fin_advance', 'fin_invest_record')
    ORDER BY TABLE_NAME;

SELECT '幂等唯一约束汇总' AS reconciliation_type, TABLE_NAME, INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND INDEX_NAME LIKE '%idempotency_key'
    AND NON_UNIQUE = 0
    ORDER BY TABLE_NAME;

-- 回滚说明（不执行）：
-- ALTER TABLE fin_sale_record DROP INDEX uk_sale_idempotency_key;
-- ALTER TABLE fin_sale_record DROP COLUMN idempotency_key;
-- ...（其他表同理）
