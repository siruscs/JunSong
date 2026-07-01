-- 单门店经营报表菜单与权限
-- 可重复执行：已存在的菜单和角色授权不会重复插入。
-- 路径：财务管理 / 单门店经营报表
-- 权限：finance:report:store

SET NAMES utf8mb4;

-- 查找财务管理根菜单
SET @financeRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'finance' AND menu_type = 'M' LIMIT 1);

-- 新增单门店经营报表菜单（C 类型，组件路径 finance/report/store）
SET @nextMenuId := (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  @nextMenuId, '单门店经营报表', @financeRootId, 20, 'storeReport', 'finance/report/store', '', '',
  1, 0, 'C', '0', '0', 'finance:report:store', 'chart',
  'admin', NOW(), '', NULL, '单门店经营报表菜单'
WHERE @financeRootId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @financeRootId AND path = 'storeReport' AND component = 'finance/report/store'
  );

-- 获取菜单 ID（无论是否本次新建）
SET @storeReportMenuId := (
  SELECT menu_id FROM sys_menu
  WHERE parent_id = @financeRootId AND path = 'storeReport' AND component = 'finance/report/store'
  LIMIT 1
);

-- 仅授权超级管理员角色 (role_id = 1)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, @storeReportMenuId
WHERE @storeReportMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = @storeReportMenuId);
