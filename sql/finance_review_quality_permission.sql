-- R10-C: 复盘质量看板权限
-- 只授权 role_id=1
-- R10-FIX-C: 父菜单应挂载在财务一级 M 菜单下，而非另一个 C 页面菜单下，
-- 否则动态路由会出现层级异常（页面挂页面）。与 finance_review_knowledge_permission.sql 对齐。

SET @finance_root := (SELECT menu_id FROM sys_menu WHERE path = 'finance' AND menu_type = 'M' LIMIT 1);

INSERT INTO sys_menu (
  menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
  menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT '复盘质量看板', COALESCE(@finance_root, 108), 90, 'review-quality', 'finance/reviewQuality/index', '', '',
  1, 0, 'C', '0', '0', 'finance:reviewQuality:view', 'chart', 'admin', NOW(), '复盘质量看板'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:reviewQuality:view');

-- R10-FIX-C2: 幂等修正已存在菜单的父级/path/component。
-- 旧 SQL 曾把父级挂到 finance:dashboard:view 的 C 菜单下，仅靠 INSERT WHERE NOT EXISTS
-- 无法修正已创建的错误记录，重跑此 UPDATE 确保父级回到财务根 M 菜单。
UPDATE sys_menu
SET parent_id = COALESCE(@finance_root, 108),
    path = 'review-quality',
    component = 'finance/reviewQuality/index',
    update_time = NOW()
WHERE perms = 'finance:reviewQuality:view'
  AND menu_type = 'C';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms = 'finance:reviewQuality:view'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );
