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
