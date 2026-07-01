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
