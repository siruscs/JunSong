-- R11-FIX-A: 为 sys_health_rule_config 补 tenant_id 列（幂等迁移）
-- 背景：TenantSqlInterceptor 会对 sys_ 前缀表自动追加 tenant_id 条件，
-- 但建表 SQL 缺该列，DEV/PROD 运行时会被打爆。
-- 此脚本幂等：先检查列是否存在，不存在则添加，并回填默认租户 1。

-- 1. 添加 tenant_id 列（MySQL 8+ 支持 IF NOT EXISTS，兼容老版本用 information_schema 判断）
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_health_rule_config'
    AND COLUMN_NAME = 'tenant_id'
);

SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE sys_health_rule_config ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID'' AFTER rule_id',
  'SELECT ''tenant_id column already exists'' AS msg'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 回填历史数据租户ID为 1（仅对 NULL 或 0 值）
UPDATE sys_health_rule_config SET tenant_id = 1 WHERE tenant_id IS NULL OR tenant_id = 0;

-- 3. 补索引（按租户+规则编码唯一），幂等
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_health_rule_config'
    AND INDEX_NAME = 'uk_health_rule_tenant_code'
);
SET @idx_ddl := IF(@idx_exists = 0,
  'CREATE UNIQUE INDEX uk_health_rule_tenant_code ON sys_health_rule_config (tenant_id, rule_code)',
  'SELECT ''index uk_health_rule_tenant_code already exists'' AS msg'
);
PREPARE stmt2 FROM @idx_ddl;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 4. R11-FIX2-B: 幂等清理旧的单列唯一索引 uk_health_rule_code
--    旧索引会阻止多租户按 (tenant_id, rule_code) 分租户配置（同 rule_code 不同租户会冲突）。
SET @old_idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_health_rule_config'
    AND INDEX_NAME = 'uk_health_rule_code'
);
SET @old_idx_ddl := IF(@old_idx_exists > 0,
  'DROP INDEX uk_health_rule_code ON sys_health_rule_config',
  'SELECT ''index uk_health_rule_code does not exist'' AS msg'
);
PREPARE stmt3 FROM @old_idx_ddl;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;
