-- 注：本项目 sys_menu 未使用 del_flag 软删，直接依赖 parent_id/role_menu 关联
SET NAMES utf8mb4;
SELECT '== admin 可见菜单总数 (含按钮) ==' AS step;
SELECT
    COUNT(*) AS total,
    SUM(CASE WHEN m.menu_type='M' THEN 1 ELSE 0 END) AS type_dir,
    SUM(CASE WHEN m.menu_type='C' THEN 1 ELSE 0 END) AS type_menu,
    SUM(CASE WHEN m.menu_type='F' THEN 1 ELSE 0 END) AS type_button,
    SUM(CASE WHEN m.visible='1' THEN 1 ELSE 0 END) AS visible_1,
    SUM(CASE WHEN m.status='0' THEN 1 ELSE 0 END) AS status_ok
FROM sys_menu m
WHERE m.menu_id IN (
    SELECT rm.menu_id FROM sys_role_menu rm
      LEFT JOIN sys_role r ON r.role_id=rm.role_id
     WHERE r.role_id=1
);

SELECT '== (A) admin 可见且非按钮菜单中 path 为空/空白字符 ==' AS step;
SELECT m.menu_id,m.parent_id,m.menu_name,m.menu_type,m.path,m.component,m.visible,m.status,m.order_num,
       m.perms
  FROM sys_menu m
 WHERE m.menu_type IN ('M','C')
   AND (TRIM(IFNULL(m.path,''))='' OR m.path IS NULL)
   AND m.menu_id IN (SELECT rm.menu_id FROM sys_role_menu rm WHERE rm.role_id=1);

SELECT '== (B) admin 可见非按钮菜单中 path 含重复斜杠 // 或头尾空白 ==' AS step;
SELECT m.menu_id,m.parent_id,m.menu_name,m.menu_type,HEX(m.path) AS path_hex,m.path,m.component
  FROM sys_menu m
 WHERE m.menu_type IN ('M','C')
   AND m.menu_id IN (SELECT rm.menu_id FROM sys_role_menu rm WHERE rm.role_id=1)
   AND (m.path REGEXP '//' OR m.path!=TRIM(m.path));

SELECT '== (C) 根级菜单(parent_id=0)中 TYPE=M(目录)但 path 以 / 开头 或 path="", 将被 getRouterPath 组装为 "//xxx" 的风险 ==' AS step;
SELECT m.menu_id,m.menu_name,m.menu_type,m.is_frame,m.path,m.component
  FROM sys_menu m
 WHERE m.parent_id=0 AND m.menu_type='M' AND m.is_frame=0
   AND m.menu_id IN (SELECT rm.menu_id FROM sys_role_menu rm WHERE rm.role_id=1)
   AND (m.path LIKE '/%' OR TRIM(IFNULL(m.path,''))='');

SELECT '== (D) admin 可见 TYPE_MENU(C) 非一级菜单(component != Layout/ParentView/InnerLink) 且 component 为空 ==' AS step;
SELECT m.menu_id,m.parent_id,m.menu_name,m.menu_type,m.is_frame,m.path,m.component
  FROM sys_menu m
 WHERE m.menu_type='C' AND m.parent_id<>0
   AND m.menu_id IN (SELECT rm.menu_id FROM sys_role_menu rm WHERE rm.role_id=1)
   AND (m.component IS NULL OR TRIM(m.component)='');

SELECT '== (E) 根级菜单中重复的 path（同 parent_id=0 下 path 冲突）== ' AS step;
SELECT parent_id,path,COUNT(*) AS cnt,GROUP_CONCAT(menu_id ORDER BY menu_id) AS menu_ids,
       GROUP_CONCAT(CONCAT(menu_name,' [',menu_type,']') ORDER BY menu_id SEPARATOR ' | ') AS names
  FROM sys_menu m
 WHERE m.parent_id=0
   AND m.menu_id IN (SELECT rm.menu_id FROM sys_role_menu rm WHERE rm.role_id=1)
 GROUP BY parent_id,path
HAVING COUNT(*)>1;

SELECT '== (F) 父菜单下兄弟菜单的 path 重复（嵌套路由中最容易 addRoute 冲突）==' AS step;
SELECT m.parent_id,m.path,COUNT(*) AS cnt,
       GROUP_CONCAT(m.menu_id ORDER BY m.menu_id) AS menu_ids,
       GROUP_CONCAT(CONCAT(m.menu_name,' [',m.menu_type,']') ORDER BY m.menu_id SEPARATOR ' | ') AS names
  FROM sys_menu m
 WHERE m.menu_type IN ('M','C') AND m.parent_id IS NOT NULL AND m.parent_id<>0
   AND m.menu_id IN (SELECT rm.menu_id FROM sys_role_menu rm WHERE rm.role_id=1)
 GROUP BY m.parent_id,m.path
HAVING COUNT(*)>1
 ORDER BY m.parent_id,cnt DESC;

SELECT '== (G) admin 可见 TYPE=M(目录) 但没有任何子菜单(将 alwaysShow=true 无 children → 侧边栏空菜单) ==' AS step;
SELECT m.menu_id,m.parent_id,m.menu_name,m.path,m.component,m.visible,m.status
  FROM sys_menu m
 WHERE m.menu_type='M'
   AND m.menu_id IN (SELECT rm.menu_id FROM sys_role_menu rm WHERE rm.role_id=1)
   AND NOT EXISTS (SELECT 1 FROM sys_menu c WHERE c.parent_id=m.menu_id);
