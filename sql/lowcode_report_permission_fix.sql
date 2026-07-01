-- =====================================================================
-- 低代码报表权限补齐
-- 可重复执行：所有 INSERT 使用 NOT EXISTS 守卫
-- 仅授权超级管理员 role_id = 1，不授予普通租户角色
--
-- 说明：LcReportController 使用 @RequiresPermissions("lowcode:report:list"/"stat")
-- 在 /lowcode/report 路径下提供数据查询和统计接口。
-- =====================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------
-- 1. 查找低代码根目录
-- -----------------------------------------------------------------
SET @lcRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'lowcode' AND menu_type = 'M' LIMIT 1);

-- -----------------------------------------------------------------
-- 2. 创建报表管理页面菜单（如不存在）
-- -----------------------------------------------------------------
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '报表管理', @lcRootId, 5, 'report', '', '', '',
  1, 0, 'C', '0', '0', 'lowcode:report:list', 'chart',
  'admin', NOW(), '', NULL, '低代码报表管理（暂无前端页面，API-only）'
WHERE @lcRootId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @lcRootId AND perms = 'lowcode:report:list' AND menu_type = 'C');

SET @lcReportMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @lcRootId AND perms = 'lowcode:report:list' AND menu_type = 'C' LIMIT 1);

-- -----------------------------------------------------------------
-- 3. 补 lowcode:report:stat 按钮权限
--    (lowcode:report:list 已作为 C 类菜单权限在上方创建)
-- -----------------------------------------------------------------
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '报表统计', @lcReportMenuId, 1, '', '', '', '',
  1, 0, 'F', '0', '0', 'lowcode:report:stat', '#',
  'admin', NOW(), '', NULL, ''
WHERE @lcReportMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'lowcode:report:stat' AND menu_type = 'F');

-- -----------------------------------------------------------------
-- 4. 授权给超级管理员
-- -----------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('lowcode:report:list', 'lowcode:report:stat')
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm
  WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id
);
