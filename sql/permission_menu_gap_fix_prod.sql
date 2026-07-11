-- ============================================================
-- PROD 权限菜单缺口补齐（V2 扫描发现的 46 个真实缺失）
-- 基于 DEV 菜单结构，通过权限码/path 动态查找父菜单
-- 仅在 PROD 执行；DEV 已通过 permission_menu_gap_fix.sql 补齐
-- ============================================================

-- =========================
-- 1. 退款申请（member:refund:*）→ 挂到会员管理根菜单（path='member' AND menu_type='M'）
-- =========================

-- C 菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '退款申请',
       (SELECT menu_id FROM sys_menu WHERE path = 'member' AND menu_type = 'M' LIMIT 1),
       30, 'refund', 'member/refund/index', '', '', 1, 0, 'C', '0', '0', 'member:refund:list', 'refund', 'admin', NOW(), '退款申请'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:refund:list' AND menu_type = 'C')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE path = 'member' AND menu_type = 'M');

-- F 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '退款申请查询', (SELECT menu_id FROM sys_menu WHERE perms = 'member:refund:list' AND menu_type = 'C' LIMIT 1), 1, '', '', '', '', 1, 0, 'F', '0', '0', 'member:refund:query', '#', 'admin', NOW(), '退款申请查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:refund:query')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:refund:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '退款申请新增', (SELECT menu_id FROM sys_menu WHERE perms = 'member:refund:list' AND menu_type = 'C' LIMIT 1), 2, '', '', '', '', 1, 0, 'F', '0', '0', 'member:refund:add', '#', 'admin', NOW(), '退款申请新增'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:refund:add')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:refund:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '退款申请修改', (SELECT menu_id FROM sys_menu WHERE perms = 'member:refund:list' AND menu_type = 'C' LIMIT 1), 3, '', '', '', '', 1, 0, 'F', '0', '0', 'member:refund:edit', '#', 'admin', NOW(), '退款申请修改'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:refund:edit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:refund:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '退款申请删除', (SELECT menu_id FROM sys_menu WHERE perms = 'member:refund:list' AND menu_type = 'C' LIMIT 1), 4, '', '', '', '', 1, 0, 'F', '0', '0', 'member:refund:remove', '#', 'admin', NOW(), '退款申请删除'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:refund:remove')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:refund:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '退款申请提交审批', (SELECT menu_id FROM sys_menu WHERE perms = 'member:refund:list' AND menu_type = 'C' LIMIT 1), 5, '', '', '', '', 1, 0, 'F', '0', '0', 'member:refund:submit', '#', 'admin', NOW(), '退款申请提交审批'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:refund:submit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:refund:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '退款申请撤回', (SELECT menu_id FROM sys_menu WHERE perms = 'member:refund:list' AND menu_type = 'C' LIMIT 1), 6, '', '', '', '', 1, 0, 'F', '0', '0', 'member:refund:withdraw', '#', 'admin', NOW(), '退款申请撤回'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:refund:withdraw')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:refund:list' AND menu_type = 'C');

-- =========================
-- 2. 门店开业申请（system:storeOpening:*）→ 挂到系统管理根菜单（path='system' AND menu_type='M' AND parent_id=0）
-- =========================

-- C 菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '门店开业申请',
       (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0 LIMIT 1),
       95, 'storeOpening', 'system/storeOpening/index', '', '', 1, 0, 'C', '0', '0', 'system:storeOpening:list', 'shop', 'admin', NOW(), '门店开业申请'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:storeOpening:list' AND menu_type = 'C')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0);

-- F 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '门店开业查询', (SELECT menu_id FROM sys_menu WHERE perms = 'system:storeOpening:list' AND menu_type = 'C' LIMIT 1), 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:storeOpening:query', '#', 'admin', NOW(), '门店开业查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:storeOpening:query')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:storeOpening:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '门店开业新增', (SELECT menu_id FROM sys_menu WHERE perms = 'system:storeOpening:list' AND menu_type = 'C' LIMIT 1), 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:storeOpening:add', '#', 'admin', NOW(), '门店开业新增'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:storeOpening:add')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:storeOpening:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '门店开业修改', (SELECT menu_id FROM sys_menu WHERE perms = 'system:storeOpening:list' AND menu_type = 'C' LIMIT 1), 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:storeOpening:edit', '#', 'admin', NOW(), '门店开业修改'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:storeOpening:edit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:storeOpening:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '门店开业删除', (SELECT menu_id FROM sys_menu WHERE perms = 'system:storeOpening:list' AND menu_type = 'C' LIMIT 1), 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:storeOpening:remove', '#', 'admin', NOW(), '门店开业删除'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:storeOpening:remove')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:storeOpening:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '门店开业提交审批', (SELECT menu_id FROM sys_menu WHERE perms = 'system:storeOpening:list' AND menu_type = 'C' LIMIT 1), 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:storeOpening:submit', '#', 'admin', NOW(), '门店开业提交审批'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:storeOpening:submit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:storeOpening:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '门店开业撤回', (SELECT menu_id FROM sys_menu WHERE perms = 'system:storeOpening:list' AND menu_type = 'C' LIMIT 1), 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:storeOpening:withdraw', '#', 'admin', NOW(), '门店开业撤回'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:storeOpening:withdraw')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:storeOpening:list' AND menu_type = 'C');

-- =========================
-- 3. 复盘知识库（finance:reviewKnowledge:*）→ 挂到财务根菜单（path='finance' AND menu_type='M'）
-- =========================

-- C 菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '复盘知识库',
       (SELECT menu_id FROM sys_menu WHERE path = 'finance' AND menu_type = 'M' LIMIT 1),
       80, 'review-knowledge', 'finance/reviewKnowledge/index', '', '', 1, 0, 'C', '0', '0', 'finance:reviewKnowledge:list', 'guide', 'admin', NOW(), '复盘知识库'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:reviewKnowledge:list' AND menu_type = 'C')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE path = 'finance' AND menu_type = 'M');

-- F 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识新增', (SELECT menu_id FROM sys_menu WHERE perms = 'finance:reviewKnowledge:list' AND menu_type = 'C' LIMIT 1), 1, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:reviewKnowledge:add', '#', 'admin', NOW(), '知识新增'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:reviewKnowledge:add')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:reviewKnowledge:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识修改', (SELECT menu_id FROM sys_menu WHERE perms = 'finance:reviewKnowledge:list' AND menu_type = 'C' LIMIT 1), 2, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:reviewKnowledge:edit', '#', 'admin', NOW(), '知识修改'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:reviewKnowledge:edit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:reviewKnowledge:list' AND menu_type = 'C');

-- =========================
-- 4. 销售钻取（finance:drilldown:sales）→ 挂到经营总览（perms=finance:dashboard:operation）
-- =========================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '销售钻取',
       (SELECT menu_id FROM sys_menu WHERE perms = 'finance:dashboard:operation' AND menu_type = 'C' LIMIT 1),
       10, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:drilldown:sales', '#', 'admin', NOW(), '销售钻取'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:drilldown:sales')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:dashboard:operation' AND menu_type = 'C');

-- =========================
-- 5. 开放平台（open:app/contract/isv/log:*）
-- PROD 无开放平台根菜单，需先创建 M 根菜单，再创建 C 菜单 + F 按钮
-- =========================

-- 开放平台 M 根菜单 → 挂到顶级（parent_id=0）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '开放平台', 0, 90, 'open', '', '', '', 1, 0, 'M', '0', '0', '', 'international', 'admin', NOW(), '开放平台'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = 'open' AND menu_type = 'M' AND parent_id = 0);

-- 应用管理 C 菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '应用管理',
       (SELECT menu_id FROM sys_menu WHERE path = 'open' AND menu_type = 'M' AND parent_id = 0 LIMIT 1),
       10, 'app', 'open/app/index', '', '', 1, 0, 'C', '0', '0', 'open:app:list', 'app', 'admin', NOW(), '应用管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE path = 'open' AND menu_type = 'M' AND parent_id = 0);

-- 应用管理 F 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '应用查询', (SELECT menu_id FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C' LIMIT 1), 1, '', '', '', '', 1, 0, 'F', '0', '0', 'open:app:query', '#', 'admin', NOW(), '应用查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:query')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '应用新增', (SELECT menu_id FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C' LIMIT 1), 2, '', '', '', '', 1, 0, 'F', '0', '0', 'open:app:add', '#', 'admin', NOW(), '应用新增'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:add')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '应用修改', (SELECT menu_id FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C' LIMIT 1), 3, '', '', '', '', 1, 0, 'F', '0', '0', 'open:app:edit', '#', 'admin', NOW(), '应用修改'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:edit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '应用删除', (SELECT menu_id FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C' LIMIT 1), 4, '', '', '', '', 1, 0, 'F', '0', '0', 'open:app:remove', '#', 'admin', NOW(), '应用删除'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:remove')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '应用审批', (SELECT menu_id FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C' LIMIT 1), 5, '', '', '', '', 1, 0, 'F', '0', '0', 'open:app:approve', '#', 'admin', NOW(), '应用审批'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:approve')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '应用导出', (SELECT menu_id FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C' LIMIT 1), 6, '', '', '', '', 1, 0, 'F', '0', '0', 'open:app:export', '#', 'admin', NOW(), '应用导出'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:export')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '密钥查看', (SELECT menu_id FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C' LIMIT 1), 7, '', '', '', '', 1, 0, 'F', '0', '0', 'open:app:key:list', '#', 'admin', NOW(), '密钥查看'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:key:list')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '密钥重置', (SELECT menu_id FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C' LIMIT 1), 8, '', '', '', '', 1, 0, 'F', '0', '0', 'open:app:key:edit', '#', 'admin', NOW(), '密钥重置'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:key:edit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:app:list' AND menu_type = 'C');

-- ISV 管理 C 菜单（无前端组件，visible='1' 隐藏，仅作为 API 权限按钮配置入口）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'ISV管理',
       (SELECT menu_id FROM sys_menu WHERE path = 'open' AND menu_type = 'M' AND parent_id = 0 LIMIT 1),
       20, 'isv', '', '', '', 1, 0, 'C', '1', '0', 'open:isv:list', 'people', 'admin', NOW(), 'ISV管理（无组件，隐藏）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:list' AND menu_type = 'C')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE path = 'open' AND menu_type = 'M' AND parent_id = 0);

-- 幂等 UPDATE：修正已存在 ISV管理 C 菜单为隐藏（组件为空，避免 404/空白页）
UPDATE sys_menu SET visible = '1', update_time = NOW()
WHERE perms = 'open:isv:list' AND menu_type = 'C' AND (component IS NULL OR component = '');

-- ISV F 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'ISV查询', (SELECT menu_id FROM sys_menu WHERE perms = 'open:isv:list' AND menu_type = 'C' LIMIT 1), 1, '', '', '', '', 1, 0, 'F', '0', '0', 'open:isv:query', '#', 'admin', NOW(), 'ISV查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:query')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'ISV新增', (SELECT menu_id FROM sys_menu WHERE perms = 'open:isv:list' AND menu_type = 'C' LIMIT 1), 2, '', '', '', '', 1, 0, 'F', '0', '0', 'open:isv:add', '#', 'admin', NOW(), 'ISV新增'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:add')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'ISV修改', (SELECT menu_id FROM sys_menu WHERE perms = 'open:isv:list' AND menu_type = 'C' LIMIT 1), 3, '', '', '', '', 1, 0, 'F', '0', '0', 'open:isv:edit', '#', 'admin', NOW(), 'ISV修改'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:edit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'ISV删除', (SELECT menu_id FROM sys_menu WHERE perms = 'open:isv:list' AND menu_type = 'C' LIMIT 1), 4, '', '', '', '', 1, 0, 'F', '0', '0', 'open:isv:remove', '#', 'admin', NOW(), 'ISV删除'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:remove')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'ISV审核', (SELECT menu_id FROM sys_menu WHERE perms = 'open:isv:list' AND menu_type = 'C' LIMIT 1), 5, '', '', '', '', 1, 0, 'F', '0', '0', 'open:isv:approve', '#', 'admin', NOW(), 'ISV审核'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:approve')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:isv:list' AND menu_type = 'C');

-- 合约管理 C 菜单（无前端组件，visible='1' 隐藏，仅作为 API 权限按钮配置入口）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '合约管理',
       (SELECT menu_id FROM sys_menu WHERE path = 'open' AND menu_type = 'M' AND parent_id = 0 LIMIT 1),
       30, 'contract', '', '', '', 1, 0, 'C', '1', '0', 'open:contract:list', 'documentation', 'admin', NOW(), '合约管理（无组件，隐藏）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:contract:list' AND menu_type = 'C')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE path = 'open' AND menu_type = 'M' AND parent_id = 0);

-- 幂等 UPDATE：修正已存在 合约管理 C 菜单为隐藏（组件为空，避免 404/空白页）
UPDATE sys_menu SET visible = '1', update_time = NOW()
WHERE perms = 'open:contract:list' AND menu_type = 'C' AND (component IS NULL OR component = '');

-- 合约 F 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '合约查询', (SELECT menu_id FROM sys_menu WHERE perms = 'open:contract:list' AND menu_type = 'C' LIMIT 1), 1, '', '', '', '', 1, 0, 'F', '0', '0', 'open:contract:query', '#', 'admin', NOW(), '合约查询'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:contract:query')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:contract:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '合约新增', (SELECT menu_id FROM sys_menu WHERE perms = 'open:contract:list' AND menu_type = 'C' LIMIT 1), 2, '', '', '', '', 1, 0, 'F', '0', '0', 'open:contract:add', '#', 'admin', NOW(), '合约新增'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:contract:add')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:contract:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '合约修改', (SELECT menu_id FROM sys_menu WHERE perms = 'open:contract:list' AND menu_type = 'C' LIMIT 1), 3, '', '', '', '', 1, 0, 'F', '0', '0', 'open:contract:edit', '#', 'admin', NOW(), '合约修改'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:contract:edit')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:contract:list' AND menu_type = 'C');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '合约删除', (SELECT menu_id FROM sys_menu WHERE perms = 'open:contract:list' AND menu_type = 'C' LIMIT 1), 4, '', '', '', '', 1, 0, 'F', '0', '0', 'open:contract:remove', '#', 'admin', NOW(), '合约删除'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:contract:remove')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:contract:list' AND menu_type = 'C');

-- API调用日志 C 菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'API调用日志',
       (SELECT menu_id FROM sys_menu WHERE path = 'open' AND menu_type = 'M' AND parent_id = 0 LIMIT 1),
       40, 'log', 'open/log/index', '', '', 1, 0, 'C', '0', '0', 'open:log:list', 'log', 'admin', NOW(), 'API调用日志'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:log:list' AND menu_type = 'C')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE path = 'open' AND menu_type = 'M' AND parent_id = 0);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '日志导出', (SELECT menu_id FROM sys_menu WHERE perms = 'open:log:list' AND menu_type = 'C' LIMIT 1), 1, '', '', '', '', 1, 0, 'F', '0', '0', 'open:log:export', '#', 'admin', NOW(), '日志导出'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:log:export')
  AND EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'open:log:list' AND menu_type = 'C');

-- =========================
-- 6. 租户管理（system:tenant:*）
-- PROD 当前为单租户环境，租户管理功能暂不启用，列为豁免
-- 如需启用，需先创建 C 菜单再补按钮，本 SQL 暂不处理
-- =========================
-- 豁免：system:tenant:list/query/add/edit/remove（PROD 单租户，暂不启用）

-- =========================
-- 7. 工作台通知（system:workbench:notify）
-- PROD 暂未启用工作台通知功能，列为豁免
-- =========================
-- 豁免：system:workbench:notify（PROD 暂未启用工作台通知）

-- =========================
-- 授权：所有新增菜单授权给 role_id=1（超管）
-- =========================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN (
  'member:refund:list', 'member:refund:query', 'member:refund:add', 'member:refund:edit', 'member:refund:remove', 'member:refund:submit', 'member:refund:withdraw',
  'system:storeOpening:list', 'system:storeOpening:query', 'system:storeOpening:add', 'system:storeOpening:edit', 'system:storeOpening:remove', 'system:storeOpening:submit', 'system:storeOpening:withdraw',
  'finance:reviewKnowledge:list', 'finance:reviewKnowledge:add', 'finance:reviewKnowledge:edit',
  'finance:drilldown:sales',
  'open:app:list', 'open:app:query', 'open:app:add', 'open:app:edit', 'open:app:remove', 'open:app:approve', 'open:app:export', 'open:app:key:list', 'open:app:key:edit',
  'open:isv:list', 'open:isv:query', 'open:isv:add', 'open:isv:edit', 'open:isv:remove', 'open:isv:approve',
  'open:contract:list', 'open:contract:query', 'open:contract:add', 'open:contract:edit', 'open:contract:remove',
  'open:log:list', 'open:log:export'
)
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
);

-- =========================
-- 验证
-- =========================
SELECT '=== PROD 权限补齐完成，新增菜单清单 ===' AS info;
SELECT menu_id, menu_name, parent_id, path, component, perms, menu_type
FROM sys_menu
WHERE perms IN (
  'member:refund:list', 'member:refund:query', 'member:refund:add', 'member:refund:edit', 'member:refund:remove', 'member:refund:submit', 'member:refund:withdraw',
  'system:storeOpening:list', 'system:storeOpening:query', 'system:storeOpening:add', 'system:storeOpening:edit', 'system:storeOpening:remove', 'system:storeOpening:submit', 'system:storeOpening:withdraw',
  'finance:reviewKnowledge:list', 'finance:reviewKnowledge:add', 'finance:reviewKnowledge:edit',
  'finance:drilldown:sales',
  'open:app:list', 'open:app:query', 'open:app:add', 'open:app:edit', 'open:app:remove', 'open:app:approve', 'open:app:export', 'open:app:key:list', 'open:app:key:edit',
  'open:isv:list', 'open:isv:query', 'open:isv:add', 'open:isv:edit', 'open:isv:remove', 'open:isv:approve',
  'open:contract:list', 'open:contract:query', 'open:contract:add', 'open:contract:edit', 'open:contract:remove',
  'open:log:list', 'open:log:export'
)
ORDER BY perms;
