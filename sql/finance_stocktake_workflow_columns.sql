SET NAMES utf8mb4;

-- ============================================================
-- 盘点工作流集成列迁移（可重复执行、租户隔离、非破坏）
-- 为 finance_stocktake 表添加工作流实例相关字段
-- ============================================================

-- ---------- 1. 添加工作流列 ----------
-- process_instance_id: 工作流实例ID
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_stocktake'
    AND COLUMN_NAME = 'process_instance_id');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE finance_stocktake ADD COLUMN process_instance_id VARCHAR(64) DEFAULT NULL COMMENT ''工作流实例ID'' AFTER reverse_idempotency_key',
    'SELECT ''process_instance_id already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- process_definition_key: 流程定义Key
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_stocktake'
    AND COLUMN_NAME = 'process_definition_key');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE finance_stocktake ADD COLUMN process_definition_key VARCHAR(64) DEFAULT NULL COMMENT ''流程定义Key'' AFTER process_instance_id',
    'SELECT ''process_definition_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- business_key: 业务Key
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_stocktake'
    AND COLUMN_NAME = 'business_key');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE finance_stocktake ADD COLUMN business_key VARCHAR(128) DEFAULT NULL COMMENT ''业务Key'' AFTER process_definition_key',
    'SELECT ''business_key already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- current_node: 当前流程节点
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_stocktake'
    AND COLUMN_NAME = 'current_node');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE finance_stocktake ADD COLUMN current_node VARCHAR(64) DEFAULT NULL COMMENT ''当前流程节点'' AFTER business_key',
    'SELECT ''current_node already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 2. 工作流实例ID索引 ----------
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_stocktake'
    AND INDEX_NAME = 'idx_stocktake_process_instance');
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE finance_stocktake ADD INDEX idx_stocktake_process_instance (tenant_id, process_instance_id)',
    'SELECT ''idx_stocktake_process_instance already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 3. 对账输出 ----------
SELECT '盘点工作流列添加' AS reconciliation_type,
       COUNT(*) AS column_count FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_stocktake'
    AND COLUMN_NAME IN ('process_instance_id', 'process_definition_key', 'business_key', 'current_node');

SELECT '盘点工作流列详情' AS reconciliation_type, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_stocktake'
    AND COLUMN_NAME IN ('process_instance_id', 'process_definition_key', 'business_key', 'current_node')
    ORDER BY ORDINAL_POSITION;
