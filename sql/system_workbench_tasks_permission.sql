-- =====================================================================
-- R6-G: 统一工作台任务聚合权限
-- 可重复执行：所有 INSERT 使用 NOT EXISTS 守卫
-- 仅授权超级管理员 role_id = 1
--
-- 说明：/system/workbench/tasks 工作台聚合接口校验
-- @RequiresPermissions("system:workbench:tasks")。本脚本补充按钮权限并授权给超管。
-- =====================================================================

SET NAMES utf8mb4;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '工作台任务', 1, 99, '', '', '', '',
  1, 0, 'F', '0', '0', 'system:workbench:tasks', '#',
  'admin', NOW(), '', NULL, '统一工作台任务权限'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'system:workbench:tasks' AND menu_type = 'F'
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms = 'system:workbench:tasks'
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm
  WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id
);
