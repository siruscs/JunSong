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
