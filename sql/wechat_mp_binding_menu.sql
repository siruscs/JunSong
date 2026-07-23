-- =====================================================================
-- 微信账号绑定管理菜单 + 按钮权限种子
-- 可重复执行：所有 INSERT 使用 NOT EXISTS 守卫
-- 仅授权超级管理员 role_id = 1
--
-- 背景：
--   Task 6 PC 绑定管理使用 system:user:unbindMp 权限查看绑定/管理员解绑
--   Task 6A 微信会话一键失效使用 system:user:wechatSession:revokeAll 权限
--   两权限均为独立操作权限，不复用 system:user:edit，需单独注册菜单
-- =====================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- =========================
-- 1. 微信解绑按钮 → 挂到用户管理（perms=system:user:list）
-- =========================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '微信解绑',
       (SELECT menu_id FROM sys_menu WHERE perms = 'system:user:list' AND menu_type = 'C' LIMIT 1),
       20, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:unbindMp', '#', 'admin', NOW(), 'PC 管理员解绑用户微信绑定（独立权限，不复用 system:user:edit）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:user:unbindMp')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:user:list' AND menu_type = 'C');

-- =========================
-- 2. 微信会话一键失效按钮 → 挂到用户管理（perms=system:user:list）
-- =========================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '微信会话失效',
       (SELECT menu_id FROM sys_menu WHERE perms = 'system:user:list' AND menu_type = 'C' LIMIT 1),
       21, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:wechatSession:revokeAll', '#', 'admin', NOW(), '一键使当前租户所有微信登录会话失效（独立权限，密码会话不受影响）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:user:wechatSession:revokeAll')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:user:list' AND menu_type = 'C');

-- =========================
-- 3. 授权给超管 role_id = 1（幂等）
-- =========================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.perms IN (
  'system:user:unbindMp',
  'system:user:wechatSession:revokeAll'
)
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm
  WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
);

-- =========================
-- 验证
-- =========================
SELECT '=== 微信绑定管理权限注册结果 ===' AS info;
SELECT menu_id, menu_name, parent_id, perms, menu_type, status
FROM sys_menu
WHERE perms IN (
  'system:user:unbindMp',
  'system:user:wechatSession:revokeAll'
)
ORDER BY perms;

SELECT '=== 超管授权检查（应均为 1 条）===' AS info;
SELECT m.perms, COUNT(rm.role_id) AS admin_granted
FROM sys_menu m
LEFT JOIN sys_role_menu rm ON m.menu_id = rm.menu_id AND rm.role_id = 1
WHERE m.perms IN (
  'system:user:unbindMp',
  'system:user:wechatSession:revokeAll'
)
GROUP BY m.perms;

-- 注意：执行本 SQL 后，需要让超管用户重新登录刷新权限缓存，
-- 或调用 /system/menu/getRouters + /system/user/getInfo 刷新前端权限。
-- 建议同时跑权限缺口扫描脚本（scripts/permission-menu-gap-health.mjs）确认无缺口。
