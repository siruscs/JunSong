-- lowcode_field_enhance.sql
-- 5.3.7 主子表/复杂单据增强 - 数据库字段变更

-- 主子表增强：parentFieldKey 标识父子关系
ALTER TABLE lc_biz_field
  ADD COLUMN `parent_field_key` VARCHAR(64) DEFAULT NULL COMMENT '父字段Key（子表场景）' AFTER `process_var_name`;

-- 复杂分支规则增强：AND/OR 组合
ALTER TABLE lc_biz_branch_rule
  ADD COLUMN `group_op` VARCHAR(16) DEFAULT NULL COMMENT '组合操作：AND/OR（同级规则的逻辑关系）' AFTER `target_var_name`,
  ADD COLUMN `parent_rule_id` BIGINT DEFAULT NULL COMMENT '父规则ID（嵌套场景）' AFTER `group_op`;

-- 复杂参与者规则增强：动态表达式
ALTER TABLE lc_biz_node_assignee
  ADD COLUMN `assignee_expr` TEXT DEFAULT NULL COMMENT '动态表达式（assigneeSource=EXPRESSION时求值）' AFTER `assignee_value`;
