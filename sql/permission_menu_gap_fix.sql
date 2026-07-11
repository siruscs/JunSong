-- ============================================================
-- 权限菜单缺口补齐 V2（基于 2026-07-02-permission-menu-gap-inventory.md）
-- 幂等：所有 INSERT 使用 WHERE NOT EXISTS，DEV/PROD 均可安全执行
-- 父菜单全部通过权限码或 path+menu_type 动态查找，不硬编码 menu_id
-- 对已存在 C 菜单补幂等 UPDATE 修正父级/path/component
-- 敏感权限清理历史非 admin 授权
-- ============================================================

-- =========================
-- P0: 财务模块
-- =========================

-- 1. 费用导入按钮 → 挂到费用记录（perms=finance:expense:list）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '费用导入',
       (SELECT menu_id FROM sys_menu WHERE perms = 'finance:expense:list' AND menu_type = 'C' LIMIT 1),
       6, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:expense:import', '#', 'admin', NOW(), '费用导入'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:expense:import')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:expense:list' AND menu_type = 'C');

-- 2. 费用OCR识别按钮 → 挂到费用记录
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '费用OCR识别',
       (SELECT menu_id FROM sys_menu WHERE perms = 'finance:expense:list' AND menu_type = 'C' LIMIT 1),
       7, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:expense:ocr', '#', 'admin', NOW(), '费用OCR识别'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:expense:ocr')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:expense:list' AND menu_type = 'C');

-- 3. 销售缴款按钮 → 挂到销售记录（perms=finance:sale:list）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '销售缴款',
       (SELECT menu_id FROM sys_menu WHERE perms = 'finance:sale:list' AND menu_type = 'C' LIMIT 1),
       6, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:sale:payment', '#', 'admin', NOW(), '销售缴款'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:sale:payment')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:sale:list' AND menu_type = 'C');

-- 4. 日复盘看板按钮 → 挂到经营总览（perms=finance:dashboard:operation）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '日复盘看板',
       (SELECT menu_id FROM sys_menu WHERE perms = 'finance:dashboard:operation' AND menu_type = 'C' LIMIT 1),
       8, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:dailyReview:view', '#', 'admin', NOW(), '日复盘看板'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:dailyReview:view')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:dashboard:operation' AND menu_type = 'C');

-- =========================
-- P0: 系统模块 - 租户按钮
-- =========================

-- 租户管理已有 list 菜单，补 query/add/edit/remove 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '租户查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C' LIMIT 1),
       1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:tenant:query', '#', 'admin', NOW(), '租户查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:query')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '租户新增',
       (SELECT menu_id FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C' LIMIT 1),
       2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:tenant:add', '#', 'admin', NOW(), '租户新增'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:add')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '租户修改',
       (SELECT menu_id FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C' LIMIT 1),
       3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:tenant:edit', '#', 'admin', NOW(), '租户修改'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:edit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '租户删除',
       (SELECT menu_id FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C' LIMIT 1),
       4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:tenant:remove', '#', 'admin', NOW(), '租户删除'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:remove')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:tenant:list' AND menu_type = 'C');

-- =========================
-- P0: 系统模块 - Webhook 管理页面 + 按钮
-- 父菜单动态查找系统管理根菜单（path='system' AND menu_type='M' AND parent_id=0）
-- =========================

-- Webhook 管理 C 菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'Webhook管理',
       (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0 LIMIT 1),
       90, 'webhook', 'system/webhook/index', '', '', 1, 0, 'C', '0', '0', 'system:webhook:list', 'tool', 'admin', NOW(), 'Webhook订阅管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:list' AND menu_type = 'C')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0);

-- 幂等 UPDATE：修正已存在 Webhook C 菜单的父级/path/component
UPDATE sys_menu SET
  parent_id = (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0 LIMIT 1) t),
  path = 'webhook',
  component = 'system/webhook/index',
  menu_type = 'C',
  update_time = NOW()
WHERE perms = 'system:webhook:list' AND menu_type = 'C';

-- Webhook 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'Webhook查询', (SELECT menu_id FROM sys_menu WHERE perms = 'system:webhook:list' AND menu_type = 'C' LIMIT 1), 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:webhook:query', '#', 'admin', NOW(), 'Webhook查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:query')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'Webhook新增', (SELECT menu_id FROM sys_menu WHERE perms = 'system:webhook:list' AND menu_type = 'C' LIMIT 1), 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:webhook:add', '#', 'admin', NOW(), 'Webhook新增'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:add')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'Webhook修改', (SELECT menu_id FROM sys_menu WHERE perms = 'system:webhook:list' AND menu_type = 'C' LIMIT 1), 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:webhook:edit', '#', 'admin', NOW(), 'Webhook修改'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:edit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'Webhook删除', (SELECT menu_id FROM sys_menu WHERE perms = 'system:webhook:list' AND menu_type = 'C' LIMIT 1), 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:webhook:remove', '#', 'admin', NOW(), 'Webhook删除'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:remove')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'Webhook导出', (SELECT menu_id FROM sys_menu WHERE perms = 'system:webhook:list' AND menu_type = 'C' LIMIT 1), 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:webhook:export', '#', 'admin', NOW(), 'Webhook导出'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:export')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'Webhook测试', (SELECT menu_id FROM sys_menu WHERE perms = 'system:webhook:list' AND menu_type = 'C' LIMIT 1), 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:webhook:test', '#', 'admin', NOW(), 'Webhook测试'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:test')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:list' AND menu_type = 'C');

-- =========================
-- P0: 会员模块
-- =========================

-- 1. 会员导入按钮 → 挂到会员信息（perms=member:member:list）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '会员导入',
       (SELECT menu_id FROM sys_menu WHERE perms = 'member:member:list' AND menu_type = 'C' LIMIT 1),
       6, '', '', '', '', 1, 0, 'F', '0', '0', 'member:member:import', '#', 'admin', NOW(), '会员导入'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:member:import')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:member:list' AND menu_type = 'C');

-- 2. 查看敏感信息按钮 → 挂到会员信息
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查看敏感信息',
       (SELECT menu_id FROM sys_menu WHERE perms = 'member:member:list' AND menu_type = 'C' LIMIT 1),
       7, '', '', '', '', 1, 0, 'F', '0', '0', 'member:member:pii', '#', 'admin', NOW(), '查看会员敏感信息'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:member:pii')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:member:list' AND menu_type = 'C');

-- =========================
-- P1: 财务商品管理菜单
-- =========================

-- 商品管理 C 菜单 → 挂到财务根菜单（path='finance' AND menu_type='M'）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '商品管理',
       (SELECT menu_id FROM sys_menu WHERE path = 'finance' AND menu_type = 'M' LIMIT 1),
       70, 'product', 'finance/product/index', '', '', 1, 0, 'C', '0', '0', 'finance:product:list', 'shopping', 'admin', NOW(), '商品管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:product:list' AND menu_type = 'C')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE path = 'finance' AND menu_type = 'M');

-- 幂等 UPDATE：修正已存在商品管理 C 菜单的父级/path/component
UPDATE sys_menu SET
  parent_id = (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE path = 'finance' AND menu_type = 'M' LIMIT 1) t),
  path = 'product',
  component = 'finance/product/index',
  menu_type = 'C',
  update_time = NOW()
WHERE perms = 'finance:product:list' AND menu_type = 'C';

-- 商品按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '商品查询', (SELECT menu_id FROM sys_menu WHERE perms = 'finance:product:list' AND menu_type = 'C' LIMIT 1), 1, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:product:query', '#', 'admin', NOW(), '商品查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:product:query')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:product:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '商品新增', (SELECT menu_id FROM sys_menu WHERE perms = 'finance:product:list' AND menu_type = 'C' LIMIT 1), 2, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:product:add', '#', 'admin', NOW(), '商品新增'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:product:add')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:product:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '商品修改', (SELECT menu_id FROM sys_menu WHERE perms = 'finance:product:list' AND menu_type = 'C' LIMIT 1), 3, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:product:edit', '#', 'admin', NOW(), '商品修改'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:product:edit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:product:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '商品删除', (SELECT menu_id FROM sys_menu WHERE perms = 'finance:product:list' AND menu_type = 'C' LIMIT 1), 4, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:product:remove', '#', 'admin', NOW(), '商品删除'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:product:remove')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:product:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '商品导出', (SELECT menu_id FROM sys_menu WHERE perms = 'finance:product:list' AND menu_type = 'C' LIMIT 1), 5, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:product:export', '#', 'admin', NOW(), '商品导出'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:product:export')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:product:list' AND menu_type = 'C');

-- =========================
-- P1: 用户门店授权菜单
-- 父菜单动态查找系统管理根菜单（path='system' AND menu_type='M' AND parent_id=0）
-- =========================

-- 用户门店授权 C 菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户门店授权',
       (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0 LIMIT 1),
       88, 'userDept', 'system/userDept/index', '', '', 1, 0, 'C', '0', '0', 'system:userDept:list', 'peoples', 'admin', NOW(), '用户门店授权'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:userDept:list' AND menu_type = 'C')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0);

-- 幂等 UPDATE：修正已存在用户门店授权 C 菜单的父级/path/component
UPDATE sys_menu SET
  parent_id = (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0 LIMIT 1) t),
  path = 'userDept',
  component = 'system/userDept/index',
  menu_type = 'C',
  update_time = NOW()
WHERE perms = 'system:userDept:list' AND menu_type = 'C';

-- 用户门店授权按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '门店授权查询', (SELECT menu_id FROM sys_menu WHERE perms = 'system:userDept:list' AND menu_type = 'C' LIMIT 1), 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:userDept:query', '#', 'admin', NOW(), '门店授权查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:userDept:query')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:userDept:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '门店授权新增', (SELECT menu_id FROM sys_menu WHERE perms = 'system:userDept:list' AND menu_type = 'C' LIMIT 1), 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:userDept:add', '#', 'admin', NOW(), '门店授权新增'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:userDept:add')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:userDept:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '门店授权修改', (SELECT menu_id FROM sys_menu WHERE perms = 'system:userDept:list' AND menu_type = 'C' LIMIT 1), 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:userDept:edit', '#', 'admin', NOW(), '门店授权修改'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:userDept:edit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:userDept:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '门店授权删除', (SELECT menu_id FROM sys_menu WHERE perms = 'system:userDept:list' AND menu_type = 'C' LIMIT 1), 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:userDept:remove', '#', 'admin', NOW(), '门店授权删除'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:userDept:remove')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:userDept:list' AND menu_type = 'C');

-- =========================
-- P1: 工作流缺失按钮（仅当环境有工作流菜单时生效）
-- =========================

-- 字段权限 add/remove → 挂到字段权限（perms=workflow:fieldPermission:list）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '权限新增', (SELECT menu_id FROM sys_menu WHERE perms = 'workflow:fieldPermission:list' AND menu_type = 'C' LIMIT 1), 3, '', '', '', '', 1, 0, 'F', '0', '0', 'workflow:fieldPermission:add', '#', 'admin', NOW(), '字段权限新增'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:fieldPermission:add')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:fieldPermission:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '权限删除', (SELECT menu_id FROM sys_menu WHERE perms = 'workflow:fieldPermission:list' AND menu_type = 'C' LIMIT 1), 4, '', '', '', '', 1, 0, 'F', '0', '0', 'workflow:fieldPermission:remove', '#', 'admin', NOW(), '字段权限删除'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:fieldPermission:remove')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:fieldPermission:list' AND menu_type = 'C');

-- 流程实例 terminate/withdraw → 挂到流程实例（perms=workflow:instance:list）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '实例终止', (SELECT menu_id FROM sys_menu WHERE perms = 'workflow:instance:list' AND menu_type = 'C' LIMIT 1), 7, '', '', '', '', 1, 0, 'F', '0', '0', 'workflow:instance:terminate', '#', 'admin', NOW(), '流程实例终止'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:instance:terminate')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:instance:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '实例撤回', (SELECT menu_id FROM sys_menu WHERE perms = 'workflow:instance:list' AND menu_type = 'C' LIMIT 1), 8, '', '', '', '', 1, 0, 'F', '0', '0', 'workflow:instance:withdraw', '#', 'admin', NOW(), '流程实例撤回'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:instance:withdraw')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:instance:list' AND menu_type = 'C');

-- 任务中心 claim/urge/cc/addsign → 挂到任务中心（perms=workflow:task:list）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '任务认领', (SELECT menu_id FROM sys_menu WHERE perms = 'workflow:task:list' AND menu_type = 'C' LIMIT 1), 6, '', '', '', '', 1, 0, 'F', '0', '0', 'workflow:task:claim', '#', 'admin', NOW(), '任务认领'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:task:claim')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:task:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '任务催办', (SELECT menu_id FROM sys_menu WHERE perms = 'workflow:task:list' AND menu_type = 'C' LIMIT 1), 7, '', '', '', '', 1, 0, 'F', '0', '0', 'workflow:task:urge', '#', 'admin', NOW(), '任务催办'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:task:urge')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:task:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '任务抄送', (SELECT menu_id FROM sys_menu WHERE perms = 'workflow:task:list' AND menu_type = 'C' LIMIT 1), 8, '', '', '', '', 1, 0, 'F', '0', '0', 'workflow:task:cc', '#', 'admin', NOW(), '任务抄送'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:task:cc')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:task:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '任务加签', (SELECT menu_id FROM sys_menu WHERE perms = 'workflow:task:list' AND menu_type = 'C' LIMIT 1), 9, '', '', '', '', 1, 0, 'F', '0', '0', 'workflow:task:addsign', '#', 'admin', NOW(), '任务加签'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:task:addsign')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'workflow:task:list' AND menu_type = 'C');

-- =========================
-- P1: 开放平台缺失按钮
-- =========================

-- 应用导出/密钥查看/密钥编辑 → 挂到应用管理（perms=open:app:list）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '应用导出', (SELECT menu_id FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C' LIMIT 1), 7, '', '', '', '', 1, 0, 'F', '0', '0', 'open:app:export', '#', 'admin', NOW(), '应用导出'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:export')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '密钥查看', (SELECT menu_id FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C' LIMIT 1), 8, '', '', '', '', 1, 0, 'F', '0', '0', 'open:app:key:list', '#', 'admin', NOW(), '密钥查看'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:key:list')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '密钥重置', (SELECT menu_id FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C' LIMIT 1), 9, '', '', '', '', 1, 0, 'F', '0', '0', 'open:app:key:edit', '#', 'admin', NOW(), '密钥重置'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:key:edit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C');

-- =========================
-- 敏感权限清理：收回非 admin 角色的历史授权
-- 敏感权限定义：租户管理、Webhook、用户门店授权、开放平台密钥
-- 这些权限只应授权给 role_id=1（超管），普通角色不应拥有
-- =========================
DELETE FROM sys_role_menu
WHERE role_id <> 1
  AND menu_id IN (
    SELECT menu_id FROM sys_menu WHERE perms IN (
      'system:tenant:list', 'system:tenant:query', 'system:tenant:add', 'system:tenant:edit', 'system:tenant:remove',
      'system:webhook:list', 'system:webhook:query', 'system:webhook:add', 'system:webhook:edit', 'system:webhook:remove', 'system:webhook:export', 'system:webhook:test',
      'system:userDept:list', 'system:userDept:query', 'system:userDept:add', 'system:userDept:edit', 'system:userDept:remove',
      'open:app:key:list', 'open:app:key:edit'
    )
  );

-- =========================
-- 授权：所有新增/已有菜单授权给 role_id=1（超管）
-- =========================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN (
  'finance:expense:import', 'finance:expense:ocr', 'finance:sale:payment', 'finance:dailyReview:view',
  'system:tenant:list', 'system:tenant:query', 'system:tenant:add', 'system:tenant:edit', 'system:tenant:remove',
  'system:webhook:list', 'system:webhook:query', 'system:webhook:add', 'system:webhook:edit', 'system:webhook:remove', 'system:webhook:export', 'system:webhook:test',
  'member:member:import', 'member:member:pii',
  'finance:product:list', 'finance:product:query', 'finance:product:add', 'finance:product:edit', 'finance:product:remove', 'finance:product:export',
  'system:userDept:list', 'system:userDept:query', 'system:userDept:add', 'system:userDept:edit', 'system:userDept:remove',
  'workflow:fieldPermission:add', 'workflow:fieldPermission:remove',
  'workflow:instance:terminate', 'workflow:instance:withdraw',
  'workflow:task:claim', 'workflow:task:urge', 'workflow:task:cc', 'workflow:task:addsign',
  'open:app:export', 'open:app:key:list', 'open:app:key:edit'
)
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
);

-- =========================
-- 权限缺口豁免清单
-- 以下权限经盘点确认不挂菜单，列为豁免，不在本 SQL 补齐：
-- =========================
-- 1. system:file:upload / system:file:remove
--    豁免原因：文件上传是内部能力，不作为独立菜单开放；按需在业务页面内调用。
-- 2. open:foundation:app:list / open:foundation:key:list / open:foundation:webhook:create
--    豁免原因：平台公共基础接口，属于开放 API scope，不走后台菜单授权。
-- 3. open:member:points:write
--    豁免原因：外部开放 API 写权限，应纳入开放平台应用 scope 管理，不挂后台菜单。
-- 4. lowcode:biz:add / lowcode:biz:remove / lowcode:biz:fulfill / lowcode:biz:import / lowcode:biz:export
--    lowcode:meta:remove / lowcode:meta:publish
--    豁免原因：低代码业务权限模型待决策（按 schema 拆分），暂不补菜单；先隐藏低代码演示菜单。
-- 5. workflow:mobile:todo / workflow:mobile:done / workflow:mobile:applied / workflow:mobile:approve
--    workflow:mobile:reject / workflow:mobile:detail
--    豁免原因：移动办公权限，属于小程序内部能力，不挂后台菜单；通过小程序权限组控制。
-- 6. gen:table:list
--    豁免原因：命名不一致（代码用 gen:table:list，菜单用 tool:gen:list），应改代码统一为 tool:gen:list，不新增别名权限。
-- 7. monitor:operlog:* / monitor:logininfor:*
--    豁免原因：仅前端 v-hasPermi 备用写法，同处已有 system:* 兜底权限，无需补菜单。

-- =========================
-- 验证
-- =========================
SELECT '=== 权限补齐完成，新增/修正菜单清单 ===' AS info;
SELECT menu_id, menu_name, parent_id, path, component, perms, menu_type
FROM sys_menu
WHERE perms IN (
  'finance:expense:import', 'finance:expense:ocr', 'finance:sale:payment', 'finance:dailyReview:view',
  'system:tenant:list', 'system:tenant:query', 'system:tenant:add', 'system:tenant:edit', 'system:tenant:remove',
  'system:webhook:list', 'system:webhook:query', 'system:webhook:add', 'system:webhook:edit', 'system:webhook:remove', 'system:webhook:export', 'system:webhook:test',
  'member:member:import', 'member:member:pii',
  'finance:product:list', 'finance:product:query', 'finance:product:add', 'finance:product:edit', 'finance:product:remove', 'finance:product:export',
  'system:userDept:list', 'system:userDept:query', 'system:userDept:add', 'system:userDept:edit', 'system:userDept:remove',
  'workflow:fieldPermission:add', 'workflow:fieldPermission:remove',
  'workflow:instance:terminate', 'workflow:instance:withdraw',
  'workflow:task:claim', 'workflow:task:urge', 'workflow:task:cc', 'workflow:task:addsign',
  'open:app:export', 'open:app:key:list', 'open:app:key:edit'
)
ORDER BY perms;

-- 验证敏感权限非 admin 授权已清理
SELECT '=== 敏感权限非 admin 授权检查（应为0）===' AS info;
SELECT COUNT(*) AS sensitive_non_admin_count
FROM sys_role_menu rm
JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE rm.role_id <> 1
  AND m.perms IN (
    'system:tenant:list', 'system:tenant:query', 'system:tenant:add', 'system:tenant:edit', 'system:tenant:remove',
    'system:webhook:list', 'system:webhook:query', 'system:webhook:add', 'system:webhook:edit', 'system:webhook:remove', 'system:webhook:export', 'system:webhook:test',
    'system:userDept:list', 'system:userDept:query', 'system:userDept:add', 'system:userDept:edit', 'system:userDept:remove',
    'open:app:key:list', 'open:app:key:edit'
  );

-- 注意：执行本 SQL 后，应再跑权限缺口扫描脚本（scripts/permission-menu-gap-health.mjs）
-- 预期结果：后端权限缺失数 = 0 或全部进入上方豁免清单
