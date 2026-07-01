-- S1: 新租户初始化边界固化 — 幂等清理
-- 规则：租户管理、开放平台及其所有子菜单只保留给平台管理员 role_id = 1。
-- 使用递归 CTE 查找所有受保护菜单的后代，删除非 admin 角色的授权。
-- 此脚本可安全重复执行（幂等）。

DELETE role_menu
FROM sys_role_menu role_menu
INNER JOIN (
  WITH RECURSIVE protected_menu AS (
    SELECT menu_id
    FROM sys_menu
    WHERE menu_name IN ('租户管理', '开放平台')

    UNION ALL

    SELECT child.menu_id
    FROM sys_menu child
    INNER JOIN protected_menu parent ON child.parent_id = parent.menu_id
  )
  SELECT menu_id FROM protected_menu
) protected ON protected.menu_id = role_menu.menu_id
WHERE role_menu.role_id <> 1;
