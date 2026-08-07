-- 会员购买退货/退款权限。退货是购买域独立业务单，不改写原购买单事实。
SET NAMES utf8mb4;

SET @purchase_menu_id := (SELECT menu_id FROM sys_menu WHERE perms = 'member:purchase:list' AND menu_type = 'C' LIMIT 1);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '会员购买退货', @purchase_menu_id, 9, '', '', 1, 0, 'F', '0', '0',
       'member:purchaseReturn:list', '#', 'admin', NOW(), '', NULL, '查询会员购买退货单'
FROM DUAL WHERE @purchase_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:purchaseReturn:list' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '查询退货详情', @purchase_menu_id, 10, '', '', 1, 0, 'F', '0', '0',
       'member:purchaseReturn:query', '#', 'admin', NOW(), '', NULL, '查看退货明细和含赠退款单价'
FROM DUAL WHERE @purchase_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:purchaseReturn:query' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '新建退货单', @purchase_menu_id, 11, '', '', 1, 0, 'F', '0', '0',
       'member:purchaseReturn:add', '#', 'admin', NOW(), '', NULL, '按原购买单剩余正品和赠品数量办理退货'
FROM DUAL WHERE @purchase_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:purchaseReturn:add' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '完成退货单', @purchase_menu_id, 12, '', '', 1, 0, 'F', '0', '0',
       'member:purchaseReturn:complete', '#', 'admin', NOW(), '', NULL, '将退货退款草稿单完成入账'
FROM DUAL WHERE @purchase_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:purchaseReturn:complete' AND menu_type = 'F');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('member:purchaseReturn:list', 'member:purchaseReturn:query', 'member:purchaseReturn:add', 'member:purchaseReturn:complete')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id);

SELECT 'member_purchase_return_menu' AS check_name, COUNT(*) AS permission_count
FROM sys_menu WHERE perms LIKE 'member:purchaseReturn:%';
