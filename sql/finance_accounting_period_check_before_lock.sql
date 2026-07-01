-- R3-E: 锁账前检查权限按钮
-- Add checkBeforeLock permission button to accounting period menu

SET @accountingPeriodMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'finance:accountingPeriod:list' AND menu_type = 'C' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '锁账检查', @accountingPeriodMenuId, 10, '', NULL, 1, 0, 'F', '0', '0', 'finance:accountingPeriod:checkBeforeLock', '#', 'admin', sysdate(), '锁账前检查权限'
FROM DUAL
WHERE @accountingPeriodMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:accountingPeriod:checkBeforeLock');

-- Grant to all active roles
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.status = '0'
  AND m.perms = 'finance:accountingPeriod:checkBeforeLock'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu e WHERE e.role_id = r.role_id AND e.menu_id = m.menu_id);
