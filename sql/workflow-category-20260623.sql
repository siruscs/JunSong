-- 工作流流程分类字典类型（使用auto_increment）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
VALUES ('流程分类', 'wf_category', '0', 'admin', NOW(), '工作流流程分类');

-- 工作流流程分类字典数据
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
VALUES
(1, '财务流程', 'finance', 'wf_category', '', 'danger', 'N', '0', 'admin', NOW()),
(2, '人力资源流程', 'hr', 'wf_category', '', 'warning', 'N', '0', 'admin', NOW()),
(3, '行政流程', 'admin', 'wf_category', '', 'primary', 'N', '0', 'admin', NOW()),
(4, '门店运营流程', 'store', 'wf_category', '', 'success', 'N', '0', 'admin', NOW()),
(5, '其他流程', 'other', 'wf_category', '', 'info', 'N', '0', 'admin', NOW());

-- 发起新流程菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
(2297, '发起新流程', 2220, 0, 'start', 'workflow/start/index', NULL, NULL, 1, 0, 'C', '0', '0', 'workflow:instance:add', 'guide', 'admin', NOW(), '发起新流程');

-- 给管理员角色授权
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2297);
