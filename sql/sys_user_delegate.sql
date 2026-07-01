-- 用户委托代理表
CREATE TABLE `sys_user_delegate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '委托ID',
  `user_id` bigint NOT NULL COMMENT '委托人用户ID',
  `delegate_user_id` bigint NOT NULL COMMENT '代理人用户ID',
  `delegate_type` varchar(20) DEFAULT 'all' COMMENT '委托类型（all=全部, workflow=工作流, system=系统）',
  `process_keys` varchar(500) DEFAULT '' COMMENT '指定流程标识（逗号分隔，type=workflow时生效）',
  `start_time` datetime NOT NULL COMMENT '委托开始时间',
  `end_time` datetime NOT NULL COMMENT '委托结束时间',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_delegate_user_id` (`delegate_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户委托代理表';
