-- 财务模块菜单
-- 一级菜单
INSERT INTO sys_menu VALUES('2000', '财务管理', '0', '4', 'finance', NULL, '', '', 1, 0, 'M', '0', '0', '', 'money', 'admin', NOW(), '', NULL, '财务管理目录');

-- 二级菜单 - 商品管理
INSERT INTO sys_menu VALUES('2001', '商品管理', '2000', '1', 'product', 'finance/product/index', '', '', 1, 0, 'C', '0', '0', 'finance:product:list', 'shopping', 'admin', NOW(), '', NULL, '商品管理菜单');
INSERT INTO sys_menu VALUES('2002', '商品查询', '2001', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:product:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2003', '商品新增', '2001', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:product:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2004', '商品修改', '2001', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:product:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2005', '商品删除', '2001', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:product:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2006', '商品导出', '2001', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:product:export', '#', 'admin', NOW(), '', NULL, '');

-- 二级菜单 - 供应商管理
INSERT INTO sys_menu VALUES('2010', '供应商管理', '2000', '2', 'supplier', 'finance/supplier/index', '', '', 1, 0, 'C', '0', '0', 'finance:supplier:list', 'peoples', 'admin', NOW(), '', NULL, '供应商管理菜单');
INSERT INTO sys_menu VALUES('2011', '供应商查询', '2010', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:supplier:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2012', '供应商新增', '2010', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:supplier:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2013', '供应商修改', '2010', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:supplier:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2014', '供应商删除', '2010', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:supplier:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2015', '供应商导出', '2010', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:supplier:export', '#', 'admin', NOW(), '', NULL, '');

-- 二级菜单 - 进货管理
INSERT INTO sys_menu VALUES('2020', '进货管理', '2000', '3', 'purchase', 'finance/purchase/index', '', '', 1, 0, 'C', '0', '0', 'finance:purchase:list', 'shopping-cart', 'admin', NOW(), '', NULL, '进货管理菜单');
INSERT INTO sys_menu VALUES('2021', '进货查询', '2020', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:purchase:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2022', '进货新增', '2020', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:purchase:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2023', '进货修改', '2020', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:purchase:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2024', '进货删除', '2020', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:purchase:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2025', '进货导出', '2020', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'finance:purchase:export', '#', 'admin', NOW(), '', NULL, '');

-- 将财务菜单权限赋予超级管理员角色
INSERT INTO sys_role_menu VALUES('1', '2000');
INSERT INTO sys_role_menu VALUES('1', '2001');
INSERT INTO sys_role_menu VALUES('1', '2002');
INSERT INTO sys_role_menu VALUES('1', '2003');
INSERT INTO sys_role_menu VALUES('1', '2004');
INSERT INTO sys_role_menu VALUES('1', '2005');
INSERT INTO sys_role_menu VALUES('1', '2006');
INSERT INTO sys_role_menu VALUES('1', '2010');
INSERT INTO sys_role_menu VALUES('1', '2011');
INSERT INTO sys_role_menu VALUES('1', '2012');
INSERT INTO sys_role_menu VALUES('1', '2013');
INSERT INTO sys_role_menu VALUES('1', '2014');
INSERT INTO sys_role_menu VALUES('1', '2015');
INSERT INTO sys_role_menu VALUES('1', '2020');
INSERT INTO sys_role_menu VALUES('1', '2021');
INSERT INTO sys_role_menu VALUES('1', '2022');
INSERT INTO sys_role_menu VALUES('1', '2023');
INSERT INTO sys_role_menu VALUES('1', '2024');
INSERT INTO sys_role_menu VALUES('1', '2025');
