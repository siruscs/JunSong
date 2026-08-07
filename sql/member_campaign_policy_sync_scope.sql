SET NAMES utf8mb4;

-- 为政策套餐补齐机构范围，避免同一政策ID下跨机构串套餐。
SET @has_package_dept := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'mem_campaign_policy_package' AND column_name = 'dept_id'
);
SET @add_package_dept_sql := IF(@has_package_dept = 0,
  'ALTER TABLE mem_campaign_policy_package ADD COLUMN dept_id BIGINT NOT NULL DEFAULT 0 COMMENT ''机构ID'' AFTER tenant_id',
  'SELECT 1');
PREPARE add_package_dept_stmt FROM @add_package_dept_sql;
EXECUTE add_package_dept_stmt;
DEALLOCATE PREPARE add_package_dept_stmt;

SET @has_detail_period := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'mem_config_sync_detail' AND column_name = 'target_period_id'
);
SET @add_detail_period_sql := IF(@has_detail_period = 0,
  'ALTER TABLE mem_config_sync_detail ADD COLUMN target_period_id BIGINT DEFAULT NULL COMMENT ''政策同步目标核算周期ID'' AFTER target_dept_id',
  'SELECT 1');
PREPARE add_detail_period_stmt FROM @add_detail_period_sql;
EXECUTE add_detail_period_stmt;
DEALLOCATE PREPARE add_detail_period_stmt;

SELECT 'member_campaign_policy_sync_scope' AS check_name,
       (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'mem_campaign_policy_package' AND column_name = 'dept_id') AS package_dept_column,
       (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'mem_config_sync_detail' AND column_name = 'target_period_id') AS target_period_column;
