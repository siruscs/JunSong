-- 低代码配置后台菜单（在"低代码应用"目录下）
-- 父目录 menu_id=2280 (低代码应用)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('配置后台', 2280, 1, 'admin', 'lowcode/admin/index', NULL, 'LowcodeAdmin', 1, 0, 'C', '0', '0', 'lowcode:meta:list', 'build', 'admin', NOW(), '低代码配置后台');
