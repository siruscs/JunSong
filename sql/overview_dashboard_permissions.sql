-- 三大模块概览页 dashboard 权限（幂等）
-- 系统概览: monitor:dashboard:view
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2390, '系统概览统计', 2, 10, 'dashboard/stats', '', 'F', '0', '0', 'monitor:dashboard:view', '#', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'monitor:dashboard:view');

-- 会员概览: member:dashboard:list
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2391, '会员概览统计', 2076, 10, 'dashboard/stats', '', 'F', '0', '0', 'member:dashboard:list', '#', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:dashboard:list');

-- 财务概览: finance:dashboard:list
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2392, '财务概览统计', 108, 10, 'dashboard/stats', '', 'F', '0', '0', 'finance:dashboard:list', '#', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:dashboard:list');

-- 授权给管理员角色
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2390 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2390);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2391 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2391);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2392 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2392);

-- 会员首页统计是非管理员首页的数据源；授权给已经拥有会员模块权限的角色，避免登录后首页三条接口无权限。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m_dashboard.menu_id
FROM sys_role_menu rm
JOIN sys_menu m_member ON m_member.menu_id = rm.menu_id
JOIN sys_menu m_dashboard ON m_dashboard.perms = 'member:dashboard:list'
WHERE (m_member.menu_id = 2076 OR m_member.parent_id = 2076 OR m_member.perms LIKE 'member:%')
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu exists_rm
    WHERE exists_rm.role_id = rm.role_id
      AND exists_rm.menu_id = m_dashboard.menu_id
  );
