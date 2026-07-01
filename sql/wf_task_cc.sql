-- 流程抄送记录表
CREATE TABLE `wf_task_cc` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `process_instance_id` varchar(64) NOT NULL COMMENT '流程实例ID',
  `from_user` varchar(64) NOT NULL COMMENT '抄送人',
  `to_user` varchar(64) NOT NULL COMMENT '被抄送人',
  `cc_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '抄送时间',
  `is_read` char(1) DEFAULT '0' COMMENT '是否已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  KEY `idx_to_user` (`to_user`),
  KEY `idx_process_instance` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程抄送记录表';
