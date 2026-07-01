-- decision_console_r2_alerts_review_menu.sql
-- 幂等插入经营预警中心、复盘任务和钻取按钮权限。
-- 使用 perms 作为幂等依据，避免 menu_id 与报表菜单碰撞。
-- 回滚顺序：先删 sys_role_menu，再删 sys_menu。

SET @operationMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'finance:dashboard:operation' AND menu_type = 'C' LIMIT 1);

-- 经营预警中心（按钮权限）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '经营预警中心', @operationMenuId, 1, '', NULL, 1, 0, 'F', '0', '0', 'finance:dashboard:alerts', '#', 'admin', sysdate(), '', NULL, '经营预警中心查询权限'
FROM DUAL
WHERE @operationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:dashboard:alerts');

-- 复盘任务（按钮权限）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '复盘任务', @operationMenuId, 2, '', NULL, 1, 0, 'F', '0', '0', 'finance:dashboard:reviewTasks', '#', 'admin', sysdate(), '', NULL, '复盘任务查询权限'
FROM DUAL
WHERE @operationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:dashboard:reviewTasks');

-- 钻取-销售明细（按钮权限）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '销售钻取', @operationMenuId, 3, '', NULL, 1, 0, 'F', '0', '0', 'finance:drilldown:sales', '#', 'admin', sysdate(), '', NULL, '销售明细钻取查询权限'
FROM DUAL
WHERE @operationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:drilldown:sales');

-- 钻取-费用明细（按钮权限）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '费用钻取', @operationMenuId, 4, '', NULL, 1, 0, 'F', '0', '0', 'finance:drilldown:expenses', '#', 'admin', sysdate(), '', NULL, '费用明细钻取查询权限'
FROM DUAL
WHERE @operationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:drilldown:expenses');

-- 钻取-分润明细（按钮权限）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '分润钻取', @operationMenuId, 5, '', NULL, 1, 0, 'F', '0', '0', 'finance:drilldown:profitShare', '#', 'admin', sysdate(), '', NULL, '分润明细钻取查询权限'
FROM DUAL
WHERE @operationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:drilldown:profitShare');

-- 已存在但父级漂移的按钮，统一挂回经营总览。
UPDATE sys_menu
SET parent_id = @operationMenuId,
    update_by = 'admin',
    update_time = sysdate()
WHERE @operationMenuId IS NOT NULL
  AND perms IN (
    'finance:dashboard:alerts',
    'finance:dashboard:reviewTasks',
    'finance:drilldown:sales',
    'finance:drilldown:expenses',
    'finance:drilldown:profitShare'
  )
  AND (parent_id IS NULL OR parent_id <> @operationMenuId);

-- 授权超级管理员角色（role_id = 1）。经营决策台按钮和钻取权限不默认授予普通租户角色。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN (
    'finance:dashboard:alerts',
    'finance:dashboard:reviewTasks',
    'finance:drilldown:sales',
    'finance:drilldown:expenses',
    'finance:drilldown:profitShare'
  )
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);

-- 若旧版本已误授权给普通角色，仅按经营决策台权限收紧，避免误伤 2411-2413 的合法报表菜单。
DELETE rm
FROM sys_role_menu rm
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE rm.role_id <> 1
  AND m.perms IN (
    'finance:dashboard:alerts',
    'finance:dashboard:reviewTasks',
    'finance:drilldown:sales',
    'finance:drilldown:expenses',
    'finance:drilldown:profitShare'
  );

-- 回滚 SQL（注释保留）:
-- DELETE rm FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id
-- WHERE m.perms IN ('finance:dashboard:alerts','finance:dashboard:reviewTasks','finance:drilldown:sales','finance:drilldown:expenses','finance:drilldown:profitShare');
-- DELETE FROM sys_menu
-- WHERE perms IN ('finance:dashboard:alerts','finance:dashboard:reviewTasks','finance:drilldown:sales','finance:drilldown:expenses','finance:drilldown:profitShare');
