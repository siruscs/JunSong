-- R21 schema fix: 补齐 fin_stock_snapshot 缺失的列
-- 原因：FinStockLedgerMapper.xml upsertSnapshot 写入 opening_quantity/in_quantity/out_quantity，
--        但 DEV 库 fin_stock_snapshot 表缺这三列，导致 R21_STOCK_DAILY_SNAPSHOT 任务 FAILED。
-- 幂等：使用 information_schema 检查列是否存在，不存在才 ADD。

-- opening_quantity: 期初库存
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_stock_snapshot' AND COLUMN_NAME = 'opening_quantity');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE fin_stock_snapshot ADD COLUMN opening_quantity int DEFAULT 0 AFTER quantity',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- in_quantity: 当日入库量
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_stock_snapshot' AND COLUMN_NAME = 'in_quantity');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE fin_stock_snapshot ADD COLUMN in_quantity int DEFAULT 0 AFTER opening_quantity',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- out_quantity: 当日出库量
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_stock_snapshot' AND COLUMN_NAME = 'out_quantity');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE fin_stock_snapshot ADD COLUMN out_quantity int DEFAULT 0 AFTER in_quantity',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
