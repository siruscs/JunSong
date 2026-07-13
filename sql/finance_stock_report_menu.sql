SET NAMES utf8mb4;

-- =====================================================================
-- 库存报表菜单与权限补齐
-- 可重复执行：所有 INSERT 使用 WHERE NOT EXISTS 守卫
-- 挂在既有库存报表菜单 menu_id=2155（finance:report:stock）下
--
-- 新增按钮权限：
--   1) 库存报表导出 finance:report:stock:export (menu_id=2156)
--   2) 库存对账     finance:stock:reconciliation    (menu_id=2157)
-- 授权给超级管理员(role_id=1)和财务角色(role_id=100)
-- =====================================================================

-- 库存报表导出权限（按钮类型，挂在库存报表菜单 2155 下）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2156, '库存报表导出', 2155, 1, '', '', 1, 0, 'F', '0', '0', 'finance:report:stock:export', '#', 'admin', sysdate(), '', NULL, '库存报表导出权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2156);

-- 库存对账权限（按钮类型）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2157, '库存对账', 2155, 2, '', '', 1, 0, 'F', '0', '0', 'finance:stock:reconciliation', '#', 'admin', sysdate(), '', NULL, '库存对账权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2157);

-- 授权给超级管理员(role_id=1)和财务角色(role_id=100)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2156 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2156);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 100, 2156 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 100 AND menu_id = 2156);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2157 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2157);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 100, 2157 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 100 AND menu_id = 2157);

-- 验证
SELECT menu_id, menu_name, perms, HEX(menu_name) as menu_name_hex FROM sys_menu WHERE menu_id IN (2155, 2156, 2157);
