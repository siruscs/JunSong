-- SYS-E1: 会计期间锁账治理增强 - 新增 carry_forward_by 列
-- 记录结转操作人，支持锁账人追溯
SET @column_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'fin_accounting_period'
      AND column_name = 'carry_forward_by'
);

SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE fin_accounting_period ADD COLUMN carry_forward_by VARCHAR(64) DEFAULT NULL COMMENT ''结转操作人'' AFTER carry_forward_time',
    'SELECT ''carry_forward_by already exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
