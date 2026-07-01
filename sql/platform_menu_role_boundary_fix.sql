-- 平台级菜单权限边界修复。
-- 规则：租户管理、开放平台及其子菜单只保留给平台管理员 role_id = 1。

DROP TEMPORARY TABLE IF EXISTS tmp_platform_menu_ids;

CREATE TEMPORARY TABLE tmp_platform_menu_ids AS
WITH RECURSIVE protected_menu AS (
  SELECT menu_id, menu_name, parent_id
  FROM sys_menu
  WHERE menu_name IN ('租户管理', '开放平台')

  UNION ALL

  SELECT child.menu_id, child.menu_name, child.parent_id
  FROM sys_menu child
  JOIN protected_menu parent ON child.parent_id = parent.menu_id
)
SELECT menu_id
FROM protected_menu;

DELETE role_menu
FROM sys_role_menu role_menu
JOIN tmp_platform_menu_ids protected ON protected.menu_id = role_menu.menu_id
WHERE role_menu.role_id <> 1;

DROP TEMPORARY TABLE IF EXISTS tmp_platform_menu_ids;
