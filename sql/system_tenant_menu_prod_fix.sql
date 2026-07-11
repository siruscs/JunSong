-- ============================================================
-- PROD 租户管理菜单补齐
-- 幂等执行: 补齐系统管理下的租户管理菜单及按钮权限,仅授权平台管理员 role_id=1。
-- ============================================================

-- 1. 租户管理 C 菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '租户管理',
       (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0 LIMIT 1),
       90, 'tenant', 'system/tenant/index', '', '', 1, 0, 'C', '0', '0', 'system:tenant:list', 'peoples', 'admin', NOW(), '租户主体管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0);

-- 2. 修正历史存在但路由/父级不完整的租户管理菜单
UPDATE sys_menu SET
  parent_id = (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0 LIMIT 1) t),
  path = 'tenant',
  component = 'system/tenant/index',
  menu_name = '租户管理',
  menu_type = 'C',
  visible = '0',
  status = '0',
  update_by = 'admin',
  update_time = NOW()
WHERE perms = 'system:tenant:list'
  AND EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0 LIMIT 1) t);

-- 3. 租户管理按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '租户查询', (SELECT menu_id FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C' LIMIT 1),
       1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:tenant:query', '#', 'admin', NOW(), '租户查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:query')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '租户新增', (SELECT menu_id FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C' LIMIT 1),
       2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:tenant:add', '#', 'admin', NOW(), '租户新增'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:add')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '租户修改', (SELECT menu_id FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C' LIMIT 1),
       3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:tenant:edit', '#', 'admin', NOW(), '租户修改'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:edit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '租户删除', (SELECT menu_id FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C' LIMIT 1),
       4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:tenant:remove', '#', 'admin', NOW(), '租户删除'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:remove')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C');

-- 4. 仅平台管理员可见/可用
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('system:tenant:list', 'system:tenant:query', 'system:tenant:add', 'system:tenant:edit', 'system:tenant:remove')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );

DELETE rm
FROM sys_role_menu rm
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE rm.role_id <> 1
  AND m.perms IN ('system:tenant:list', 'system:tenant:query', 'system:tenant:add', 'system:tenant:edit', 'system:tenant:remove');
