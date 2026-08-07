SET NAMES utf8mb4;

SET @member_root_id := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'member' AND menu_type = 'M' LIMIT 1);
SET @next_menu_id := (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT @next_menu_id, '会员商品销售政策', @member_root_id, 7, 'campaignPolicy', 'member/campaignPolicy/index',
       1, 0, 'C', '0', '0', 'member:campaignPolicy:list', 'discount', 'admin', NOW(), '', NULL,
       '维护核算周期内商品会员购买政策和赠送套餐档位'
FROM DUAL
WHERE @member_root_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:campaignPolicy:list' AND menu_type = 'C');

SET @policy_menu_id := (SELECT menu_id FROM sys_menu WHERE perms = 'member:campaignPolicy:list' AND menu_type = 'C' LIMIT 1);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '查询会员商品政策', @policy_menu_id, 1, '', '', 1, 0, 'F', '0', '0',
       'member:campaignPolicy:query', '#', 'admin', NOW(), '', NULL, '查询会员商品销售政策'
FROM DUAL WHERE @policy_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:campaignPolicy:query' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '新增会员商品政策', @policy_menu_id, 2, '', '', 1, 0, 'F', '0', '0',
       'member:campaignPolicy:add', '#', 'admin', NOW(), '', NULL, '新增会员商品销售政策和套餐'
FROM DUAL WHERE @policy_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:campaignPolicy:add' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '维护会员商品政策状态', @policy_menu_id, 3, '', '', 1, 0, 'F', '0', '0',
       'member:campaignPolicy:edit', '#', 'admin', NOW(), '', NULL, '启用或停用会员商品销售政策'
FROM DUAL WHERE @policy_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:campaignPolicy:edit' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '删除会员商品政策', @policy_menu_id, 4, '', '', 1, 0, 'F', '0', '0',
       'member:campaignPolicy:remove', '#', 'admin', NOW(), '', NULL, '删除会员商品销售政策及其套餐档位'
FROM DUAL WHERE @policy_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:campaignPolicy:remove' AND menu_type = 'F');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('member:campaignPolicy:list', 'member:campaignPolicy:query', 'member:campaignPolicy:add', 'member:campaignPolicy:edit', 'member:campaignPolicy:remove')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id);

SELECT 'member_campaign_policy_menu' AS check_name,
       COUNT(*) AS menu_count
FROM sys_menu WHERE perms LIKE 'member:campaignPolicy:%';
