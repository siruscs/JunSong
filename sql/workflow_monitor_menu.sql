-- 流程监控仪表盘菜单注册
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2370, '流程监控', 2220, 10, 'monitor', 'workflow/monitor/index', '', 1, 0, 'C', '0', '0', 'workflow:monitor:list', 'monitor', 'admin', NOW(), '流程运维监控仪表盘');

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2370);
