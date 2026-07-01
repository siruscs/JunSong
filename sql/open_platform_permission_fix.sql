-- =====================================================================
-- 开放平台权限补齐：ISV 管理、合约管理、API 日志
-- 可重复执行：所有 INSERT 使用 NOT EXISTS 守卫
-- 仅授权超级管理员 role_id = 1，不授予普通租户角色
-- =====================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------
-- 1. 开放平台根目录（如不存在则创建）
-- -----------------------------------------------------------------
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '开放平台', 0, 5, 'open', NULL, '', '',
  1, 0, 'M', '0', '0', '', 'link',
  'admin', NOW(), '', NULL, '开放平台目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 0 AND path = 'open' AND menu_type = 'M');

SET @openRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'open' AND menu_type = 'M' LIMIT 1);

-- =================================================================
-- 2. ISV 管理
-- =================================================================

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  'ISV管理', @openRootId, 2, 'isv', '', '', '',
  1, 0, 'C', '0', '0', 'open:isv:list', 'peoples',
  'admin', NOW(), '', NULL, 'ISV注册管理（暂无前端页面，API-only）'
WHERE @openRootId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @openRootId AND perms = 'open:isv:list' AND menu_type = 'C');

SET @isvMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @openRootId AND perms = 'open:isv:list' AND menu_type = 'C' LIMIT 1);

-- open:isv:query
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  'ISV查询', @isvMenuId, 1, '', '', '', '',
  1, 0, 'F', '0', '0', 'open:isv:query', '#',
  'admin', NOW(), '', NULL, ''
WHERE @isvMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:query' AND menu_type = 'F');

-- open:isv:add
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  'ISV新增', @isvMenuId, 2, '', '', '', '',
  1, 0, 'F', '0', '0', 'open:isv:add', '#',
  'admin', NOW(), '', NULL, ''
WHERE @isvMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:add' AND menu_type = 'F');

-- open:isv:edit
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  'ISV修改', @isvMenuId, 3, '', '', '', '',
  1, 0, 'F', '0', '0', 'open:isv:edit', '#',
  'admin', NOW(), '', NULL, ''
WHERE @isvMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:edit' AND menu_type = 'F');

-- open:isv:approve
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  'ISV审核', @isvMenuId, 4, '', '', '', '',
  1, 0, 'F', '0', '0', 'open:isv:approve', '#',
  'admin', NOW(), '', NULL, ''
WHERE @isvMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:approve' AND menu_type = 'F');

-- open:isv:remove
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  'ISV删除', @isvMenuId, 5, '', '', '', '',
  1, 0, 'F', '0', '0', 'open:isv:remove', '#',
  'admin', NOW(), '', NULL, ''
WHERE @isvMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:remove' AND menu_type = 'F');

-- =================================================================
-- 3. 合约管理
-- =================================================================

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '合约管理', @openRootId, 3, 'contract', '', '', '',
  1, 0, 'C', '0', '0', 'open:contract:list', 'documentation',
  'admin', NOW(), '', NULL, '开放平台合约管理（暂无前端页面，API-only）'
WHERE @openRootId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @openRootId AND perms = 'open:contract:list' AND menu_type = 'C');

SET @contractMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @openRootId AND perms = 'open:contract:list' AND menu_type = 'C' LIMIT 1);

-- open:contract:query
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '合约查询', @contractMenuId, 1, '', '', '', '',
  1, 0, 'F', '0', '0', 'open:contract:query', '#',
  'admin', NOW(), '', NULL, ''
WHERE @contractMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:contract:query' AND menu_type = 'F');

-- open:contract:add
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '合约新增', @contractMenuId, 2, '', '', '', '',
  1, 0, 'F', '0', '0', 'open:contract:add', '#',
  'admin', NOW(), '', NULL, ''
WHERE @contractMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:contract:add' AND menu_type = 'F');

-- open:contract:edit
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '合约修改', @contractMenuId, 3, '', '', '', '',
  1, 0, 'F', '0', '0', 'open:contract:edit', '#',
  'admin', NOW(), '', NULL, ''
WHERE @contractMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:contract:edit' AND menu_type = 'F');

-- open:contract:remove
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '合约删除', @contractMenuId, 4, '', '', '', '',
  1, 0, 'F', '0', '0', 'open:contract:remove', '#',
  'admin', NOW(), '', NULL, ''
WHERE @contractMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:contract:remove' AND menu_type = 'F');

-- =================================================================
-- 4. API 调用日志
-- =================================================================

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  'API调用日志', @openRootId, 4, 'log', 'open/log/index', '', '',
  1, 0, 'C', '0', '0', 'open:log:list', 'log',
  'admin', NOW(), '', NULL, '开放平台API调用日志'
WHERE @openRootId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @openRootId AND perms = 'open:log:list' AND menu_type = 'C');

SET @logMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @openRootId AND perms = 'open:log:list' AND menu_type = 'C' LIMIT 1);

-- open:log:export
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '日志导出', @logMenuId, 1, '', '', '', '',
  1, 0, 'F', '0', '0', 'open:log:export', '#',
  'admin', NOW(), '', NULL, ''
WHERE @logMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:log:export' AND menu_type = 'F');

-- =================================================================
-- 5. 授权：将所有新增权限授予超级管理员角色 (role_id = 1)
--    不得授予普通租户角色
-- =================================================================

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN (
  'open:isv:list',
  'open:isv:query',
  'open:isv:add',
  'open:isv:edit',
  'open:isv:approve',
  'open:isv:remove',
  'open:contract:list',
  'open:contract:query',
  'open:contract:add',
  'open:contract:edit',
  'open:contract:remove',
  'open:log:list',
  'open:log:export'
)
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm
  WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id
);
