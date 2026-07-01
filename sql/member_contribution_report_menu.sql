-- member_contribution_report_menu.sql
-- 幂等插入会员经营贡献报表菜单

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2420, '会员经营贡献', 2076, 5, 'contribution', 'member/report/member', 1, 0, 'C', '0', '0', 'member:report:contribution', 'chart', 'admin', sysdate(), '', NULL, '会员经营贡献报表'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2420);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2420 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2420);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 100, 2420 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 100 AND menu_id = 2420);

-- 回滚 SQL:
-- DELETE FROM sys_role_menu WHERE menu_id = 2420;
-- DELETE FROM sys_menu WHERE menu_id = 2420;
