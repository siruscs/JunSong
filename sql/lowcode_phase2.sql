SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================================
-- Workflow 低代码平台 阶段 2：配置元数据 + 运行态通用单据
-- 设计依据：docs/superpowers/specs/2026-06-22-workflow-lowcode-phase2-config-driven-design.md
-- 全部表前缀 lc_（lowcode）
-- ============================================================

-- 1. 业务对象元数据
CREATE TABLE IF NOT EXISTS `lc_biz_object` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_code` VARCHAR(64) NOT NULL COMMENT '业务编码（唯一，=processKey=前端路由key）',
  `biz_name` VARCHAR(128) NOT NULL COMMENT '业务中文名',
  `storage_mode` VARCHAR(16) NOT NULL DEFAULT 'GENERIC' COMMENT '存储模式：GENERIC通用表/NATIVE独立表',
  `table_name` VARCHAR(64) DEFAULT NULL COMMENT 'NATIVE模式物理表名；GENERIC留空',
  `pk_field` VARCHAR(64) NOT NULL DEFAULT 'id' COMMENT '主键字段名',
  `order_no_field` VARCHAR(64) DEFAULT 'order_no' COMMENT '单号字段名',
  `order_no_prefix` VARCHAR(16) DEFAULT NULL COMMENT '单号前缀',
  `status_field` VARCHAR(64) NOT NULL DEFAULT 'workflow_status' COMMENT '状态字段名',
  `workflow_enabled` CHAR(1) NOT NULL DEFAULT '1' COMMENT '是否启用流程（0否1是）',
  `process_key` VARCHAR(64) DEFAULT NULL COMMENT '关联BPMN process id',
  `fulfillment_enabled` CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否启用履约阶段（0否1是）',
  `menu_parent_path` VARCHAR(64) DEFAULT NULL COMMENT '菜单挂载父级path',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '启用状态（0启用1停用）',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在2删除）',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lc_biz_object_code` (`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码-业务对象元数据';

-- 2. 字段元数据
CREATE TABLE IF NOT EXISTS `lc_biz_field` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_code` VARCHAR(64) NOT NULL COMMENT '所属业务编码',
  `field_key` VARCHAR(64) NOT NULL COMMENT '字段编码（英文）',
  `field_label` VARCHAR(128) NOT NULL COMMENT '字段中文名',
  `field_type` VARCHAR(32) NOT NULL DEFAULT 'string' COMMENT '数据类型',
  `component_type` VARCHAR(32) NOT NULL DEFAULT 'input' COMMENT '组件类型',
  `required` CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否必填（0否1是）',
  `default_value` VARCHAR(255) DEFAULT NULL COMMENT '默认值',
  `dict_type` VARCHAR(64) DEFAULT NULL COMMENT '字典来源',
  `upload_config` VARCHAR(255) DEFAULT NULL COMMENT '上传配置JSON',
  `field_ext` VARCHAR(512) DEFAULT NULL COMMENT '字段扩展配置JSON',
  `validate_rule` VARCHAR(255) DEFAULT NULL COMMENT '校验规则JSON',
  `stage` VARCHAR(16) NOT NULL DEFAULT 'APPLY' COMMENT '录入阶段：APPLY发起/FULFILLMENT履约',
  `is_query` CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否查询条件',
  `is_list` CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否列表展示',
  `is_detail` CHAR(1) NOT NULL DEFAULT '1' COMMENT '是否详情展示',
  `is_process_var` CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否作为流程变量',
  `process_var_name` VARCHAR(64) DEFAULT NULL COMMENT '映射的流程变量名',
  `order_num` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在2删除）',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lc_field_biz_key` (`biz_code`, `field_key`),
  KEY `idx_lc_field_biz` (`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码-字段元数据';

-- 3. 页面 Schema
CREATE TABLE IF NOT EXISTS `lc_biz_page_schema` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_code` VARCHAR(64) NOT NULL COMMENT '所属业务编码',
  `page_type` VARCHAR(16) NOT NULL COMMENT '页面类型：FORM/LIST/DETAIL',
  `schema_json` TEXT COMMENT '页面schema JSON',
  `version` INT NOT NULL DEFAULT 1 COMMENT 'schema版本',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '启用状态（0启用1停用）',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在2删除）',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lc_schema_biz_type` (`biz_code`, `page_type`),
  KEY `idx_lc_schema_biz` (`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码-页面Schema';

-- 4. 节点处理人来源
CREATE TABLE IF NOT EXISTS `lc_biz_node_assignee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_code` VARCHAR(64) NOT NULL COMMENT '所属业务编码',
  `task_key` VARCHAR(64) NOT NULL COMMENT 'BPMN UserTask id',
  `task_name` VARCHAR(128) DEFAULT NULL COMMENT '节点中文名',
  `assignee_source` VARCHAR(32) NOT NULL COMMENT '来源类型：FIXED_USER/FIXED_ROLE/DEPT_LEADER/FORM_FIELD_USER/FORM_FIELD_DEPT_LEADER/INITIATOR_LEADER/INITIATOR',
  `assignee_value` VARCHAR(255) DEFAULT NULL COMMENT '来源值',
  `process_var_name` VARCHAR(64) DEFAULT NULL COMMENT '注入的流程变量名',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在2删除）',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lc_assignee_biz_task` (`biz_code`, `task_key`),
  KEY `idx_lc_assignee_biz` (`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码-节点处理人来源';

-- 5. 分支规则
CREATE TABLE IF NOT EXISTS `lc_biz_branch_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_code` VARCHAR(64) NOT NULL COMMENT '所属业务编码',
  `gateway_key` VARCHAR(64) DEFAULT NULL COMMENT 'BPMN网关id',
  `field_key` VARCHAR(64) NOT NULL COMMENT '参与判断的字段',
  `operator` VARCHAR(8) NOT NULL COMMENT '运算符：>= > <= < == !=',
  `compare_value` VARCHAR(64) DEFAULT NULL COMMENT '比较值',
  `target_var_name` VARCHAR(64) DEFAULT NULL COMMENT '生成的流程变量名',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '说明',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在2删除）',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_lc_branch_biz` (`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码-分支规则';

-- 6. 运行态通用单据
CREATE TABLE IF NOT EXISTS `lc_biz_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_code` VARCHAR(64) NOT NULL COMMENT '业务编码',
  `order_no` VARCHAR(64) NOT NULL COMMENT '业务单号（businessKey）',
  `form_data` JSON DEFAULT NULL COMMENT '业务字段值（KV JSON）',
  `process_instance_id` VARCHAR(64) DEFAULT NULL COMMENT '流程实例ID',
  `process_definition_key` VARCHAR(64) DEFAULT NULL COMMENT '流程定义Key',
  `workflow_status` VARCHAR(64) NOT NULL DEFAULT 'DRAFT' COMMENT '审批状态',
  `current_task_name` VARCHAR(128) DEFAULT NULL COMMENT '当前节点名称',
  `submitter` VARCHAR(64) DEFAULT NULL COMMENT '提交人',
  `submit_time` DATETIME DEFAULT NULL COMMENT '提交时间',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在2删除）',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lc_instance_biz_order` (`biz_code`, `order_no`),
  KEY `idx_lc_instance_status` (`workflow_status`),
  KEY `idx_lc_instance_process` (`process_instance_id`),
  FULLTEXT KEY `ft_form_data` (`form_data`) COMMENT 'JSON 内容全文索引，支持关键词检索'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码-运行态通用单据';
