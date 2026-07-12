SET NAMES utf8mb4;

-- ==========================================================================
-- Task 1: 租户安全的库存表结构与幂等迁移
-- 目标：为 fin_stock_ledger / fin_stock_position / fin_stock_snapshot 建立
--       tenant_id 隔离键、报表组合索引与来源对账索引。
-- 原则：非破坏、可重复执行；禁止用 tenant_id = 1 覆盖未知存量数据；
--       无法从来源单据唯一推导租户或存在重复业务键时用 SIGNAL 阻断迁移。
-- ==========================================================================

-- Step 1: 幂等补列（初始允许为空，回填后再收紧为 NOT NULL）
DELIMITER //
DROP PROCEDURE IF EXISTS fin_stock_add_tenant_columns_20260712//
CREATE PROCEDURE fin_stock_add_tenant_columns_20260712()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fin_stock_ledger' AND COLUMN_NAME='tenant_id') THEN
    ALTER TABLE fin_stock_ledger ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER ledger_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fin_stock_position' AND COLUMN_NAME='tenant_id') THEN
    ALTER TABLE fin_stock_position ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER position_id;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='fin_stock_snapshot' AND COLUMN_NAME='tenant_id') THEN
    ALTER TABLE fin_stock_snapshot ADD COLUMN tenant_id BIGINT NULL COMMENT '租户ID' AFTER snapshot_id;
  END IF;
END//
CALL fin_stock_add_tenant_columns_20260712()//
DROP PROCEDURE fin_stock_add_tenant_columns_20260712//
DELIMITER ;

-- Step 2: 仅从来源单据唯一推导租户回填流水（采购单 / 销售单），不做 tenant 兜底
UPDATE fin_stock_ledger l
JOIN fin_purchase p ON p.purchase_id = l.reference_id
SET l.tenant_id = p.tenant_id
WHERE l.reference_type = 'PURCHASE'
  AND (l.tenant_id IS NULL OR l.tenant_id = 0)
  AND p.tenant_id IS NOT NULL AND p.tenant_id <> 0;

UPDATE fin_stock_ledger l
JOIN fin_sale_record s ON s.sale_id = l.reference_id
SET l.tenant_id = s.tenant_id
WHERE l.reference_type = 'SALE'
  AND (l.tenant_id IS NULL OR l.tenant_id = 0)
  AND s.tenant_id IS NOT NULL AND s.tenant_id <> 0;

-- Step 3: 从流水唯一推导租户回填结存与快照（同一 dept+product 仅存在唯一租户时才回填）
UPDATE fin_stock_position pos
JOIN (
  SELECT dept_id, product_id, MIN(tenant_id) AS tenant_id
  FROM fin_stock_ledger
  WHERE tenant_id IS NOT NULL AND tenant_id <> 0
  GROUP BY dept_id, product_id
  HAVING COUNT(DISTINCT tenant_id) = 1
) d ON d.dept_id = pos.dept_id AND d.product_id = pos.product_id
SET pos.tenant_id = d.tenant_id
WHERE pos.tenant_id IS NULL OR pos.tenant_id = 0;

UPDATE fin_stock_snapshot snap
JOIN (
  SELECT dept_id, product_id, MIN(tenant_id) AS tenant_id
  FROM fin_stock_ledger
  WHERE tenant_id IS NOT NULL AND tenant_id <> 0
  GROUP BY dept_id, product_id
  HAVING COUNT(DISTINCT tenant_id) = 1
) d ON d.dept_id = snap.dept_id AND d.product_id = snap.product_id
SET snap.tenant_id = d.tenant_id
WHERE snap.tenant_id IS NULL OR snap.tenant_id = 0;

-- Step 4: 迁移前置校验——无法推导租户或存在重复业务键时阻断
DELIMITER //
DROP PROCEDURE IF EXISTS fin_stock_assert_tenant_ready_20260712//
CREATE PROCEDURE fin_stock_assert_tenant_ready_20260712()
BEGIN
  DECLARE v_zero INT DEFAULT 0;
  DECLARE v_dup_pos INT DEFAULT 0;
  DECLARE v_dup_snap INT DEFAULT 0;
  SELECT
    (SELECT COUNT(*) FROM fin_stock_ledger WHERE tenant_id IS NULL OR tenant_id = 0)
    + (SELECT COUNT(*) FROM fin_stock_position WHERE tenant_id IS NULL OR tenant_id = 0)
    + (SELECT COUNT(*) FROM fin_stock_snapshot WHERE tenant_id IS NULL OR tenant_id = 0)
    INTO v_zero;
  IF v_zero <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='存在无法从来源单据唯一推导租户的库存行，请人工核对后再迁移';
  END IF;
  SELECT COUNT(*) INTO v_dup_pos FROM (
    SELECT tenant_id, dept_id, product_id FROM fin_stock_position
    GROUP BY tenant_id, dept_id, product_id HAVING COUNT(*) > 1
  ) t;
  IF v_dup_pos <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='结存表存在重复 (tenant_id, dept_id, product_id) 业务键';
  END IF;
  SELECT COUNT(*) INTO v_dup_snap FROM (
    SELECT tenant_id, snapshot_date, dept_id, product_id FROM fin_stock_snapshot
    GROUP BY tenant_id, snapshot_date, dept_id, product_id HAVING COUNT(*) > 1
  ) t;
  IF v_dup_snap <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='快照表存在重复 (tenant_id, snapshot_date, dept_id, product_id) 业务键';
  END IF;
END//
CALL fin_stock_assert_tenant_ready_20260712()//
DROP PROCEDURE fin_stock_assert_tenant_ready_20260712//
DELIMITER ;

-- Step 5: 收紧租户列为 NOT NULL，替换旧键为租户安全键并补齐报表索引
DELIMITER //
DROP PROCEDURE IF EXISTS fin_stock_apply_tenant_keys_20260712//
CREATE PROCEDURE fin_stock_apply_tenant_keys_20260712()
BEGIN
  ALTER TABLE fin_stock_ledger MODIFY COLUMN tenant_id BIGINT NOT NULL COMMENT '租户ID';
  ALTER TABLE fin_stock_position MODIFY COLUMN tenant_id BIGINT NOT NULL COMMENT '租户ID';
  ALTER TABLE fin_stock_snapshot MODIFY COLUMN tenant_id BIGINT NOT NULL COMMENT '租户ID';

  -- 结存表：替换旧唯一键为租户安全唯一键
  IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='fin_stock_position' AND INDEX_NAME='uk_stock_position_dept_product') THEN
    ALTER TABLE fin_stock_position DROP INDEX uk_stock_position_dept_product;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='fin_stock_position' AND INDEX_NAME='uk_stock_position_tenant_dept_product') THEN
    ALTER TABLE fin_stock_position ADD UNIQUE KEY uk_stock_position_tenant_dept_product (tenant_id, dept_id, product_id);
  END IF;

  -- 快照表：替换旧唯一键为租户安全唯一键
  IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='fin_stock_snapshot' AND INDEX_NAME='uk_stock_snapshot_date_dept_product') THEN
    ALTER TABLE fin_stock_snapshot DROP INDEX uk_stock_snapshot_date_dept_product;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='fin_stock_snapshot' AND INDEX_NAME='uk_stock_snapshot_tenant_date_dept_product') THEN
    ALTER TABLE fin_stock_snapshot ADD UNIQUE KEY uk_stock_snapshot_tenant_date_dept_product (tenant_id, snapshot_date, dept_id, product_id);
  END IF;

  -- 流水表：替换旧 dept_product 索引，补齐租户报表索引与来源幂等对账索引
  IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='fin_stock_ledger' AND INDEX_NAME='idx_stock_ledger_dept_product') THEN
    ALTER TABLE fin_stock_ledger DROP INDEX idx_stock_ledger_dept_product;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='fin_stock_ledger' AND INDEX_NAME='idx_stock_ledger_tenant_dept_product_time') THEN
    ALTER TABLE fin_stock_ledger ADD KEY idx_stock_ledger_tenant_dept_product_time (tenant_id, dept_id, product_id, create_time);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='fin_stock_ledger' AND INDEX_NAME='idx_stock_ledger_tenant_reference') THEN
    ALTER TABLE fin_stock_ledger ADD KEY idx_stock_ledger_tenant_reference (tenant_id, dept_id, reference_type, reference_id, product_id);
  END IF;
END//
CALL fin_stock_apply_tenant_keys_20260712()//
DROP PROCEDURE fin_stock_apply_tenant_keys_20260712//
DELIMITER ;

-- Step 6: 迁移后对账输出（人工复核用，非破坏只读统计）
SELECT
  (SELECT COUNT(*) FROM fin_stock_ledger WHERE tenant_id IS NULL OR tenant_id = 0)
    + (SELECT COUNT(*) FROM fin_stock_position WHERE tenant_id IS NULL OR tenant_id = 0)
    + (SELECT COUNT(*) FROM fin_stock_snapshot WHERE tenant_id IS NULL OR tenant_id = 0) AS zero_tenant_count,
  (SELECT COUNT(*) FROM fin_stock_position pos
     WHERE NOT EXISTS (SELECT 1 FROM fin_stock_ledger l
       WHERE l.tenant_id = pos.tenant_id AND l.dept_id = pos.dept_id AND l.product_id = pos.product_id)) AS position_without_ledger_count,
  (SELECT COUNT(*) FROM fin_stock_position pos
     JOIN (SELECT tenant_id, dept_id, product_id, SUM(change_quantity) AS net
           FROM fin_stock_ledger GROUP BY tenant_id, dept_id, product_id) l
       ON l.tenant_id = pos.tenant_id AND l.dept_id = pos.dept_id AND l.product_id = pos.product_id
     WHERE l.net <> pos.quantity) AS ledger_position_mismatch_count,
  (SELECT COUNT(*) FROM (SELECT tenant_id, dept_id, product_id FROM fin_stock_position
     GROUP BY tenant_id, dept_id, product_id HAVING COUNT(*) > 1) t) AS duplicate_position_key_count,
  (SELECT COUNT(*) FROM (SELECT tenant_id, snapshot_date, dept_id, product_id FROM fin_stock_snapshot
     GROUP BY tenant_id, snapshot_date, dept_id, product_id HAVING COUNT(*) > 1) t) AS duplicate_snapshot_key_count;
