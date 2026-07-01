-- =====================================================================
-- 会员 PII 导出权限补齐
-- 可重复执行：所有 INSERT 使用 NOT EXISTS 守卫
-- 仅授权超级管理员 role_id = 1，不授予普通租户角色
--
-- 说明：member:member:pii（查看明文）已在 permission_fix_three_modules.sql 中创建。
-- 本脚本补充 member:member:piiExport（明文导出），用于 MemMemberController.export()
-- 中的 AuthUtil.hasPermi("member:member:piiExport") 运行时检查。
-- =====================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------
-- 1. 查找会员信息菜单
-- -----------------------------------------------------------------
SET @memberRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'member' AND menu_type = 'M' LIMIT 1);
SET @memberListMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @memberRootId AND perms = 'member:member:list' AND menu_type = 'C' LIMIT 1);

-- -----------------------------------------------------------------
-- 2. 补 member:member:piiExport 按钮权限
-- -----------------------------------------------------------------
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '导出会员敏感信息', @memberListMenuId, 11, '', '', '', '',
  1, 0, 'F', '0', '0', 'member:member:piiExport', '#',
  'admin', NOW(), '', NULL, '导出时查看明文PII（手机号/身份证/地址）'
WHERE @memberListMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:member:piiExport' AND menu_type = 'F');

-- -----------------------------------------------------------------
-- 3. 授权给超级管理员
-- -----------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms = 'member:member:piiExport'
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm
  WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id
);
