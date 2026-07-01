-- Phase 4 超时配置表增强
-- 增加最后触发时间字段，防止重复触发
SET @exist := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'wf_node_timeout' AND column_name = 'last_trigger_time');
SET @sql := IF(@exist = 0, 'ALTER TABLE wf_node_timeout ADD COLUMN last_trigger_time datetime DEFAULT NULL COMMENT "最后触发时间"', 'SELECT "column already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Phase 4 超时触发日志表（记录每次超时触发详情）
CREATE TABLE IF NOT EXISTS wf_timeout_trigger_log (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `timeout_config_id` bigint NOT NULL COMMENT '超时配置ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `process_instance_id` varchar(64) NOT NULL COMMENT '流程实例ID',
  `escalation_type` varchar(20) NOT NULL COMMENT '实际触发的升级类型',
  `trigger_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
  `status` varchar(20) DEFAULT 'success' COMMENT '触发状态',
  PRIMARY KEY (`id`),
  KEY `idx_config_id` (`timeout_config_id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='超时触发日志表';
