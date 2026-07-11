-- =====================================================
-- R7-D 轻量现金流看板权限菜单
-- 幂等：重复执行不会产生重复数据
-- 注意：必须挂在"经营总览"(finance:dashboard:operation) 下，作为功能按钮(F)
-- =====================================================

-- 查找经营总览菜单（perms='finance:dashboard:operation'）
SET @parent_menu_id = (SELECT menu_id FROM sys_menu WHERE perms = 'finance:dashboard:operation' AND menu_type = 'C' LIMIT 1);

-- 新增 finance:cashflow:dashboard 权限菜单（类型 F，挂在经营总览下）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '现金流看板', @parent_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'finance:cashflow:dashboard', '#', 'admin', NOW(), '', NULL, 'R7-D 轻量现金流看板'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'finance:cashflow:dashboard'
);

-- 给所有拥有 finance:dashboard:operation 权限的角色也授予 finance:cashflow:dashboard
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m2.menu_id
FROM sys_role_menu rm
JOIN sys_menu m1 ON m1.menu_id = rm.menu_id AND m1.perms = 'finance:dashboard:operation'
JOIN sys_menu m2 ON m2.perms = 'finance:cashflow:dashboard'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm2
    WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m2.menu_id
);

-- 也给 admin（role_id=1）授权
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms = 'finance:cashflow:dashboard'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
