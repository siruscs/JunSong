-- 门店地图管理菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
(2294, '门店地图', 1, 20, 'storeMap', NULL, NULL, NULL, 1, 0, 'M', '0', '0', '', 'location', 'admin', NOW(), '门店地图管理'),
(2295, '门店地图查询', 2294, 1, 'stores', 'system/storeMap/index', NULL, NULL, 1, 0, 'C', '0', '0', 'system:storeMap:list', '#', 'admin', NOW(), '门店地图查询'),
(2296, '门店密度查询', 2294, 2, 'density', 'system/storeMap/density', NULL, NULL, 1, 0, 'C', '0', '0', 'system:storeMap:density', '#', 'admin', NOW(), '门店密度查询');

-- 给管理员角色授权
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 2294), (1, 2295), (1, 2296);
