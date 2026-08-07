SET NAMES utf8mb4;

-- 会员等级同步需要机构范围；历史配置保留为 dept_id=0 的租户级基线。
SET @has_dept_id := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'mem_member_card_type' AND column_name = 'dept_id'
);
SET @add_dept_id_sql := IF(@has_dept_id = 0,
  'ALTER TABLE mem_member_card_type ADD COLUMN dept_id BIGINT NOT NULL DEFAULT 0 COMMENT ''机构ID，0表示租户级基线'' AFTER tenant_id',
  'SELECT 1');
PREPARE add_dept_id_stmt FROM @add_dept_id_sql;
EXECUTE add_dept_id_stmt;
DEALLOCATE PREPARE add_dept_id_stmt;

-- 等级编码允许在不同机构复用；同一租户、同一机构内仍必须唯一。
SET @has_global_unique := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'mem_member_card_type'
    AND index_name = 'uk_tenant_type_code'
);
SET @drop_global_unique_sql := IF(@has_global_unique > 0,
  'ALTER TABLE mem_member_card_type DROP INDEX uk_tenant_type_code',
  'SELECT 1');
PREPARE drop_global_unique_stmt FROM @drop_global_unique_sql;
EXECUTE drop_global_unique_stmt;
DEALLOCATE PREPARE drop_global_unique_stmt;

SET @has_scoped_unique := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'mem_member_card_type'
    AND index_name = 'uk_tenant_dept_type_code'
);
SET @add_scoped_unique_sql := IF(@has_scoped_unique = 0,
  'ALTER TABLE mem_member_card_type ADD UNIQUE KEY uk_tenant_dept_type_code (tenant_id, dept_id, type_code)',
  'SELECT 1');
PREPARE add_scoped_unique_stmt FROM @add_scoped_unique_sql;
EXECUTE add_scoped_unique_stmt;
DEALLOCATE PREPARE add_scoped_unique_stmt;

SET @has_scope_index := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'mem_member_card_type' AND index_name = 'idx_member_card_type_scope'
);
SET @add_scope_index_sql := IF(@has_scope_index = 0,
  'ALTER TABLE mem_member_card_type ADD INDEX idx_member_card_type_scope (tenant_id, dept_id, type_code, del_flag)',
  'SELECT 1');
PREPARE add_scope_index_stmt FROM @add_scope_index_sql;
EXECUTE add_scope_index_stmt;
DEALLOCATE PREPARE add_scope_index_stmt;

SELECT 'member_level_scope' AS check_name,
       COUNT(*) AS scoped_rows,
       SUM(CASE WHEN dept_id = 0 THEN 1 ELSE 0 END) AS baseline_rows
FROM mem_member_card_type;
