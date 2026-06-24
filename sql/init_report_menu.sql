-- 报表功能菜单配置（完整版）
-- 执行日期: 2026-06-07

SET NAMES utf8mb4;

-- 删除之前可能存在的报表菜单
DELETE FROM sys_role_menu WHERE menu_id >= 2150 AND menu_id <= 2160;
DELETE FROM sys_menu WHERE menu_id >= 2150 AND menu_id <= 2160;

-- ==========================================
-- 财务报表菜单（在财务管理 2000 下）
-- ==========================================

-- 费用分析报表
INSERT INTO sys_menu VALUES('2150', '费用分析报表', '2000', '10', 'expenseReport', 'finance/report/expense', '', '', 1, 0, 'C', '0', '0', 'finance:report:expense', 'chart', 'admin', NOW(), '', NULL, '费用分析报表菜单');

-- 成本分析报表
INSERT INTO sys_menu VALUES('2151', '成本分析报表', '2000', '11', 'costReport', 'finance/report/cost', '', '', 1, 0, 'C', '0', '0', 'finance:report:cost', 'chart', 'admin', NOW(), '', NULL, '成本分析报表菜单');

-- 分润报表
INSERT INTO sys_menu VALUES('2152', '分润报表', '2000', '12', 'profitShareReport', 'finance/report/profitShare', '', '', 1, 0, 'C', '0', '0', 'finance:report:profitShare', 'money', 'admin', NOW(), '', NULL, '分润报表菜单');

-- 销售报表
INSERT INTO sys_menu VALUES('2153', '销售报表', '2000', '13', 'saleReport', 'finance/report/sale', '', '', 1, 0, 'C', '0', '0', 'finance:report:sale', 'shopping', 'admin', NOW(), '', NULL, '销售报表菜单');

-- 利润报表
INSERT INTO sys_menu VALUES('2154', '利润报表', '2000', '14', 'profitReport', 'finance/report/profit', '', '', 1, 0, 'C', '0', '0', 'finance:report:profit', 'money', 'admin', NOW(), '', NULL, '利润报表菜单');

-- 库存报表
INSERT INTO sys_menu VALUES('2155', '库存报表', '2000', '15', 'stockReport', 'finance/report/stock', '', '', 1, 0, 'C', '0', '0', 'finance:report:stock', 'goods', 'admin', NOW(), '', NULL, '库存报表菜单');

-- ==========================================
-- 会员报表菜单（在会员服务下）
-- ==========================================

-- 查询会员服务菜单ID
SELECT @memberMenuId := menu_id FROM sys_menu WHERE path = 'member' AND parent_id = 0 LIMIT 1;

-- 秒杀团购活动汇总
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('秒杀团购活动汇总', @memberMenuId, 8, 'seckillReport', 'member/report/seckill', '', '', 1, 0, 'C', '0', '0', 'member:report:seckill', 'list', 'admin', NOW(), '', NULL, '秒杀团购活动汇总报表菜单');

-- 会员分析报表
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('会员分析报表', @memberMenuId, 9, 'memberReport', 'member/report/member', '', '', 1, 0, 'C', '0', '0', 'member:report:member', 'chart', 'admin', NOW(), '', NULL, '会员分析报表菜单');

-- ==========================================
-- 将报表菜单权限赋予超级管理员角色（role_id = 1）
-- ==========================================

-- 财务报表权限
INSERT INTO sys_role_menu VALUES ('1', '2150'), ('1', '2151'), ('1', '2152'), ('1', '2153'), ('1', '2154'), ('1', '2155');

-- 会员报表权限
SELECT @memberReport1 := menu_id FROM sys_menu WHERE path = 'seckillReport' LIMIT 1;
SELECT @memberReport2 := menu_id FROM sys_menu WHERE path = 'memberReport' LIMIT 1;
INSERT INTO sys_role_menu VALUES ('1', @memberReport1), ('1', @memberReport2);