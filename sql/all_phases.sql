-- 加签记录表
CREATE TABLE `wf_task_addsign` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `original_task_id` varchar(64) NOT NULL COMMENT '原始任务ID',
  `addsign_task_id` varchar(64) NOT NULL COMMENT '加签任务ID',
  `addsign_user` varchar(64) NOT NULL COMMENT '加签人',
  `type` varchar(20) NOT NULL COMMENT '加签类型（before=前加签, after=后加签）',
  `process_instance_id` varchar(64) NOT NULL COMMENT '流程实例ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_original_task` (`original_task_id`),
  KEY `idx_addsign_task` (`addsign_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程加签记录表';
-- 流程节点超时配置表
CREATE TABLE `wf_node_timeout` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `process_definition_id` varchar(64) NOT NULL COMMENT '流程定义ID',
  `process_definition_key` varchar(64) NOT NULL COMMENT '流程标识',
  `activity_id` varchar(64) NOT NULL COMMENT '节点ID',
  `activity_name` varchar(100) DEFAULT '' COMMENT '节点名称',
  `timeout_minutes` int NOT NULL COMMENT '超时时间（分钟）',
  `escalation_type` varchar(20) DEFAULT 'urge' COMMENT '升级类型（urge=催办, transfer=转上级, auto_approve=自动通过, auto_reject=自动驳回）',
  `escalation_target` varchar(200) DEFAULT '' COMMENT '升级目标（用户/角色/表达式）',
  `is_workday` char(1) DEFAULT '0' COMMENT '是否按工作日计算（0否 1是）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_proc_activity` (`process_definition_key`, `activity_id`),
  KEY `idx_proc_key` (`process_definition_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程节点超时配置表';
-- 流程节点字段权限表
CREATE TABLE `wf_node_field_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `process_definition_key` varchar(64) NOT NULL COMMENT '流程标识',
  `activity_id` varchar(64) NOT NULL COMMENT '节点ID',
  `field_key` varchar(100) NOT NULL COMMENT '字段标识',
  `field_label` varchar(100) DEFAULT '' COMMENT '字段名称',
  `permission` varchar(20) NOT NULL COMMENT '权限（hidden=隐藏, readonly=只读, editable=可编辑, required=必填）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_node_field` (`process_definition_key`, `activity_id`, `field_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程节点字段权限表';
CREATE TABLE `wf_task_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `process_instance_id` varchar(64) NOT NULL COMMENT '流程实例ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名称',
  `file_url` varchar(500) NOT NULL COMMENT '文件URL',
  `file_size` bigint DEFAULT 0 COMMENT '文件大小（字节）',
  `upload_user` varchar(64) NOT NULL COMMENT '上传人',
  `upload_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `action_type` varchar(20) DEFAULT 'approve' COMMENT '操作类型（approve=审批通过, reject=驳回, comment=评论）',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程任务附件表';
