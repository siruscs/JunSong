SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 先删除可能存在的财务模块菜单
DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT menu_id FROM sys_menu WHERE menu_id BETWEEN 2000 AND 2099
);

DELETE FROM sys_menu WHERE menu_id BETWEEN 2000 AND 2099;

-- 财务模块菜单
-- 一级菜单
INSERT INTO sys_menu VALUES('2000', '财务管理', '0', '4', 'finance', NULL, '1', '0', '1', '0', 'M', '0', '0', '', 'money', 'admin', NOW(), '', NULL, '财务管理目录');

-- 二级菜单 - 商品管理
INSERT INTO sys_menu VALUES('2001', '商品管理', '2000', '1', 'product', 'finance/product/index', '1', '0', '1', '0', 'C', '0', '0', 'finance:product:list', 'shopping', 'admin', NOW(), '', NULL, '商品管理菜单');
INSERT INTO sys_menu VALUES('2002', '商品查询', '2001', '1', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:product:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2003', '商品新增', '2001', '2', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:product:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2004', '商品修改', '2001', '3', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:product:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2005', '商品删除', '2001', '4', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:product:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2006', '商品导出', '2001', '5', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:product:export', '#', 'admin', NOW(), '', NULL, '');

-- 二级菜单 - 供应商管理
INSERT INTO sys_menu VALUES('2010', '供应商管理', '2000', '2', 'supplier', 'finance/supplier/index', '1', '0', '1', '0', 'C', '0', '0', 'finance:supplier:list', 'peoples', 'admin', NOW(), '', NULL, '供应商管理菜单');
INSERT INTO sys_menu VALUES('2011', '供应商查询', '2010', '1', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:supplier:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2012', '供应商新增', '2010', '2', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:supplier:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2013', '供应商修改', '2010', '3', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:supplier:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2014', '供应商删除', '2010', '4', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:supplier:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2015', '供应商导出', '2010', '5', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:supplier:export', '#', 'admin', NOW(), '', NULL, '');

-- 二级菜单 - 进货管理
INSERT INTO sys_menu VALUES('2020', '进货管理', '2000', '3', 'purchase', 'finance/purchase/index', '1', '0', '1', '0', 'C', '0', '0', 'finance:purchase:list', 'shopping', 'admin', NOW(), '', NULL, '进货管理菜单');
INSERT INTO sys_menu VALUES('2021', '进货查询', '2020', '1', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:purchase:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2022', '进货新增', '2020', '2', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:purchase:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2023', '进货修改', '2020', '3', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:purchase:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2024', '进货删除', '2020', '4', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:purchase:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2025', '进货导出', '2020', '5', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:purchase:export', '#', 'admin', NOW(), '', NULL, '');

-- 二级菜单 - 销售记录
INSERT INTO sys_menu VALUES('2030', '销售记录', '2000', '4', 'sale', 'finance/sale/index', '1', '0', '1', '0', 'C', '0', '0', 'finance:sale:list', 'date', 'admin', NOW(), '', NULL, '销售记录菜单');
INSERT INTO sys_menu VALUES('2031', '销售查询', '2030', '1', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:sale:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2032', '销售新增', '2030', '2', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:sale:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2033', '销售修改', '2030', '3', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:sale:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2034', '销售删除', '2030', '4', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:sale:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2035', '缴款修改', '2030', '5', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:sale:payment', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2036', '销售导出', '2030', '6', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:sale:export', '#', 'admin', NOW(), '', NULL, '');

-- 二级菜单 - 费用记录
INSERT INTO sys_menu VALUES('2040', '费用记录', '2000', '5', 'expense', 'finance/expense/index', '1', '0', '1', '0', 'C', '0', '0', 'finance:expense:list', 'shopping', 'admin', NOW(), '', NULL, '费用记录菜单');
INSERT INTO sys_menu VALUES('2041', '费用查询', '2040', '1', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:expense:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2042', '费用新增', '2040', '2', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:expense:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2043', '费用修改', '2040', '3', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:expense:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2044', '费用删除', '2040', '4', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:expense:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2045', '费用核销', '2040', '5', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:expense:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2046', '费用导出', '2040', '6', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:expense:export', '#', 'admin', NOW(), '', NULL, '');

-- 二级菜单 - 借支记录
INSERT INTO sys_menu VALUES('2050', '借支记录', '2000', '6', 'advance', 'finance/advance/index', '1', '0', '1', '0', 'C', '0', '0', 'finance:advance:list', 'money', 'admin', NOW(), '', NULL, '借支记录菜单');
INSERT INTO sys_menu VALUES('2051', '借支查询', '2050', '1', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:advance:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2052', '借支新增', '2050', '2', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:advance:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2053', '借支修改', '2050', '3', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:advance:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2054', '借支删除', '2050', '4', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:advance:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2055', '借支核销', '2050', '5', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:advance:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2056', '借支导出', '2050', '6', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:advance:export', '#', 'admin', NOW(), '', NULL, '');

-- 二级菜单 - 成本核算
INSERT INTO sys_menu VALUES('2060', '成本核算', '2000', '7', 'costAccounting', 'finance/costAccounting/index', '1', '0', '1', '0', 'C', '0', '0', 'finance:costAccounting:list', 'chart', 'admin', NOW(), '', NULL, '成本核算菜单');
INSERT INTO sys_menu VALUES('2061', '成本查询', '2060', '1', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:costAccounting:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2062', '成本新增', '2060', '2', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:costAccounting:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2063', '成本修改', '2060', '3', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:costAccounting:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2064', '成本删除', '2060', '4', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:costAccounting:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2065', '成本导出', '2060', '5', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:costAccounting:export', '#', 'admin', NOW(), '', NULL, '');

-- 二级菜单 - 投资人返款
INSERT INTO sys_menu VALUES('2070', '投资人返款', '2000', '8', 'investorPayment', 'finance/investorPayment/index', '1', '0', '1', '0', 'C', '0', '0', 'finance:investorPayment:list', 'peoples', 'admin', NOW(), '', NULL, '投资人返款菜单');
INSERT INTO sys_menu VALUES('2071', '返款查询', '2070', '1', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:investorPayment:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2072', '返款新增', '2070', '2', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:investorPayment:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2073', '返款修改', '2070', '3', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:investorPayment:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2074', '返款删除', '2070', '4', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:investorPayment:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO sys_menu VALUES('2075', '返款导出', '2070', '5', '', '', '1', '0', '1', '0', 'F', '0', '0', 'finance:investorPayment:export', '#', 'admin', NOW(), '', NULL, '');

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
INSERT INTO sys_role_menu VALUES('1', '2030');
INSERT INTO sys_role_menu VALUES('1', '2031');
INSERT INTO sys_role_menu VALUES('1', '2032');
INSERT INTO sys_role_menu VALUES('1', '2033');
INSERT INTO sys_role_menu VALUES('1', '2034');
INSERT INTO sys_role_menu VALUES('1', '2035');
INSERT INTO sys_role_menu VALUES('1', '2036');
INSERT INTO sys_role_menu VALUES('1', '2040');
INSERT INTO sys_role_menu VALUES('1', '2041');
INSERT INTO sys_role_menu VALUES('1', '2042');
INSERT INTO sys_role_menu VALUES('1', '2043');
INSERT INTO sys_role_menu VALUES('1', '2044');
INSERT INTO sys_role_menu VALUES('1', '2045');
INSERT INTO sys_role_menu VALUES('1', '2046');
INSERT INTO sys_role_menu VALUES('1', '2050');
INSERT INTO sys_role_menu VALUES('1', '2051');
INSERT INTO sys_role_menu VALUES('1', '2052');
INSERT INTO sys_role_menu VALUES('1', '2053');
INSERT INTO sys_role_menu VALUES('1', '2054');
INSERT INTO sys_role_menu VALUES('1', '2055');
INSERT INTO sys_role_menu VALUES('1', '2056');
INSERT INTO sys_role_menu VALUES('1', '2060');
INSERT INTO sys_role_menu VALUES('1', '2061');
INSERT INTO sys_role_menu VALUES('1', '2062');
INSERT INTO sys_role_menu VALUES('1', '2063');
INSERT INTO sys_role_menu VALUES('1', '2064');
INSERT INTO sys_role_menu VALUES('1', '2065');
INSERT INTO sys_role_menu VALUES('1', '2070');
INSERT INTO sys_role_menu VALUES('1', '2071');
INSERT INTO sys_role_menu VALUES('1', '2072');
INSERT INTO sys_role_menu VALUES('1', '2073');
INSERT INTO sys_role_menu VALUES('1', '2074');
INSERT INTO sys_role_menu VALUES('1', '2075');
