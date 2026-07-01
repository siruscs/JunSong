-- NIGHT-P1-E Repair SQL: Three Overview Menu & Permission Health Fix
-- Idempotent: safe to run multiple times.
-- Scope:
--   1. Repair R2 decision-console button permissions by perms, not hardcoded menu_id.
--   2. Repair operation dashboard parent under finance root.
--   3. Grant member dashboard permission to roles that already have member-module access.

SET @financeRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'finance' AND menu_type = 'M' LIMIT 1);
SET @operationMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'finance:dashboard:operation' AND menu_type = 'C' LIMIT 1);
SET @memberRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'member' AND menu_type = 'M' LIMIT 1);

UPDATE sys_menu
SET parent_id = @financeRootId,
    path = 'operation',
    component = 'finance/overview/index',
    update_by = 'admin',
    update_time = NOW()
WHERE @financeRootId IS NOT NULL
  AND @operationMenuId IS NOT NULL
  AND menu_id = @operationMenuId
  AND (parent_id IS NULL OR parent_id <> @financeRootId OR path <> 'operation' OR component <> 'finance/overview/index');

-- finance:dashboard:alerts
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '经营预警中心', @operationMenuId, 1, '', NULL, 1, 0, 'F', '0', '0', 'finance:dashboard:alerts', '#', 'admin', NOW(), '', NULL, '经营预警中心查询权限'
FROM DUAL
WHERE @operationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:dashboard:alerts');

-- finance:dashboard:reviewTasks
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '复盘任务', @operationMenuId, 2, '', NULL, 1, 0, 'F', '0', '0', 'finance:dashboard:reviewTasks', '#', 'admin', NOW(), '', NULL, '复盘任务查询权限'
FROM DUAL
WHERE @operationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:dashboard:reviewTasks');

-- finance:drilldown:sales
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '销售钻取', @operationMenuId, 3, '', NULL, 1, 0, 'F', '0', '0', 'finance:drilldown:sales', '#', 'admin', NOW(), '', NULL, '销售明细钻取查询权限'
FROM DUAL
WHERE @operationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:drilldown:sales');

-- finance:drilldown:expenses
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '费用钻取', @operationMenuId, 4, '', NULL, 1, 0, 'F', '0', '0', 'finance:drilldown:expenses', '#', 'admin', NOW(), '', NULL, '费用明细钻取查询权限'
FROM DUAL
WHERE @operationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:drilldown:expenses');

-- finance:drilldown:profitShare
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '分润钻取', @operationMenuId, 5, '', NULL, 1, 0, 'F', '0', '0', 'finance:drilldown:profitShare', '#', 'admin', NOW(), '', NULL, '分润明细钻取查询权限'
FROM DUAL
WHERE @operationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:drilldown:profitShare');

UPDATE sys_menu
SET parent_id = @operationMenuId,
    update_by = 'admin',
    update_time = NOW()
WHERE @operationMenuId IS NOT NULL
  AND perms IN (
    'finance:dashboard:alerts',
    'finance:dashboard:reviewTasks',
    'finance:drilldown:sales',
    'finance:drilldown:expenses',
    'finance:drilldown:profitShare'
  )
  AND (parent_id IS NULL OR parent_id <> @operationMenuId);

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
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu e WHERE e.role_id = 1 AND e.menu_id = m.menu_id);

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

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.menu_id
FROM sys_role_menu rm
JOIN sys_menu m_member ON m_member.menu_id = rm.menu_id
JOIN sys_menu m ON m.perms = 'member:dashboard:list'
WHERE @memberRootId IS NOT NULL
  AND (m_member.menu_id = @memberRootId OR m_member.parent_id = @memberRootId OR m_member.perms LIKE 'member:%')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu e
    WHERE e.role_id = rm.role_id AND e.menu_id = m.menu_id
  );

-- Verification queries:
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE perms IN ('finance:dashboard:alerts','finance:dashboard:reviewTasks','finance:drilldown:sales','finance:drilldown:expenses','finance:drilldown:profitShare') ORDER BY order_num;
-- SELECT rm.role_id, m.perms FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id WHERE m.perms LIKE 'finance:dashboard:%' OR m.perms LIKE 'finance:drilldown:%' ORDER BY m.perms, rm.role_id;
