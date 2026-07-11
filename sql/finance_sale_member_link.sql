-- finance_sale_member_link.sql
-- 幂等增加 fin_sale_record 会员关联字段
-- 回滚：ALTER TABLE fin_sale_record DROP COLUMN member_name, DROP COLUMN member_no, DROP COLUMN member_id;

SET @ddl := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE fin_sale_record ADD COLUMN member_id BIGINT DEFAULT NULL COMMENT ''会员ID'' AFTER product_id',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND BINARY TABLE_NAME = BINARY 'fin_sale_record' AND BINARY COLUMN_NAME = BINARY 'member_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE fin_sale_record ADD COLUMN member_no VARCHAR(64) DEFAULT NULL COMMENT ''会员编号'' AFTER member_id',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND BINARY TABLE_NAME = BINARY 'fin_sale_record' AND BINARY COLUMN_NAME = BINARY 'member_no'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE fin_sale_record ADD COLUMN member_name VARCHAR(100) DEFAULT NULL COMMENT ''会员姓名'' AFTER member_no',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND BINARY TABLE_NAME = BINARY 'fin_sale_record' AND BINARY COLUMN_NAME = BINARY 'member_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COUNT(*) = 0,
    'CREATE INDEX idx_fin_sale_member ON fin_sale_record(member_id, sale_date)',
    'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND BINARY INDEX_NAME = BINARY 'idx_fin_sale_member'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
