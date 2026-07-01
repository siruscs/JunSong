-- 修复系统监控日志菜单组件路径。
-- 可重复执行：仅在菜单存在时更新 component，不改变菜单授权。

UPDATE sys_menu
SET component = 'monitor/log/operlog/index',
    update_by = 'admin',
    update_time = NOW(),
    remark = '操作日志菜单'
WHERE menu_name = '操作日志'
  AND path = 'operlog'
  AND menu_type = 'C';

UPDATE sys_menu
SET component = 'monitor/log/logininfor/index',
    update_by = 'admin',
    update_time = NOW(),
    remark = '登录日志菜单'
WHERE menu_name = '登录日志'
  AND path = 'logininfor'
  AND menu_type = 'C';
