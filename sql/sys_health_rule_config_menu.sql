-- R10-A: 自检规则配置菜单权限
-- 只授权 role_id=1

SET @parent_id := (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' LIMIT 1);
SET @menu_id := 2470;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
  menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT
  @menu_id, '自检规则配置', COALESCE(@parent_id, 0), 85, 'health-rule', 'system/healthRule/index', '', '',
  1, 0, 'C', '0', '0', 'system:healthRule:list', 'setting', 'admin', NOW(), '自检规则配置'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:healthRule:list');

INSERT INTO sys_menu (
  menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
  menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT '规则查询', @menu_id, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:healthRule:query', '#', 'admin', NOW(), '自检规则查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:healthRule:query');

INSERT INTO sys_menu (
  menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
  menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT '规则修改', @menu_id, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:healthRule:edit', '#', 'admin', NOW(), '自检规则修改'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:healthRule:edit');

-- 授权 role_id=1
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('system:healthRule:list', 'system:healthRule:query', 'system:healthRule:edit')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );
