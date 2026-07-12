SET NAMES utf8mb4;

-- Fail closed before DDL if a partially-created target table lacks tenant_id.
DELIMITER //
DROP PROCEDURE IF EXISTS assert_workflow_schema_preconditions//
CREATE PROCEDURE assert_workflow_schema_preconditions()
BEGIN
  DECLARE v_partial_without_tenant INT;
  SELECT COUNT(*) INTO v_partial_without_tenant
  FROM information_schema.TABLES t
  WHERE t.TABLE_SCHEMA=DATABASE()
    AND t.TABLE_NAME IN (
      'wf_node_field_permission','wf_node_timeout','wf_timeout_trigger_log','wf_task_addsign','wf_task_attachment','wf_task_cc',
      'lc_biz_object','lc_biz_field','lc_biz_page_schema','lc_biz_node_assignee','lc_biz_branch_rule','lc_biz_instance',
      'lc_biz_config_snapshot','lc_biz_node_timer','lc_biz_template','lc_biz_action','lc_biz_post_action')
    AND NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS c
      WHERE c.TABLE_SCHEMA=t.TABLE_SCHEMA AND c.TABLE_NAME=t.TABLE_NAME AND c.COLUMN_NAME='tenant_id');
  IF v_partial_without_tenant <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='existing workflow/lowcode table lacks tenant_id';
  END IF;
END//
CALL assert_workflow_schema_preconditions()//
DROP PROCEDURE assert_workflow_schema_preconditions//
DELIMITER ;

CREATE TABLE IF NOT EXISTS `wf_node_field_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `process_definition_key` VARCHAR(64) NOT NULL, `activity_id` VARCHAR(64) NOT NULL,
  `field_key` VARCHAR(100) NOT NULL, `field_label` VARCHAR(100) DEFAULT '',
  `permission` VARCHAR(20) NOT NULL, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_tenant_node_field` (`tenant_id`,`process_definition_key`,`activity_id`,`field_key`),
  KEY `idx_tenant_process_node` (`tenant_id`,`process_definition_key`,`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `wf_node_timeout` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `process_definition_id` VARCHAR(64) NOT NULL, `process_definition_key` VARCHAR(64) NOT NULL,
  `activity_id` VARCHAR(64) NOT NULL, `activity_name` VARCHAR(100) DEFAULT '',
  `timeout_minutes` INT NOT NULL, `escalation_type` VARCHAR(20) DEFAULT 'urge',
  `escalation_target` VARCHAR(200) DEFAULT '', `is_workday` CHAR(1) DEFAULT '0',
  `last_trigger_time` DATETIME DEFAULT NULL, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_tenant_proc_activity` (`tenant_id`,`process_definition_key`,`activity_id`),
  KEY `idx_tenant_proc_key` (`tenant_id`,`process_definition_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `wf_timeout_trigger_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `timeout_config_id` BIGINT NOT NULL, `task_id` VARCHAR(64) NOT NULL,
  `process_instance_id` VARCHAR(64) NOT NULL, `escalation_type` VARCHAR(20) NOT NULL,
  `trigger_time` DATETIME DEFAULT CURRENT_TIMESTAMP, `status` VARCHAR(20) DEFAULT 'success',
  PRIMARY KEY (`id`), KEY `idx_tenant_timeout_config` (`tenant_id`,`timeout_config_id`),
  KEY `idx_tenant_timeout_task` (`tenant_id`,`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `wf_task_addsign` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `original_task_id` VARCHAR(64) NOT NULL, `addsign_task_id` VARCHAR(64) NOT NULL,
  `addsign_user` VARCHAR(64) NOT NULL, `type` VARCHAR(20) NOT NULL,
  `process_instance_id` VARCHAR(64) NOT NULL, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `complete_time` DATETIME DEFAULT NULL, PRIMARY KEY (`id`),
  KEY `idx_tenant_original_task` (`tenant_id`,`original_task_id`),
  KEY `idx_tenant_addsign_task` (`tenant_id`,`addsign_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `wf_task_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `task_id` VARCHAR(64) NOT NULL, `process_instance_id` VARCHAR(64) NOT NULL,
  `file_name` VARCHAR(255) NOT NULL, `file_url` VARCHAR(500) NOT NULL,
  `file_size` BIGINT DEFAULT 0, `upload_user` VARCHAR(64) NOT NULL,
  `upload_time` DATETIME DEFAULT CURRENT_TIMESTAMP, `action_type` VARCHAR(20) DEFAULT 'approve',
  PRIMARY KEY (`id`), KEY `idx_tenant_attachment_task` (`tenant_id`,`task_id`),
  KEY `idx_tenant_attachment_process` (`tenant_id`,`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `wf_task_cc` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `task_id` VARCHAR(64) NOT NULL, `process_instance_id` VARCHAR(64) NOT NULL,
  `from_user` VARCHAR(64) NOT NULL, `to_user` VARCHAR(64) NOT NULL,
  `cc_time` DATETIME DEFAULT CURRENT_TIMESTAMP, `is_read` CHAR(1) DEFAULT '0', `read_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_tenant_cc_user` (`tenant_id`,`to_user`),
  KEY `idx_tenant_cc_process` (`tenant_id`,`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `lc_biz_object` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `biz_code` VARCHAR(64) NOT NULL, `biz_name` VARCHAR(128) NOT NULL,
  `storage_mode` VARCHAR(16) NOT NULL DEFAULT 'GENERIC', `table_name` VARCHAR(64) DEFAULT NULL,
  `pk_field` VARCHAR(64) NOT NULL DEFAULT 'id', `order_no_field` VARCHAR(64) DEFAULT 'order_no',
  `order_no_prefix` VARCHAR(16) DEFAULT NULL, `status_field` VARCHAR(64) NOT NULL DEFAULT 'workflow_status',
  `workflow_enabled` CHAR(1) NOT NULL DEFAULT '1', `process_key` VARCHAR(64) DEFAULT NULL,
  `fulfillment_enabled` CHAR(1) NOT NULL DEFAULT '0', `menu_parent_path` VARCHAR(64) DEFAULT NULL,
  `submit_validators` TEXT DEFAULT NULL, `status` CHAR(1) NOT NULL DEFAULT '0',
  `config_status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT', `published_version` INT NOT NULL DEFAULT 0,
  `del_flag` CHAR(1) NOT NULL DEFAULT '0', `create_by` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, `update_by` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_tenant_lc_biz_code` (`tenant_id`,`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `lc_biz_field` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `biz_code` VARCHAR(64) NOT NULL, `field_key` VARCHAR(64) NOT NULL, `field_label` VARCHAR(128) NOT NULL,
  `field_type` VARCHAR(32) NOT NULL DEFAULT 'string', `component_type` VARCHAR(32) NOT NULL DEFAULT 'input',
  `required` CHAR(1) NOT NULL DEFAULT '0', `default_value` VARCHAR(255) DEFAULT NULL,
  `dict_type` VARCHAR(64) DEFAULT NULL, `upload_config` VARCHAR(255) DEFAULT NULL,
  `field_ext` VARCHAR(512) DEFAULT NULL, `validate_rule` VARCHAR(255) DEFAULT NULL,
  `stage` VARCHAR(16) NOT NULL DEFAULT 'APPLY', `is_query` CHAR(1) NOT NULL DEFAULT '0',
  `is_list` CHAR(1) NOT NULL DEFAULT '0', `is_detail` CHAR(1) NOT NULL DEFAULT '1',
  `is_process_var` CHAR(1) NOT NULL DEFAULT '0', `process_var_name` VARCHAR(64) DEFAULT NULL,
  `parent_field_key` VARCHAR(64) DEFAULT NULL, `order_num` INT NOT NULL DEFAULT 0,
  `del_flag` CHAR(1) NOT NULL DEFAULT '0', `create_by` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, `update_by` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_tenant_lc_field` (`tenant_id`,`biz_code`,`field_key`),
  KEY `idx_tenant_lc_field_biz` (`tenant_id`,`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `lc_biz_page_schema` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `biz_code` VARCHAR(64) NOT NULL, `page_type` VARCHAR(16) NOT NULL, `schema_json` LONGTEXT DEFAULT NULL,
  `version` INT NOT NULL DEFAULT 1, `status` CHAR(1) NOT NULL DEFAULT '0', `del_flag` CHAR(1) NOT NULL DEFAULT '0',
  `create_by` VARCHAR(64) DEFAULT '', `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT '', `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_tenant_lc_schema` (`tenant_id`,`biz_code`,`page_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `lc_biz_node_assignee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `biz_code` VARCHAR(64) NOT NULL, `task_key` VARCHAR(64) NOT NULL, `task_name` VARCHAR(128) DEFAULT NULL,
  `assignee_source` VARCHAR(32) NOT NULL, `assignee_value` VARCHAR(255) DEFAULT NULL,
  `assignee_expr` VARCHAR(500) DEFAULT NULL, `process_var_name` VARCHAR(64) DEFAULT NULL,
  `multi_instance_type` VARCHAR(20) DEFAULT 'none', `completion_condition` VARCHAR(200) DEFAULT '',
  `collection_source` VARCHAR(50) DEFAULT '', `del_flag` CHAR(1) NOT NULL DEFAULT '0',
  `create_by` VARCHAR(64) DEFAULT '', `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT '', `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_tenant_lc_assignee` (`tenant_id`,`biz_code`,`task_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `lc_biz_branch_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `biz_code` VARCHAR(64) NOT NULL, `gateway_key` VARCHAR(64) DEFAULT NULL,
  `field_key` VARCHAR(64) NOT NULL, `operator` VARCHAR(8) NOT NULL, `compare_value` VARCHAR(64) DEFAULT NULL,
  `target_var_name` VARCHAR(64) DEFAULT NULL, `group_op` VARCHAR(16) DEFAULT NULL, `parent_rule_id` BIGINT DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL, `del_flag` CHAR(1) NOT NULL DEFAULT '0',
  `create_by` VARCHAR(64) DEFAULT '', `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT '', `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_tenant_lc_branch` (`tenant_id`,`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `lc_biz_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `biz_code` VARCHAR(64) NOT NULL, `order_no` VARCHAR(64) NOT NULL, `form_data` JSON DEFAULT NULL,
  `process_instance_id` VARCHAR(64) DEFAULT NULL, `process_definition_key` VARCHAR(64) DEFAULT NULL,
  `workflow_status` VARCHAR(64) NOT NULL DEFAULT 'DRAFT', `current_task_name` VARCHAR(128) DEFAULT NULL,
  `submitter` VARCHAR(64) DEFAULT NULL, `submit_time` DATETIME DEFAULT NULL,
  `del_flag` CHAR(1) NOT NULL DEFAULT '0', `create_by` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, `update_by` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_tenant_lc_instance_order` (`tenant_id`,`biz_code`,`order_no`),
  KEY `idx_tenant_lc_instance_status` (`tenant_id`,`workflow_status`),
  KEY `idx_tenant_lc_instance_process` (`tenant_id`,`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `lc_biz_config_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `biz_code` VARCHAR(64) NOT NULL, `version_no` INT NOT NULL, `config_json` LONGTEXT NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED', `publish_remark` VARCHAR(500) DEFAULT NULL,
  `del_flag` CHAR(1) DEFAULT '0', `create_by` VARCHAR(64) DEFAULT '', `create_time` DATETIME DEFAULT NULL,
  `update_by` VARCHAR(64) DEFAULT '', `update_time` DATETIME DEFAULT NULL, `remark` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_tenant_lc_biz_version` (`tenant_id`,`biz_code`,`version_no`),
  KEY `idx_tenant_lc_snapshot_status` (`tenant_id`,`biz_code`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `lc_biz_node_timer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `biz_code` VARCHAR(64) NOT NULL, `task_key` VARCHAR(128) NOT NULL, `task_name` VARCHAR(128) DEFAULT NULL,
  `duration` VARCHAR(32) NOT NULL, `timeout_action` VARCHAR(32) NOT NULL DEFAULT 'AUTO_CANCEL',
  `target_status` VARCHAR(32) DEFAULT NULL, `del_flag` CHAR(1) DEFAULT '0',
  `create_by` VARCHAR(64) DEFAULT '', `create_time` DATETIME DEFAULT NULL,
  `update_by` VARCHAR(64) DEFAULT '', `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_tenant_lc_timer_biz` (`tenant_id`,`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `lc_biz_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `template_code` VARCHAR(64) NOT NULL, `template_name` VARCHAR(128) NOT NULL,
  `category` VARCHAR(32) DEFAULT NULL, `description` VARCHAR(500) DEFAULT NULL,
  `thumbnail` VARCHAR(256) DEFAULT NULL, `process_key` VARCHAR(64) DEFAULT NULL,
  `config_json` LONGTEXT NOT NULL, `usage_count` INT DEFAULT 0, `is_starter` CHAR(1) DEFAULT '0',
  `del_flag` CHAR(1) DEFAULT '0', `create_by` VARCHAR(64) DEFAULT '', `create_time` DATETIME DEFAULT NULL,
  `update_by` VARCHAR(64) DEFAULT '', `update_time` DATETIME DEFAULT NULL, `remark` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_tenant_lc_template_code` (`tenant_id`,`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `lc_biz_action` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `biz_code` VARCHAR(64) NOT NULL, `action_code` VARCHAR(64) NOT NULL, `action_name` VARCHAR(64) NOT NULL,
  `action_type` VARCHAR(20) NOT NULL DEFAULT 'BUILTIN', `trigger_status` VARCHAR(200) NOT NULL,
  `api_endpoint` VARCHAR(500) DEFAULT NULL, `button_style` VARCHAR(20) DEFAULT 'primary',
  `button_icon` VARCHAR(50) DEFAULT NULL, `sort_order` INT DEFAULT 0, `status` CHAR(1) DEFAULT '0',
  `del_flag` CHAR(1) DEFAULT '0', `create_by` VARCHAR(64) DEFAULT '', `create_time` DATETIME DEFAULT NULL,
  `update_by` VARCHAR(64) DEFAULT '', `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_tenant_lc_action` (`tenant_id`,`biz_code`,`action_code`,`del_flag`),
  KEY `idx_tenant_lc_action_biz` (`tenant_id`,`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `lc_biz_post_action` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `biz_code` VARCHAR(64) NOT NULL, `trigger_event` VARCHAR(30) NOT NULL, `action_type` VARCHAR(20) NOT NULL,
  `target_field` VARCHAR(100) DEFAULT NULL, `target_value` VARCHAR(200) DEFAULT NULL,
  `condition_expr` VARCHAR(500) DEFAULT NULL, `callback_url` VARCHAR(500) DEFAULT NULL,
  `sort_order` INT DEFAULT 0, `status` CHAR(1) DEFAULT '0', `del_flag` CHAR(1) DEFAULT '0',
  `create_by` VARCHAR(64) DEFAULT '', `create_time` DATETIME DEFAULT NULL,
  `update_by` VARCHAR(64) DEFAULT '', `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_tenant_lc_post_action_biz` (`tenant_id`,`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELIMITER //
DROP PROCEDURE IF EXISTS assert_workflow_schema_result//
CREATE PROCEDURE assert_workflow_schema_result()
BEGIN
  DECLARE v_table_count INT;
  DECLARE v_tenant_column_count INT;
  DECLARE v_required_column_count INT;
  DECLARE v_tenant_unique_index_count INT;
  SELECT COUNT(*) INTO v_table_count FROM information_schema.TABLES
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN (
    'wf_node_field_permission','wf_node_timeout','wf_timeout_trigger_log','wf_task_addsign','wf_task_attachment','wf_task_cc',
    'lc_biz_object','lc_biz_field','lc_biz_page_schema','lc_biz_node_assignee','lc_biz_branch_rule','lc_biz_instance',
    'lc_biz_config_snapshot','lc_biz_node_timer','lc_biz_template','lc_biz_action','lc_biz_post_action');
  SELECT COUNT(*) INTO v_tenant_column_count FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='tenant_id' AND TABLE_NAME IN (
    'wf_node_field_permission','wf_node_timeout','wf_timeout_trigger_log','wf_task_addsign','wf_task_attachment','wf_task_cc',
    'lc_biz_object','lc_biz_field','lc_biz_page_schema','lc_biz_node_assignee','lc_biz_branch_rule','lc_biz_instance',
    'lc_biz_config_snapshot','lc_biz_node_timer','lc_biz_template','lc_biz_action','lc_biz_post_action');
  SELECT COUNT(*) INTO v_required_column_count FROM information_schema.COLUMNS c JOIN (
    SELECT 'wf_node_field_permission' t,'permission' n UNION ALL SELECT 'wf_node_timeout','timeout_minutes'
    UNION ALL SELECT 'wf_timeout_trigger_log','timeout_config_id' UNION ALL SELECT 'wf_task_addsign','addsign_task_id'
    UNION ALL SELECT 'wf_task_attachment','file_url' UNION ALL SELECT 'wf_task_cc','to_user'
    UNION ALL SELECT 'lc_biz_object','submit_validators' UNION ALL SELECT 'lc_biz_object','config_status'
    UNION ALL SELECT 'lc_biz_object','published_version' UNION ALL SELECT 'lc_biz_field','field_ext'
    UNION ALL SELECT 'lc_biz_page_schema','schema_json' UNION ALL SELECT 'lc_biz_node_assignee','completion_condition'
    UNION ALL SELECT 'lc_biz_branch_rule','parent_rule_id' UNION ALL SELECT 'lc_biz_instance','form_data'
    UNION ALL SELECT 'lc_biz_config_snapshot','config_json' UNION ALL SELECT 'lc_biz_node_timer','timeout_action'
    UNION ALL SELECT 'lc_biz_template','config_json' UNION ALL SELECT 'lc_biz_action','trigger_status'
    UNION ALL SELECT 'lc_biz_post_action','trigger_event'
  ) r ON r.t=c.TABLE_NAME AND r.n=c.COLUMN_NAME WHERE c.TABLE_SCHEMA=DATABASE();
  SELECT COUNT(*) INTO v_tenant_unique_index_count FROM information_schema.STATISTICS s
  WHERE s.TABLE_SCHEMA=DATABASE() AND s.NON_UNIQUE=0 AND s.SEQ_IN_INDEX=1 AND s.COLUMN_NAME='tenant_id'
    AND s.TABLE_NAME IN ('wf_node_field_permission','wf_node_timeout','lc_biz_object','lc_biz_field','lc_biz_page_schema',
      'lc_biz_node_assignee','lc_biz_instance','lc_biz_config_snapshot','lc_biz_template','lc_biz_action');
  IF v_table_count <> 17 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='workflow extension table reconciliation failed'; END IF;
  IF v_tenant_column_count <> 17 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='workflow tenant column reconciliation failed'; END IF;
  IF v_required_column_count <> 19 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='workflow required column reconciliation failed'; END IF;
  IF v_tenant_unique_index_count <> 10 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='workflow tenant unique index reconciliation failed'; END IF;
END//
CALL assert_workflow_schema_result()//
DROP PROCEDURE assert_workflow_schema_result//
DELIMITER ;

SELECT TABLE_NAME,ENGINE,TABLE_COLLATION FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN (
  'wf_node_field_permission','wf_node_timeout','wf_timeout_trigger_log','wf_task_addsign','wf_task_attachment','wf_task_cc',
  'lc_biz_object','lc_biz_field','lc_biz_page_schema','lc_biz_node_assignee','lc_biz_branch_rule','lc_biz_instance',
  'lc_biz_config_snapshot','lc_biz_node_timer','lc_biz_template','lc_biz_action','lc_biz_post_action')
ORDER BY TABLE_NAME;
SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='tenant_id' AND TABLE_NAME IN (
  'wf_node_field_permission','wf_node_timeout','wf_timeout_trigger_log','wf_task_addsign','wf_task_attachment','wf_task_cc',
  'lc_biz_object','lc_biz_field','lc_biz_page_schema','lc_biz_node_assignee','lc_biz_branch_rule','lc_biz_instance',
  'lc_biz_config_snapshot','lc_biz_node_timer','lc_biz_template','lc_biz_action','lc_biz_post_action')
ORDER BY TABLE_NAME;
