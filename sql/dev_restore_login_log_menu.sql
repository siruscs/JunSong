SET NAMES utf8mb4;
START TRANSACTION;

-- DEV 修复：登录日志子菜单仍存在，但其历史父 ID=108 已被复用为“财务管理”，导致菜单树挂错。
-- 使用未占用的新父 ID，避免覆盖财务菜单；保留并修正已有登录日志及按钮权限。
INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 3230, '日志管理', 0, 9, 'log', '', '', 1, 0,
       'M', '0', '0', '', 'log', 'admin', NOW(), '日志管理目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3230);

UPDATE sys_menu
SET parent_id = 3230, order_num = 2
WHERE menu_id = 501 AND path = 'logininfor' AND perms = 'system:logininfor:list';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, 3230
FROM sys_role_menu rm
WHERE rm.menu_id = 501;

COMMIT;

SELECT menu_id, menu_name, parent_id, path, component, perms, visible, status
FROM sys_menu
WHERE menu_id IN (3230, 501, 1042, 1043, 1044, 1045)
ORDER BY menu_id;
