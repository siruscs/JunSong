SET NAMES utf8mb4;

START TRANSACTION;

SET @overviewMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'finance:overview:list' LIMIT 1);
SET @baseMenuId := (SELECT COALESCE(MAX(menu_id), 0) FROM sys_menu);

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT @baseMenuId + 1, '经营数据查询', @overviewMenuId, 1, '', '', '', '0', '0', 'F', '0', '0', 'finance:dashboard:operation', '#', 'admin', NOW(), '', NULL, '财务概览经营数据查询权限'
 WHERE @overviewMenuId IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:dashboard:operation');

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT @baseMenuId + 2, '经营预警查询', @overviewMenuId, 2, '', '', '', '0', '0', 'F', '0', '0', 'finance:dashboard:alerts', '#', 'admin', NOW(), '', NULL, '财务概览经营预警查询权限'
 WHERE @overviewMenuId IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:dashboard:alerts');

INSERT INTO sys_menu
  (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT @baseMenuId + 3, '复盘任务查询', @overviewMenuId, 3, '', '', '', '0', '0', 'F', '0', '0', 'finance:dashboard:reviewTasks', '#', 'admin', NOW(), '', NULL, '财务概览复盘任务查询权限'
 WHERE @overviewMenuId IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:dashboard:reviewTasks');

INSERT INTO sys_role_menu(role_id, menu_id)
SELECT r.role_id, m.menu_id
  FROM sys_role r JOIN sys_menu m
 WHERE r.role_name = '店长主管'
   AND m.perms IN ('finance:dashboard:operation', 'finance:dashboard:alerts', 'finance:dashboard:reviewTasks')
   AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.role_id AND x.menu_id = m.menu_id);

COMMIT;

SELECT menu_id, parent_id, menu_name, perms FROM sys_menu
 WHERE perms IN ('finance:dashboard:operation', 'finance:dashboard:alerts', 'finance:dashboard:reviewTasks');
SELECT role_id, menu_id FROM sys_role_menu
 WHERE role_id IN (SELECT role_id FROM sys_role WHERE role_name = '店长主管')
   AND menu_id IN (SELECT menu_id FROM sys_menu WHERE perms LIKE 'finance:dashboard:%');
