SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mem_config_sync_batch` (
  `batch_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '同步批次ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `source_dept_id` BIGINT NOT NULL COMMENT '来源机构ID',
  `sync_type` VARCHAR(32) NOT NULL COMMENT '同步类型',
  `preview_version` BIGINT NOT NULL DEFAULT 1 COMMENT '预览版本',
  `status` VARCHAR(24) NOT NULL DEFAULT 'PREVIEWED' COMMENT '批次状态',
  `idempotency_key` VARCHAR(128) NOT NULL COMMENT '幂等键',
  `create_by` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`batch_id`),
  UNIQUE KEY `uk_mem_config_sync_batch_idempotency` (`tenant_id`, `idempotency_key`),
  KEY `idx_mem_config_sync_batch_scope` (`tenant_id`, `source_dept_id`, `sync_type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员及基础配置同步批次';

CREATE TABLE IF NOT EXISTS `mem_config_sync_detail` (
  `detail_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '同步明细ID',
  `batch_id` BIGINT NOT NULL COMMENT '批次ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `target_dept_id` BIGINT NOT NULL COMMENT '目标机构ID',
  `target_period_id` BIGINT DEFAULT NULL COMMENT '政策同步目标核算周期ID',
  `business_key` VARCHAR(128) NOT NULL COMMENT '业务匹配键',
  `source_record_id` BIGINT DEFAULT NULL COMMENT '来源记录ID',
  `target_record_id` BIGINT DEFAULT NULL COMMENT '目标记录ID',
  `operation` VARCHAR(24) NOT NULL COMMENT 'CREATE/DIFF/NOOP',
  `decision` VARCHAR(24) DEFAULT NULL COMMENT 'CREATE/OVERWRITE/SKIP',
  `source_snapshot` JSON NOT NULL COMMENT '来源快照',
  `target_snapshot` JSON DEFAULT NULL COMMENT '目标快照',
  `diff_snapshot` JSON NOT NULL COMMENT '字段差异快照',
  `source_row_version` BIGINT DEFAULT NULL COMMENT '来源版本',
  `target_row_version` BIGINT DEFAULT NULL COMMENT '目标版本',
  `result_status` VARCHAR(24) NOT NULL DEFAULT 'PENDING' COMMENT '明细结果',
  `error_code` VARCHAR(64) DEFAULT NULL,
  `error_message` VARCHAR(500) DEFAULT NULL,
  `result_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `uk_mem_config_sync_detail_key` (`batch_id`, `target_dept_id`, `business_key`),
  KEY `idx_mem_config_sync_detail_scope` (`tenant_id`, `target_dept_id`, `result_status`),
  CONSTRAINT `fk_mem_config_sync_detail_batch` FOREIGN KEY (`batch_id`) REFERENCES `mem_config_sync_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员及基础配置同步明细';

SELECT 'mem_config_sync_tables' AS check_name,
       COUNT(*) AS created_tables
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('mem_config_sync_batch', 'mem_config_sync_detail');
