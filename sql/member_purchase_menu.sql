-- 会员购买域 PC 菜单和按钮权限。
-- 幂等执行；不授予普通角色，角色授权由管理员按门店和岗位配置。
SET NAMES utf8mb4;

SET @member_root_id := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'member' AND menu_type = 'M' LIMIT 1);
SET @next_menu_id := (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT @next_menu_id, '会员购买记录', @member_root_id, 6, 'purchase', 'member/purchase/index',
       1, 0, 'C', '0', '0', 'member:purchase:list', 'shopping', 'admin', NOW(), '', NULL, '会员购买域明细，不纳入财务销售统计'
FROM DUAL
WHERE @member_root_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:purchase:list' AND menu_type = 'C');

SET @purchase_menu_id := (SELECT menu_id FROM sys_menu WHERE perms = 'member:purchase:list' AND menu_type = 'C' LIMIT 1);
SET @next_menu_id := (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT @next_menu_id, '查询会员购买记录', @purchase_menu_id, 1, '', '', 1, 0, 'F', '0', '0',
       'member:purchase:query', '#', 'admin', NOW(), '', NULL, '查询购买单详情'
FROM DUAL
WHERE @purchase_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:purchase:query' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '新建会员购买单', @purchase_menu_id, 2, '', '', 1, 0, 'F', '0', '0',
       'member:purchase:add', '#', 'admin', NOW(), '', NULL, '新建会员购买单'
FROM DUAL
WHERE @purchase_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:purchase:add' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '编辑会员购买单', @purchase_menu_id, 3, '', '', 1, 0, 'F', '0', '0',
       'member:purchase:edit', '#', 'admin', NOW(), '', NULL, '编辑购买单及其收款、领取记录'
FROM DUAL
WHERE @purchase_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:purchase:edit' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '会员购买收款', @purchase_menu_id, 4, '', '', 1, 0, 'F', '0', '0',
       'member:purchase:payment', '#', 'admin', NOW(), '', NULL, '登记购买域收款和欠款回收'
FROM DUAL
WHERE @purchase_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:purchase:payment' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '会员购买领取', @purchase_menu_id, 5, '', '', 1, 0, 'F', '0', '0',
       'member:purchase:delivery', '#', 'admin', NOW(), '', NULL, '登记购买单分批领取'
FROM DUAL
WHERE @purchase_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:purchase:delivery' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '作废会员购买单', @purchase_menu_id, 6, '', '', 1, 0, 'F', '0', '0',
       'member:purchase:cancel', '#', 'admin', NOW(), '', NULL, '未领取购买单作废并冲正会员奖励'
FROM DUAL
WHERE @purchase_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:purchase:cancel' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '散客购买单绑定会员', @purchase_menu_id, 7, '', '', 1, 0, 'F', '0', '0',
       'member:purchase:bind', '#', 'admin', NOW(), '', NULL, '将实名散客购买单绑定到当前机构有效会员'
FROM DUAL
WHERE @purchase_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:purchase:bind' AND menu_type = 'F');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu), '导出会员购买记录', @purchase_menu_id, 8, '', '', 1, 0, 'F', '0', '0',
       'member:purchase:export', '#', 'admin', NOW(), '', NULL, '导出当前查询条件下的会员购买记录 XLSX'
FROM DUAL
WHERE @purchase_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:purchase:export' AND menu_type = 'F');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE (perms = 'member:purchase:list' OR perms = 'member:purchase:query'
       OR perms = 'member:purchase:add' OR perms = 'member:purchase:edit' OR perms = 'member:purchase:payment'
       OR perms = 'member:purchase:delivery' OR perms = 'member:purchase:cancel'
       OR perms = 'member:purchase:bind' OR perms = 'member:purchase:export')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id);
