SET @system_menu_id := (SELECT menu_id FROM sys_menu WHERE menu_name = '系统管理' AND parent_id = 0 LIMIT 1);
SET @region_menu_id := (SELECT menu_id FROM sys_menu WHERE menu_name = '地址维护' AND parent_id = @system_menu_id LIMIT 1);

INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '地址维护', @system_menu_id, 7, 'region', 'system/region/index', NULL, 'Region', 1, 0, 'C', '0', '0', 'system:region:list', 'tree', 'admin', NOW(), '', NULL, '地址维护菜单'
WHERE @region_menu_id IS NULL;

SET @region_menu_id := (SELECT menu_id FROM sys_menu WHERE menu_name = '地址维护' AND parent_id = @system_menu_id LIMIT 1);

INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '地址查询', @region_menu_id, 1, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:region:query', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @region_menu_id AND perms = 'system:region:query');

INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '地址新增', @region_menu_id, 2, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:region:add', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @region_menu_id AND perms = 'system:region:add');

INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '地址修改', @region_menu_id, 3, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:region:edit', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @region_menu_id AND perms = 'system:region:edit');

INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '地址删除', @region_menu_id, 4, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:region:remove', '#', 'admin', NOW(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @region_menu_id AND perms = 'system:region:remove');
