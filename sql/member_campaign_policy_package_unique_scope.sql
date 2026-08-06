SET NAMES utf8mb4;

-- 套餐明细采用逻辑删除；唯一约束必须允许已删除历史行与新有效行使用相同购买数量。
SET @has_old_package_unique := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'mem_campaign_policy_package'
    AND index_name = 'uk_mem_campaign_package_quantity'
);
SET @has_scoped_package_unique := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'mem_campaign_policy_package'
    AND index_name = 'uk_mem_campaign_package_quantity_scope'
);
SET @drop_old_package_unique_sql := IF(
  @has_old_package_unique > 0,
  'ALTER TABLE mem_campaign_policy_package DROP INDEX uk_mem_campaign_package_quantity',
  'SELECT 1'
);
PREPARE drop_old_package_unique_stmt FROM @drop_old_package_unique_sql;
EXECUTE drop_old_package_unique_stmt;
DEALLOCATE PREPARE drop_old_package_unique_stmt;

SET @add_scoped_package_unique_sql := IF(
  @has_scoped_package_unique = 0,
  'ALTER TABLE mem_campaign_policy_package ADD UNIQUE KEY uk_mem_campaign_package_quantity_scope (policy_id, purchase_quantity, del_flag)',
  'SELECT 1'
);
PREPARE add_scoped_package_unique_stmt FROM @add_scoped_package_unique_sql;
EXECUTE add_scoped_package_unique_stmt;
DEALLOCATE PREPARE add_scoped_package_unique_stmt;

SELECT 'member_campaign_policy_package_unique_scope' AS check_name,
       (SELECT COUNT(*) FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'mem_campaign_policy_package'
          AND index_name = 'uk_mem_campaign_package_quantity_scope') AS scoped_unique_index;
