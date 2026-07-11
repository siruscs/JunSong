-- R11-F: 复盘知识库菜单权限
-- 幂等：SELECT ... WHERE NOT EXISTS
-- 仅授权 role_id=1，V1 不含删除权限

SET @finance_root := (SELECT menu_id FROM sys_menu WHERE path = 'finance' AND menu_type = 'M' LIMIT 1);
SET @knowledge_menu_id := 2480;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
  menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT @knowledge_menu_id, '复盘知识库', COALESCE(@finance_root, 108), 95, 'review-knowledge', 'finance/reviewKnowledge/index', '', '',
  1, 0, 'C', '0', '0', 'finance:reviewKnowledge:list', 'education', 'admin', NOW(), '复盘知识库'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:reviewKnowledge:list');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识新增', @knowledge_menu_id, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:reviewKnowledge:add', '#', 'admin', NOW(), '复盘知识新增'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:reviewKnowledge:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识修改', @knowledge_menu_id, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:reviewKnowledge:edit', '#', 'admin', NOW(), '复盘知识修改'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:reviewKnowledge:edit');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('finance:reviewKnowledge:list', 'finance:reviewKnowledge:add', 'finance:reviewKnowledge:edit')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
