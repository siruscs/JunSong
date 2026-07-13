SET NAMES utf8mb4;

-- =====================================================================
-- 库存成本调整菜单与权限补齐
-- 可重复执行：所有 INSERT 使用 WHERE NOT EXISTS 守卫
-- 挂在既有库存报表菜单 menu_id=2155（finance:report:stock）下
--
-- 新增按钮权限：
--   库存成本调整 finance:stock:costAdjust
-- 授权给超级管理员(role_id=1)和财务角色(role_id=100)
-- =====================================================================

-- 以权限码为稳定键；menu_id 动态选择空闲 ID，避免不同环境已有菜单 ID 冲突
SET @stock_cost_adjust_perm := 'finance:stock:costAdjust';
SET @stock_cost_adjust_menu_id := (
    SELECT menu_id FROM sys_menu WHERE perms = @stock_cost_adjust_perm LIMIT 1
);
SET @stock_cost_adjust_menu_id := IFNULL(@stock_cost_adjust_menu_id, (
    SELECT GREATEST(IFNULL(MAX(menu_id), 0) + 1, 2158) FROM sys_menu
));

-- 库存成本调整权限（按钮类型，挂在库存报表菜单 2155 下）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT @stock_cost_adjust_menu_id, '库存成本调整', 2155, 3, '', '', 1, 0, 'F', '0', '0', @stock_cost_adjust_perm, '#', 'admin', sysdate(), '', NULL, '库存成本调整写权限（与报表读权限分离）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = @stock_cost_adjust_perm);

-- 授权给超级管理员(role_id=1)和财务角色(role_id=100)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, @stock_cost_adjust_menu_id FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = @stock_cost_adjust_menu_id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 100, @stock_cost_adjust_menu_id FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 100 AND menu_id = @stock_cost_adjust_menu_id);

-- 验证
SELECT menu_id, menu_name, perms, HEX(menu_name) as menu_name_hex
FROM sys_menu
WHERE perms IN ('finance:report:stock', @stock_cost_adjust_perm)
ORDER BY menu_id;
