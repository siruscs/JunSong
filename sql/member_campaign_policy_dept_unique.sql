SET NAMES utf8mb4;

SET @has_global_policy_unique := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'mem_campaign_policy'
    AND index_name = 'uk_mem_campaign_policy_no'
);
SET @drop_policy_unique_sql := IF(@has_global_policy_unique > 0,
  'ALTER TABLE mem_campaign_policy DROP INDEX uk_mem_campaign_policy_no', 'SELECT 1');
PREPARE drop_policy_unique_stmt FROM @drop_policy_unique_sql;
EXECUTE drop_policy_unique_stmt;
DEALLOCATE PREPARE drop_policy_unique_stmt;

SET @has_scoped_policy_unique := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'mem_campaign_policy'
    AND index_name = 'uk_mem_campaign_policy_dept_no'
);
SET @add_policy_unique_sql := IF(@has_scoped_policy_unique = 0,
  'ALTER TABLE mem_campaign_policy ADD UNIQUE KEY uk_mem_campaign_policy_dept_no (tenant_id, dept_id, policy_no, version)', 'SELECT 1');
PREPARE add_policy_unique_stmt FROM @add_policy_unique_sql;
EXECUTE add_policy_unique_stmt;
DEALLOCATE PREPARE add_policy_unique_stmt;

SELECT 'member_campaign_policy_dept_unique' AS check_name,
       (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'mem_campaign_policy' AND index_name = 'uk_mem_campaign_policy_dept_no') AS scoped_unique_count;
