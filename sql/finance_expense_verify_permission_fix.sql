SET NAMES utf8mb4;

-- 将历史上误配置为费用编辑权限的“费用核销”按钮原位修正为独立核销权限。
-- 原位更新可保留现有 sys_role_menu 授权关系，避免 WJS 等账户丢失授权。
SET @expense_menu_id := (
  SELECT menu_id FROM sys_menu
  WHERE perms = 'finance:expense:list'
  ORDER BY menu_id LIMIT 1
);

UPDATE sys_menu
SET perms = 'finance:expense:verify',
    update_by = 'system',
    update_time = NOW(),
    remark = '费用单笔及批量核销权限'
WHERE parent_id = @expense_menu_id
  AND menu_name = '费用核销'
  AND perms <> 'finance:expense:verify';

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, remark)
SELECT '费用核销', @expense_menu_id, 10, '', '', '', '', 1, 0, 'F', '0', '0',
       'finance:expense:verify', '#', 'system', NOW(), '费用单笔及批量核销权限'
WHERE @expense_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE parent_id = @expense_menu_id AND perms = 'finance:expense:verify'
  );

SELECT 'finance_expense_verify_permission_fix' AS check_name,
       (SELECT COUNT(*) FROM sys_menu WHERE perms = 'finance:expense:verify') AS verify_menu_count,
       (SELECT COUNT(*) FROM sys_menu WHERE parent_id = @expense_menu_id AND menu_name = '费用核销'
          AND perms = 'finance:expense:verify') AS scoped_verify_menu_count;
