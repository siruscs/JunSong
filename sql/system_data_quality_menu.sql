-- R20: 数据质量看板菜单与权限
-- 幂等插入，仅授权 role_id = 1
-- P2-fix: 动态计算 menu_id，避免与已有菜单主键冲突

-- 动态查找系统管理父菜单
SET @systemParentId := (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0 LIMIT 1);

-- 动态分配 menu_id：取当前最大值 + 100，保底 2800
SET @menuId := GREATEST(2800, (SELECT IFNULL(MAX(menu_id) + 100, 2800) FROM sys_menu));

-- 插入数据质量看板页面菜单 (C)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT @menuId, '数据质量', COALESCE(@systemParentId, 0), 90, 'dataQuality', 'system/dataQuality/index', '', 'DataQuality', 1, 0, 'C', '0', '0', 'system:data-quality:view', 'monitor', 'admin', NOW(), '数据质量看板'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:data-quality:view');

-- 插入查询按钮权限 (F)：按 perms 查找父菜单 ID，不硬编码
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据质量查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'system:data-quality:view' LIMIT 1),
       1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:data-quality:query', '#', 'admin', NOW(), '数据质量查询按钮'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:data-quality:query');

-- 仅授权 role_id = 1
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.perms IN ('system:data-quality:view', 'system:data-quality:query')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
