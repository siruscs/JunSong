SET NAMES utf8mb4;

SET @add_adjustment_type_sql := IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fin_stock_init_batch' AND column_name = 'adjustment_type'),
    'SELECT 1',
    'ALTER TABLE fin_stock_init_batch ADD COLUMN adjustment_type VARCHAR(32) NOT NULL DEFAULT ''OPENING_STOCK'' COMMENT ''库存调整类型''');
PREPARE stmt FROM @add_adjustment_type_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_adjustment_direction_sql := IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fin_stock_init_batch' AND column_name = 'adjustment_direction'),
    'SELECT 1',
    'ALTER TABLE fin_stock_init_batch ADD COLUMN adjustment_direction VARCHAR(16) NULL COMMENT ''OTHER 类型库存方向 INCREASE/DECREASE''');
PREPARE stmt FROM @add_adjustment_direction_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

SET @stock_adjustment_menu_id := (SELECT menu_id FROM sys_menu WHERE menu_name = '库存调整' AND parent_id = @finance_parent_id LIMIT 1);
UPDATE sys_menu
SET menu_name = '库存调整查询'
WHERE parent_id = @stock_adjustment_menu_id
  AND perms = 'finance:stockInit:query' AND status = '0';

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '库存调整删除', @stock_adjustment_menu_id, 6, '#', '', 1, 0, 'F', '0', '0',
       'finance:stockInit:remove', '#', 'admin', NOW(), '仅未过账调整单可删除'
FROM DUAL
WHERE @stock_adjustment_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:stockInit:remove' AND parent_id = @stock_adjustment_menu_id);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
WHERE r.role_key IN ('finance', 'finance_staff', 'finance_manager')
  AND m.perms = 'finance:stockInit:remove'
  AND m.parent_id = @stock_adjustment_menu_id;
