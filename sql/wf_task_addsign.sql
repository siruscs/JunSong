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
