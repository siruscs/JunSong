-- 销售记录模块菜单
-- 二级菜单 - 销售记录
INSERT INTO sys_menu VALUES('2030', '销售记录', '2000', '4', 'sale', 'finance/sale/index', '', '', 1, 0, 'C', '0', '0', 'finance:sale:list', 'date', 'admin', NOW(), '', NULL, '销售记录菜单');
INSERT INTO sys_menu VALUES('2031', '销售查询', '2030', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:sale:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2032', '销售新增', '2030', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:sale:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2033', '销售修改', '2030', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:sale:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2034', '销售删除', '2030', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:sale:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2035', '缴款修改', '2030', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:sale:payment', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2036', '销售导出', '2030', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:sale:export', '#', 'admin', NOW(), '', NULL, '');

-- 将销售记录菜单权限赋予超级管理员角色
INSERT INTO sys_role_menu VALUES('1', '2030');
INSERT INTO sys_role_menu VALUES('1', '2031');
INSERT INTO sys_role_menu VALUES('1', '2032');
INSERT INTO sys_role_menu VALUES('1', '2033');
INSERT INTO sys_role_menu VALUES('1', '2034');
INSERT INTO sys_role_menu VALUES('1', '2035');
INSERT INTO sys_role_menu VALUES('1', '2036');
