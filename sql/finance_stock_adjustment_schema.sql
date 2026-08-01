SET NAMES utf8mb4;

ALTER TABLE fin_stock_init_batch
    ADD COLUMN IF NOT EXISTS adjustment_type VARCHAR(32) NOT NULL DEFAULT 'OPENING_STOCK' COMMENT '库存调整类型';

ALTER TABLE fin_stock_init_batch
    ADD COLUMN IF NOT EXISTS adjustment_direction VARCHAR(16) NULL COMMENT 'OTHER 类型库存方向 INCREASE/DECREASE';

SELECT '库存调整字段核验' AS reconciliation_type,
       COUNT(*) AS column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'fin_stock_init_batch'
  AND column_name IN ('adjustment_type', 'adjustment_direction');

SET @finance_parent_id := (SELECT menu_id FROM sys_menu WHERE menu_name = '财务管理' LIMIT 1);
UPDATE sys_menu
SET menu_name = '库存调整', remark = '库存调整管理'
WHERE parent_id = @finance_parent_id AND component = 'finance/stockInit/index' AND status = '0';

UPDATE sys_menu
SET menu_name = '库存调整查询'
WHERE parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '库存调整' AND parent_id = @finance_parent_id LIMIT 1)
  AND perms = 'finance:stockInit:query' AND status = '0';
