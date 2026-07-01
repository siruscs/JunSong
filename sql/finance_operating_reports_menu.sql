-- finance_operating_reports_menu.sql
-- 幂等插入经营利润钻取、费用异常分析、销售经营分析、分润结算菜单

-- 利润钻取
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2410, '利润钻取', 2000, 1, 'profitDrilldown', 'finance/report/profit', 1, 0, 'C', '0', '0', 'finance:report:profit', 'money', 'admin', sysdate(), '', NULL, '经营利润钻取分析'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2410);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2410 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2410);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 100, 2410 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 100 AND menu_id = 2410);

-- 费用异常分析
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2411, '费用异常分析', 2000, 2, 'expenseAnomaly', 'finance/report/expense', 1, 0, 'C', '0', '0', 'finance:report:expense', 'bug', 'admin', sysdate(), '', NULL, '费用异常分析'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2411);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2411 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2411);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 100, 2411 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 100 AND menu_id = 2411);

-- 销售经营分析
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2412, '销售经营分析', 2000, 3, 'salesOperation', 'finance/report/sale', 1, 0, 'C', '0', '0', 'finance:report:sale', 'shopping', 'admin', sysdate(), '', NULL, '销售经营分析'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2412);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2412 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2412);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 100, 2412 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 100 AND menu_id = 2412);

-- 分润结算看板
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2413, '分润结算看板', 2000, 4, 'profitShareSettlement', 'finance/report/profitShare', 1, 0, 'C', '0', '0', 'finance:report:profitShare', 'peoples', 'admin', sysdate(), '', NULL, '分润结算看板'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2413);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2413 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2413);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 100, 2413 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 100 AND menu_id = 2413);

-- 回滚 SQL:
-- DELETE FROM sys_role_menu WHERE menu_id IN (2410, 2411, 2412, 2413);
-- DELETE FROM sys_menu WHERE menu_id IN (2410, 2411, 2412, 2413);
