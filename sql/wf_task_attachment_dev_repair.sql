SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `wf_task_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1,
  `task_id` VARCHAR(64) NOT NULL,
  `process_instance_id` VARCHAR(64) NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `file_url` VARCHAR(500) NOT NULL,
  `file_size` BIGINT DEFAULT 0,
  `upload_user` VARCHAR(64) NOT NULL,
  `upload_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `action_type` VARCHAR(20) DEFAULT 'approve',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_attachment_task` (`tenant_id`, `task_id`),
  KEY `idx_tenant_attachment_process` (`tenant_id`, `process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程任务附件表';
