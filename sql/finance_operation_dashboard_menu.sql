-- finance_operation_dashboard_menu.sql
-- 幂等插入经营总览菜单和权限
-- 回滚顺序：先删 sys_role_menu，再删 sys_menu

SET @financeRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'finance' AND menu_type = 'M' LIMIT 1);
SET @operationMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'finance:dashboard:operation' AND menu_type = 'C' LIMIT 1);

-- 经营总览菜单（挂在财务管理根菜单下，按 perms 幂等）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '经营总览', @financeRootId, 0, 'operation', 'finance/overview/index', 1, 0, 'C', '0', '0', 'finance:dashboard:operation', 'dashboard', 'admin', sysdate(), '', NULL, '经营决策总览看板'
FROM DUAL
WHERE @financeRootId IS NOT NULL
  AND @operationMenuId IS NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:dashboard:operation' AND menu_type = 'C');

SET @operationMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'finance:dashboard:operation' AND menu_type = 'C' LIMIT 1);

UPDATE sys_menu
SET parent_id = @financeRootId,
    path = 'operation',
    component = 'finance/overview/index',
    update_by = 'admin',
    update_time = sysdate()
WHERE @financeRootId IS NOT NULL
  AND menu_id = @operationMenuId
  AND (parent_id IS NULL OR parent_id <> @financeRootId OR path <> 'operation' OR component <> 'finance/overview/index');

-- 授权 admin 角色和财务角色
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, @operationMenuId FROM DUAL
WHERE @operationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = @operationMenuId);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 100, @operationMenuId FROM DUAL
WHERE @operationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 100 AND menu_id = @operationMenuId);

-- 回滚 SQL（注释保留）:
-- DELETE rm FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id WHERE m.perms = 'finance:dashboard:operation';
-- DELETE FROM sys_menu WHERE perms = 'finance:dashboard:operation' AND menu_type = 'C';
