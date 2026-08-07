SET NAMES utf8mb4;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '商品同步',
       (SELECT menu_id FROM sys_menu WHERE perms = 'finance:product:list' AND menu_type = 'C' LIMIT 1),
       20, '', '', 1, 0, 'F', '0', '0', 'finance:product:sync', '#', 'admin', NOW(), '将商品配置同步到有权限的其他机构'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:product:sync' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '供应商同步',
       (SELECT menu_id FROM sys_menu WHERE perms = 'finance:supplier:list' AND menu_type = 'C' LIMIT 1),
       20, '', '', 1, 0, 'F', '0', '0', 'finance:supplier:sync', '#', 'admin', NOW(), '将供应商配置同步到有权限的其他机构'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:supplier:sync' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '会员等级同步',
       (SELECT menu_id FROM sys_menu WHERE perms = 'member:level:list' AND menu_type = 'C' LIMIT 1),
       20, '', '', 1, 0, 'F', '0', '0', 'member:level:sync', '#', 'admin', NOW(), '将会员等级配置同步到有权限的其他机构'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:level:sync' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '会员政策同步',
       (SELECT menu_id FROM sys_menu WHERE perms = 'member:purchase:list' AND menu_type = 'C' LIMIT 1),
       20, '', '', 1, 0, 'F', '0', '0', 'member:campaignPolicy:sync', '#', 'admin', NOW(), '将会员商品销售政策同步到有权限的其他机构'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:campaignPolicy:sync' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '配置同步查询',
       (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'member' AND menu_type = 'M' LIMIT 1),
       90, '', '', 1, 0, 'F', '0', '0', 'member:configSync:query', '#', 'admin', NOW(), '查询跨机构配置同步批次和结果'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:configSync:query' AND menu_type = 'F');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('finance:product:sync', 'finance:supplier:sync', 'member:level:sync',
                'member:campaignPolicy:sync', 'member:configSync:query')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id);
