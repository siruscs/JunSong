-- R8-A: 每日经营复盘权限（幂等）
SET @parent_id := (SELECT menu_id FROM sys_menu WHERE perms = 'finance:dashboard:operation' LIMIT 1);
SET @menu_id := 2450;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
  menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT
  @menu_id, '每日经营复盘', COALESCE(@parent_id, 0), 80, 'daily-review', '', '', '',
  1, 0, 'F', '0', '0', 'finance:dailyReview:view', '#', 'admin', NOW(), '每日经营复盘权限'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:dailyReview:view');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms = 'finance:dailyReview:view'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );
