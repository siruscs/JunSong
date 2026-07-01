-- 后台管理三大模块概览入口
-- 可重复执行：已存在的菜单和角色授权不会重复插入。

UPDATE sys_menu
SET menu_name = '会员管理',
    remark = '会员管理目录'
WHERE parent_id = 0
  AND path = 'member'
  AND menu_type = 'M';

SET @systemRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'system' AND menu_type = 'M' LIMIT 1);
SET @financeRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'finance' AND menu_type = 'M' LIMIT 1);
SET @memberRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'member' AND menu_type = 'M' LIMIT 1);

SET @nextMenuId := (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  @nextMenuId, '系统概览', @systemRootId, 0, 'overview', 'system/overview/index', '', '',
  1, 0, 'C', '0', '0', 'system:overview:list', 'dashboard',
  'admin', NOW(), '', NULL, '系统管理概览入口'
WHERE @systemRootId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @systemRootId AND path = 'overview' AND component = 'system/overview/index'
  );

SET @systemOverviewId := (
  SELECT menu_id FROM sys_menu
  WHERE parent_id = @systemRootId AND path = 'overview' AND component = 'system/overview/index'
  LIMIT 1
);

SET @nextMenuId := (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  @nextMenuId, '财务概览', @financeRootId, 0, 'overview', 'finance/overview/index', '', '',
  1, 0, 'C', '0', '0', 'finance:overview:list', 'chart',
  'admin', NOW(), '', NULL, '财务管理概览入口'
WHERE @financeRootId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @financeRootId AND path = 'overview' AND component = 'finance/overview/index'
  );

SET @financeOverviewId := (
  SELECT menu_id FROM sys_menu
  WHERE parent_id = @financeRootId AND path = 'overview' AND component = 'finance/overview/index'
  LIMIT 1
);

SET @nextMenuId := (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  @nextMenuId, '会员概览', @memberRootId, 0, 'overview', 'member/overview/index', '', '',
  1, 0, 'C', '0', '0', 'member:overview:list', 'people',
  'admin', NOW(), '', NULL, '会员管理概览入口'
WHERE @memberRootId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @memberRootId AND path = 'overview' AND component = 'member/overview/index'
  );

SET @memberOverviewId := (
  SELECT menu_id FROM sys_menu
  WHERE parent_id = @memberRootId AND path = 'overview' AND component = 'member/overview/index'
  LIMIT 1
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, @systemOverviewId
WHERE @systemOverviewId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = @systemOverviewId);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, @financeOverviewId
WHERE @financeOverviewId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = @financeOverviewId);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, @memberOverviewId
WHERE @memberOverviewId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = @memberOverviewId);
