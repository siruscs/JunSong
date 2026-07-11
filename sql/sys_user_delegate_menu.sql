-- =====================================================================
-- 用户委托代理菜单 + 按钮权限种子
-- 可重复执行：所有 INSERT 使用 NOT EXISTS 守卫
-- 仅授权超级管理员 role_id = 1
--
-- 背景：/system/delegate 路由在 dynamicRoutes.ts 带
--   meta: { permissions: ['system:delegate:list'] }，
--   后端 SysUserDelegateController 校验 @RequiresPermissions("system:delegate:xxx")。
--   原始 sys_user_delegate.sql 仅建表，未种菜单，导致非超管用户
--   无权限 → 前端路由被 filterDynamicRoutes 过滤 → 点击"委托设置"404。
-- 本脚本补充菜单与按钮权限并授权给超管。
-- =====================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1) 隐藏 C 菜单：system:delegate:list（挂在"系统管理" parent_id=1 下，不显示在左侧）
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '委托设置', 1, 99, 'delegate', 'system/delegate/index', '', '',
  1, 0, 'C', '1', '0', 'system:delegate:list', '#',
  'admin', NOW(), '', NULL, '用户委托代理（头像下拉入口，隐藏菜单）'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'system:delegate:list' AND menu_type = 'C'
);

-- 2) 按钮权限 F：query/add/edit/remove（挂在上述 C 菜单下）
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '委托查询', (SELECT menu_id FROM sys_menu WHERE perms = 'system:delegate:list' AND menu_type = 'C' LIMIT 1),
  1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:delegate:query', '#',
  'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'system:delegate:query' AND menu_type = 'F'
);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '委托新增', (SELECT menu_id FROM sys_menu WHERE perms = 'system:delegate:list' AND menu_type = 'C' LIMIT 1),
  2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:delegate:add', '#',
  'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'system:delegate:add' AND menu_type = 'F'
);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '委托修改', (SELECT menu_id FROM sys_menu WHERE perms = 'system:delegate:list' AND menu_type = 'C' LIMIT 1),
  3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:delegate:edit', '#',
  'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'system:delegate:edit' AND menu_type = 'F'
);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '委托删除', (SELECT menu_id FROM sys_menu WHERE perms = 'system:delegate:list' AND menu_type = 'C' LIMIT 1),
  4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:delegate:remove', '#',
  'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'system:delegate:remove' AND menu_type = 'F'
);

-- 3) 授权给超管 role_id = 1（幂等）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.perms IN (
  'system:delegate:list',
  'system:delegate:query',
  'system:delegate:add',
  'system:delegate:edit',
  'system:delegate:remove'
)
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm
  WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
);
