UPDATE sys_menu SET
  menu_name = '租户管理',
  remark = '租户主体管理'
WHERE perms = 'system:tenant:list' AND menu_type = 'C';

UPDATE sys_menu SET menu_name = '租户查询' WHERE perms = 'system:tenant:query';
UPDATE sys_menu SET menu_name = '租户新增' WHERE perms = 'system:tenant:add';
UPDATE sys_menu SET menu_name = '租户修改' WHERE perms = 'system:tenant:edit';
UPDATE sys_menu SET menu_name = '租户删除' WHERE perms = 'system:tenant:remove';