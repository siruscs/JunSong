-- =====================================================================
-- R6-E: 库存底座健康检查权限
-- 可重复执行：所有 INSERT 使用 NOT EXISTS 守卫
-- 仅授权超级管理员 role_id = 1
--
-- 说明：/finance/stock/health 库存健康检查接口校验
-- @RequiresPermissions("finance:stock:health")。本脚本补充按钮权限并授权给超管。
-- 本轮不开放正式库存报表，仅诊断底座健康。
-- =====================================================================

SET NAMES utf8mb4;

-- 查找财务菜单根节点（M 类，path=finance）
SET @financeRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'finance' AND menu_type = 'M' LIMIT 1);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '库存健康检查', COALESCE(@financeRootId, 0), 99, '', '', '', '',
  1, 0, 'F', '0', '0', 'finance:stock:health', '#',
  'admin', NOW(), '', NULL, '库存底座健康检查权限'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'finance:stock:health' AND menu_type = 'F'
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms = 'finance:stock:health'
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm
  WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id
);
