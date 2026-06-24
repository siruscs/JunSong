-- 先查询财务管理菜单的ID
SET @parent_menu_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '财务管理' LIMIT 1);

-- 费用记录菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('费用记录', @parent_menu_id, 4, 'expense', 'finance/expense/index', 1, 0, 'C', '0', '0', 'finance:expense:list', 'shopping', 'admin', NOW(), '', NULL, '费用记录菜单');

SET @expense_menu_id = LAST_INSERT_ID();

-- 费用记录子菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES 
('费用查询', @expense_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'finance:expense:query', '#', 'admin', NOW(), '', NULL, ''),
('费用新增', @expense_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'finance:expense:add', '#', 'admin', NOW(), '', NULL, ''),
('费用修改', @expense_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'finance:expense:edit', '#', 'admin', NOW(), '', NULL, ''),
('费用删除', @expense_menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'finance:expense:remove', '#', 'admin', NOW(), '', NULL, ''),
('费用导出', @expense_menu_id, 5, '', '', 1, 0, 'F', '0', '0', 'finance:expense:export', '#', 'admin', NOW(), '', NULL, '');

-- 借支记录菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('借支记录', @parent_menu_id, 5, 'advance', 'finance/advance/index', 1, 0, 'C', '0', '0', 'finance:advance:list', 'money', 'admin', NOW(), '', NULL, '借支记录菜单');

SET @advance_menu_id = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES 
('借支查询', @advance_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'finance:advance:query', '#', 'admin', NOW(), '', NULL, ''),
('借支新增', @advance_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'finance:advance:add', '#', 'admin', NOW(), '', NULL, ''),
('借支修改', @advance_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'finance:advance:edit', '#', 'admin', NOW(), '', NULL, ''),
('借支删除', @advance_menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'finance:advance:remove', '#', 'admin', NOW(), '', NULL, ''),
('借支导出', @advance_menu_id, 5, '', '', 1, 0, 'F', '0', '0', 'finance:advance:export', '#', 'admin', NOW(), '', NULL, '');

-- 成本核算菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('成本核算', @parent_menu_id, 6, 'costAccounting', 'finance/costAccounting/index', 1, 0, 'C', '0', '0', 'finance:costAccounting:list', 'chart', 'admin', NOW(), '', NULL, '成本核算菜单');

SET @cost_accounting_menu_id = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES 
('成本查询', @cost_accounting_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'finance:costAccounting:query', '#', 'admin', NOW(), '', NULL, ''),
('成本新增', @cost_accounting_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'finance:costAccounting:add', '#', 'admin', NOW(), '', NULL, ''),
('成本修改', @cost_accounting_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'finance:costAccounting:edit', '#', 'admin', NOW(), '', NULL, ''),
('成本删除', @cost_accounting_menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'finance:costAccounting:remove', '#', 'admin', NOW(), '', NULL, ''),
('成本导出', @cost_accounting_menu_id, 5, '', '', 1, 0, 'F', '0', '0', 'finance:costAccounting:export', '#', 'admin', NOW(), '', NULL, '');

-- 投资人返款菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('投资人返款', @parent_menu_id, 7, 'investorPayment', 'finance/investorPayment/index', 1, 0, 'C', '0', '0', 'finance:investorPayment:list', 'peoples', 'admin', NOW(), '', NULL, '投资人返款菜单');

SET @investor_payment_menu_id = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) 
VALUES 
('返款查询', @investor_payment_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'finance:investorPayment:query', '#', 'admin', NOW(), '', NULL, ''),
('返款新增', @investor_payment_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'finance:investorPayment:add', '#', 'admin', NOW(), '', NULL, ''),
('返款修改', @investor_payment_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'finance:investorPayment:edit', '#', 'admin', NOW(), '', NULL, ''),
('返款删除', @investor_payment_menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'finance:investorPayment:remove', '#', 'admin', NOW(), '', NULL, ''),
('返款导出', @investor_payment_menu_id, 5, '', '', 1, 0, 'F', '0', '0', 'finance:investorPayment:export', '#', 'admin', NOW(), '', NULL, '');

-- 给超级管理员分配菜单权限
INSERT INTO sys_role_menu (role_id, menu_id) 
SELECT 1, menu_id FROM sys_menu WHERE perms LIKE 'finance:expense:%' OR perms LIKE 'finance:advance:%' OR perms LIKE 'finance:costAccounting:%' OR perms LIKE 'finance:investorPayment:%';
