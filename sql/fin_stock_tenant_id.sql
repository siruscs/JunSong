-- FIX: 为 fin_stock_ledger / fin_stock_position / fin_stock_snapshot 补 tenant_id 列（幂等迁移）
-- 背景：TenantSqlInterceptor 会对 fin_ 前缀表自动追加 tenant_id 条件，
-- 但这三张库存表建表 SQL 缺该列，DEV/PROD 运行时查询会报
-- "Unknown column 'fin_stock_ledger.tenant_id' in 'where clause'"。
-- 此脚本幂等：先检查列是否存在，不存在则添加，并回填默认租户 1。

-- ===== fin_stock_ledger =====
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'fin_stock_ledger'
    AND COLUMN_NAME = 'tenant_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE fin_stock_ledger ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
  'SELECT ''fin_stock_ledger.tenant_id already exists'' AS msg'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE fin_stock_ledger SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

-- ===== fin_stock_position =====
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'fin_stock_position'
    AND COLUMN_NAME = 'tenant_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE fin_stock_position ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
  'SELECT ''fin_stock_position.tenant_id already exists'' AS msg'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE fin_stock_position SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

-- ===== fin_stock_snapshot =====
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'fin_stock_snapshot'
    AND COLUMN_NAME = 'tenant_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE fin_stock_snapshot ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
  'SELECT ''fin_stock_snapshot.tenant_id already exists'' AS msg'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE fin_stock_snapshot SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;
