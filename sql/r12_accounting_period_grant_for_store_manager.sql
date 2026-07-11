-- R12: 经营总览"成本核算"快捷入口改指向"核算周期"
-- 店长主管(dzzg/DZZG)需分配核算周期菜单(2123)及周期查询(2124)才能访问页面
-- 幂等：已存在的授权不会重复插入

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_key IN ('dzzg', 'DZZG')
  AND r.status = '0'
  AND m.menu_id IN (2123, 2124)
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );

-- 验证
SELECT r.role_id, r.role_name, r.role_key, m.menu_id, m.menu_name
FROM sys_role_menu rm
JOIN sys_role r ON r.role_id = rm.role_id
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE r.role_key IN ('dzzg', 'DZZG') AND m.menu_id IN (2123, 2124)
ORDER BY r.role_id, m.menu_id;
