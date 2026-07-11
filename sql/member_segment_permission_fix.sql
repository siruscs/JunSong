-- =====================================================================
-- R6-P0: 会员分层清单权限补齐
-- 可重复执行：所有 INSERT 使用 NOT EXISTS 守卫
-- 仅授权超级管理员 role_id = 1，不授予普通租户角色
--
-- 说明：/member/segment 分层钻取接口（MemberSegmentController.list）运行时校验
-- @RequiresPermissions("member:segment:list")，但后端菜单系统尚未配置对应按钮权限，
-- 影响正式授权与权限扫描治理。本脚本补充 member:segment:list 按钮权限并授权给超管。
-- =====================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------
-- 1. 查找会员信息菜单（父级为 member:member:list 的 C 类菜单）
-- -----------------------------------------------------------------
SET @memberRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'member' AND menu_type = 'M' LIMIT 1);
SET @memberListMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @memberRootId AND perms = 'member:member:list' AND menu_type = 'C' LIMIT 1);

-- -----------------------------------------------------------------
-- 2. 补 member:segment:list 按钮权限
-- -----------------------------------------------------------------
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '会员分层清单', @memberListMenuId, 99, '', '', '', '',
  1, 0, 'F', '0', '0', 'member:segment:list', '#',
  'admin', NOW(), '', NULL, '会员分层清单权限'
WHERE @memberListMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:segment:list' AND menu_type = 'F');

-- -----------------------------------------------------------------
-- 3. 授权给超级管理员
-- -----------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms = 'member:segment:list'
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm
  WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id
);
