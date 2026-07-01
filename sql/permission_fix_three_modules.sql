-- =====================================================================
-- 三大模块 P0 权限收口：补齐缺失的按钮权限并授权给超级管理员角色
-- 可重复执行：所有 INSERT 使用 NOT EXISTS 守卫
-- =====================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------
-- 1. 查找父菜单 ID
-- -----------------------------------------------------------------

-- 系统管理根目录
SET @systemRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'system' AND menu_type = 'M' LIMIT 1);

-- 财务管理根目录
SET @financeRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'finance' AND menu_type = 'M' LIMIT 1);

-- 会员管理根目录
SET @memberRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'member' AND menu_type = 'M' LIMIT 1);

-- 系统管理下的参数设置菜单 (component 包含 config)
SET @configMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @systemRootId AND perms = 'system:config:list' AND menu_type = 'C' LIMIT 1);

-- 系统管理下的 Webhook 订阅菜单 (如果存在)
SET @webhookMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @systemRootId AND menu_name = 'Webhook订阅' AND menu_type = 'C' LIMIT 1);

-- 财务管理下的报表菜单 (如果存在)
SET @finReportMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @financeRootId AND menu_name LIKE '%报表%' AND menu_type = 'C' LIMIT 1);

-- 财务管理下的费用菜单 (如果存在)
SET @finExpenseMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @financeRootId AND perms = 'finance:expense:list' AND menu_type = 'C' LIMIT 1);

-- 会员管理下的报表菜单 (如果存在)
SET @memReportMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @memberRootId AND menu_name LIKE '%报表%' AND menu_type = 'C' LIMIT 1);

-- -----------------------------------------------------------------
-- 2. 系统管理 → 参数设置：补 system:config:query
-- -----------------------------------------------------------------
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '参数查询', @configMenuId, 10, '', '', '', '',
  1, 0, 'F', '0', '0', 'system:config:query', '#',
  'admin', NOW(), '', NULL, '参数配置查询权限'
WHERE @configMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:config:query' AND menu_type = 'F');

-- -----------------------------------------------------------------
-- 3. 系统管理 → Webhook 订阅：补 system:webhook:* 权限
--    如果 Webhook 菜单不存在，跳过（仅添加权限条目需要先有菜单）
-- -----------------------------------------------------------------

-- 先确保 Webhook 页面菜单存在（如果不存在则创建）
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  'Webhook订阅', @systemRootId, 99, 'webhook', 'system/webhook/index', '', '',
  1, 0, 'C', '0', '0', 'system:webhook:list', 'link',
  'admin', NOW(), '', NULL, 'Webhook订阅管理'
WHERE @systemRootId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @systemRootId AND perms = 'system:webhook:list' AND menu_type = 'C');

-- 重新获取 Webhook 菜单 ID
SET @webhookMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @systemRootId AND perms = 'system:webhook:list' AND menu_type = 'C' LIMIT 1);

-- system:webhook:query
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  'Webhook查询', @webhookMenuId, 1, '', '', '', '',
  1, 0, 'F', '0', '0', 'system:webhook:query', '#',
  'admin', NOW(), '', NULL, ''
WHERE @webhookMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:query' AND menu_type = 'F');

-- system:webhook:add
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  'Webhook新增', @webhookMenuId, 2, '', '', '', '',
  1, 0, 'F', '0', '0', 'system:webhook:add', '#',
  'admin', NOW(), '', NULL, ''
WHERE @webhookMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:add' AND menu_type = 'F');

-- system:webhook:edit
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  'Webhook修改', @webhookMenuId, 3, '', '', '', '',
  1, 0, 'F', '0', '0', 'system:webhook:edit', '#',
  'admin', NOW(), '', NULL, ''
WHERE @webhookMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:edit' AND menu_type = 'F');

-- system:webhook:remove
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  'Webhook删除', @webhookMenuId, 4, '', '', '', '',
  1, 0, 'F', '0', '0', 'system:webhook:remove', '#',
  'admin', NOW(), '', NULL, ''
WHERE @webhookMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:remove' AND menu_type = 'F');

-- system:webhook:export
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  'Webhook导出', @webhookMenuId, 5, '', '', '', '',
  1, 0, 'F', '0', '0', 'system:webhook:export', '#',
  'admin', NOW(), '', NULL, ''
WHERE @webhookMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:export' AND menu_type = 'F');

-- system:webhook:test
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  'Webhook测试', @webhookMenuId, 6, '', '', '', '',
  1, 0, 'F', '0', '0', 'system:webhook:test', '#',
  'admin', NOW(), '', NULL, ''
WHERE @webhookMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:webhook:test' AND menu_type = 'F');

-- -----------------------------------------------------------------
-- 4. 财务管理 → 报表：补 finance:report:* 权限
--    如果报表菜单不存在，先创建
-- -----------------------------------------------------------------

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '报表管理', @financeRootId, 90, 'report', 'finance/report/index', '', '',
  1, 0, 'C', '0', '0', 'finance:report:expense', 'chart',
  'admin', NOW(), '', NULL, '财务报表管理'
WHERE @financeRootId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @financeRootId AND path = 'report' AND menu_type = 'C');

SET @finReportMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @financeRootId AND path = 'report' AND menu_type = 'C' LIMIT 1);

-- finance:report:expense
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '费用报表', @finReportMenuId, 1, '', '', '', '',
  1, 0, 'F', '0', '0', 'finance:report:expense', '#',
  'admin', NOW(), '', NULL, ''
WHERE @finReportMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:report:expense' AND menu_type = 'F');

-- finance:report:cost
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '成本报表', @finReportMenuId, 2, '', '', '', '',
  1, 0, 'F', '0', '0', 'finance:report:cost', '#',
  'admin', NOW(), '', NULL, ''
WHERE @finReportMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:report:cost' AND menu_type = 'F');

-- finance:report:profitShare
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '分润报表', @finReportMenuId, 3, '', '', '', '',
  1, 0, 'F', '0', '0', 'finance:report:profitShare', '#',
  'admin', NOW(), '', NULL, ''
WHERE @finReportMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:report:profitShare' AND menu_type = 'F');

-- finance:report:sale
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '销售报表', @finReportMenuId, 4, '', '', '', '',
  1, 0, 'F', '0', '0', 'finance:report:sale', '#',
  'admin', NOW(), '', NULL, ''
WHERE @finReportMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:report:sale' AND menu_type = 'F');

-- finance:report:profit
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '利润报表', @finReportMenuId, 5, '', '', '', '',
  1, 0, 'F', '0', '0', 'finance:report:profit', '#',
  'admin', NOW(), '', NULL, ''
WHERE @finReportMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:report:profit' AND menu_type = 'F');

-- finance:report:stock
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '库存报表', @finReportMenuId, 6, '', '', '', '',
  1, 0, 'F', '0', '0', 'finance:report:stock', '#',
  'admin', NOW(), '', NULL, ''
WHERE @finReportMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:report:stock' AND menu_type = 'F');

-- -----------------------------------------------------------------
-- 5. 财务管理 → 费用：补 finance:expense:ocr
-- -----------------------------------------------------------------
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '费用OCR识别', @finExpenseMenuId, 10, '', '', '', '',
  1, 0, 'F', '0', '0', 'finance:expense:ocr', '#',
  'admin', NOW(), '', NULL, ''
WHERE @finExpenseMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:expense:ocr' AND menu_type = 'F');

-- -----------------------------------------------------------------
-- 6. 会员管理 → 报表：补 member:report:* 权限
--    如果报表菜单不存在，先创建
-- -----------------------------------------------------------------

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '报表管理', @memberRootId, 90, 'report', 'member/report/index', '', '',
  1, 0, 'C', '0', '0', 'member:report:member', 'chart',
  'admin', NOW(), '', NULL, '会员报表管理'
WHERE @memberRootId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @memberRootId AND path = 'report' AND menu_type = 'C');

SET @memReportMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @memberRootId AND path = 'report' AND menu_type = 'C' LIMIT 1);

-- member:report:member
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '会员报表', @memReportMenuId, 1, '', '', '', '',
  1, 0, 'F', '0', '0', 'member:report:member', '#',
  'admin', NOW(), '', NULL, ''
WHERE @memReportMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:report:member' AND menu_type = 'F');

-- member:report:seckill
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '秒杀报表', @memReportMenuId, 2, '', '', '', '',
  1, 0, 'F', '0', '0', 'member:report:seckill', '#',
  'admin', NOW(), '', NULL, ''
WHERE @memReportMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:report:seckill' AND menu_type = 'F');

-- -----------------------------------------------------------------
-- 6b. 会员管理 → 会员信息：补 member:member:pii 查看明文权限
-- -----------------------------------------------------------------

SET @memberListMenuId := (SELECT menu_id FROM sys_menu WHERE parent_id = @memberRootId AND perms = 'member:member:list' AND menu_type = 'C' LIMIT 1);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu),
  '查看会员敏感信息', @memberListMenuId, 10, '', '', '', '',
  1, 0, 'F', '0', '0', 'member:member:pii', '#',
  'admin', NOW(), '', NULL, '查看会员手机号/身份证/地址明文'
WHERE @memberListMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:member:pii' AND menu_type = 'F');

-- -----------------------------------------------------------------
-- 7. 授权：将所有新增权限授予超级管理员角色 (role_id = 1)
--    不得授予普通租户角色
-- -----------------------------------------------------------------

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN (
  'system:config:query',
  'system:webhook:list',
  'system:webhook:query',
  'system:webhook:add',
  'system:webhook:edit',
  'system:webhook:remove',
  'system:webhook:export',
  'system:webhook:test',
  'finance:report:expense',
  'finance:report:cost',
  'finance:report:profitShare',
  'finance:report:sale',
  'finance:report:profit',
  'finance:report:stock',
  'finance:expense:ocr',
  'member:report:member',
  'member:report:seckill',
  'member:member:pii'
)
AND menu_type = 'F'
AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm
  WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id
);
