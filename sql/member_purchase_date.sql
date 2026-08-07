SET NAMES utf8mb4;

-- 购买业务日期独立于 create_time，支持历史销售单据补录及跨日查询。
SET @purchase_date_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mem_purchase_order'
      AND column_name = 'purchase_date'
);
SET @purchase_date_add_sql = IF(
    @purchase_date_column_exists = 0,
    'ALTER TABLE mem_purchase_order ADD COLUMN purchase_date DATETIME NULL COMMENT ''购买业务日期'' AFTER period_id',
    'SELECT 1'
);
PREPARE purchase_date_add_stmt FROM @purchase_date_add_sql;
EXECUTE purchase_date_add_stmt;
DEALLOCATE PREPARE purchase_date_add_stmt;

UPDATE mem_purchase_order
SET purchase_date = create_time
WHERE purchase_date IS NULL;

ALTER TABLE mem_purchase_order
    MODIFY COLUMN purchase_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '购买业务日期';

SET @purchase_date_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mem_purchase_order'
      AND index_name = 'idx_mem_purchase_purchase_date'
);
SET @purchase_date_index_sql = IF(
    @purchase_date_index_exists = 0,
    'ALTER TABLE mem_purchase_order ADD KEY idx_mem_purchase_purchase_date (tenant_id, dept_id, purchase_date)',
    'SELECT 1'
);
PREPARE purchase_date_index_stmt FROM @purchase_date_index_sql;
EXECUTE purchase_date_index_stmt;
DEALLOCATE PREPARE purchase_date_index_stmt;
