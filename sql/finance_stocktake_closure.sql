SET NAMES utf8mb4;

-- ==========================================================================
-- 库存盘点闭环迁移：表结构、权限、字典、对账
-- 目标：为盘点工作流（DRAFT→COUNTING→SUBMITTED→RECOUNTING→APPROVED→POSTED）
--       与整单冲销 REVERSED 建立可审计的 schema 基础。
-- 原则：非破坏、可重复执行；所有 INSERT 使用 WHERE NOT EXISTS 守卫。
-- 表结构严格对齐 docs/superpowers/plans/2026-07-25-inventory-stocktake-practical-intelligence.md 第 5 节。
-- ==========================================================================

-- Step 1: 幂等创建盘点头表 finance_stocktake
CREATE TABLE IF NOT EXISTS finance_stocktake (
  stocktake_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '盘点任务ID',
  tenant_id         BIGINT       NOT NULL                COMMENT '租户ID',
  take_no           VARCHAR(64)  NOT NULL                COMMENT '盘点单号（租户内唯一）',
  dept_id           BIGINT       NOT NULL                COMMENT '门店ID',
  scope_type        VARCHAR(32)  NOT NULL                COMMENT '盘点范围类型: SELECTED_PRODUCTS/FULL_DEPT',
  status            VARCHAR(24)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/COUNTING/SUBMITTED/RECOUNTING/APPROVED/POSTED/CANCELLED/REVERSED',
  freeze_time       DATETIME     NOT NULL                COMMENT '冻结时间（冻结时刻的结存作为 expected_quantity）',
  counter_user_id   BIGINT       NOT NULL                COMMENT '盘点人ID',
  recount_user_id   BIGINT       DEFAULT NULL            COMMENT '复盘人ID（阈值触发的复盘必须与盘点人不同）',
  submitted_by      VARCHAR(64)  DEFAULT NULL            COMMENT '提交人',
  submitted_time    DATETIME     DEFAULT NULL            COMMENT '提交时间',
  approved_by       VARCHAR(64)  DEFAULT NULL            COMMENT '审批人',
  approved_time     DATETIME     DEFAULT NULL            COMMENT '审批时间',
  posted_by         VARCHAR(64)  DEFAULT NULL            COMMENT '过账人',
  posted_time       DATETIME     DEFAULT NULL            COMMENT '过账时间',
  reversed_by       VARCHAR(64)  DEFAULT NULL            COMMENT '冲销人',
  reversed_time     DATETIME     DEFAULT NULL            COMMENT '冲销时间',
  reversal_reason   VARCHAR(256) DEFAULT NULL            COMMENT '冲销原因',
  version           INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  remark            VARCHAR(256) DEFAULT NULL            COMMENT '备注',
  create_by         VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '创建者',
  create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by         VARCHAR(64)  DEFAULT NULL            COMMENT '更新者',
  update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (stocktake_id),
  UNIQUE KEY uk_stocktake_tenant_no (tenant_id, take_no),
  KEY idx_stocktake_tenant_dept_status (tenant_id, dept_id, status),
  KEY idx_stocktake_tenant_counter (tenant_id, counter_user_id, status),
  KEY idx_stocktake_tenant_posted (tenant_id, posted_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存盘点任务头表';

-- Step 2: 幂等创建盘点行表 finance_stocktake_item
CREATE TABLE IF NOT EXISTS finance_stocktake_item (
  item_id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '盘点行ID',
  stocktake_id              BIGINT       NOT NULL                COMMENT '盘点任务ID',
  tenant_id                 BIGINT       NOT NULL                COMMENT '租户ID',
  dept_id                   BIGINT       NOT NULL                COMMENT '门店ID',
  product_id                BIGINT       NOT NULL                COMMENT '商品ID',
  product_name              VARCHAR(128) NOT NULL                COMMENT '商品名称（冻结时快照）',
  expected_quantity         INT          NOT NULL                COMMENT '冻结时预期数量',
  movement_quantity_after_freeze INT     NOT NULL DEFAULT 0      COMMENT '冻结后到过账前的净移动量',
  adjusted_expected_quantity INT         DEFAULT NULL            COMMENT '调整后预期数量 = expected + movement_after_freeze',
  actual_quantity           INT          DEFAULT NULL            COMMENT '盘点实际数量（盲盘录入）',
  recount_quantity          INT          DEFAULT NULL            COMMENT '复盘数量',
  final_quantity            INT          DEFAULT NULL            COMMENT '最终确认数量（审批后）',
  variance_quantity         INT          DEFAULT NULL            COMMENT '差异数量 = final - adjusted_expected',
  unit_cost                 DECIMAL(18,2) DEFAULT NULL           COMMENT '单位成本（过账时锁定的移动加权平均成本）',
  variance_amount           DECIMAL(18,2) DEFAULT NULL           COMMENT '差异金额 = variance_quantity * unit_cost',
  reason_code               VARCHAR(32)  DEFAULT NULL            COMMENT '损耗原因码: EXPIRED/DAMAGED/THEFT/WEIGHING/OPERATION/MISSING_TRANSACTION/OTHER',
  reason                    VARCHAR(256) DEFAULT NULL            COMMENT '损耗原因描述',
  attachments               JSON         DEFAULT NULL            COMMENT '附件（图片等证据）',
  counted_by                VARCHAR(64)  DEFAULT NULL            COMMENT '盘点人',
  counted_time              DATETIME     DEFAULT NULL            COMMENT '盘点时间',
  recounted_by              VARCHAR(64)  DEFAULT NULL            COMMENT '复盘人',
  recounted_time            DATETIME     DEFAULT NULL            COMMENT '复盘时间',
  stock_ledger_id           BIGINT       DEFAULT NULL            COMMENT '过账生成的库存流水ID',
  cost_ledger_id            BIGINT       DEFAULT NULL            COMMENT '过账生成的成本流水ID',
  reverse_stock_ledger_id   BIGINT       DEFAULT NULL            COMMENT '冲销生成的反向库存流水ID',
  reverse_cost_ledger_id    BIGINT       DEFAULT NULL            COMMENT '冲销生成的反向成本流水ID',
  count_idempotency_key     VARCHAR(96)  DEFAULT NULL            COMMENT '盘点幂等键（tenant+key 唯一）',
  version                   INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  PRIMARY KEY (item_id),
  UNIQUE KEY uk_stocktake_product (stocktake_id, product_id),
  UNIQUE KEY uk_stocktake_count_key (tenant_id, count_idempotency_key),
  KEY idx_stocktake_item_tenant_dept_product (tenant_id, dept_id, product_id),
  KEY idx_stocktake_item_tenant_reason (tenant_id, reason_code),
  KEY idx_stocktake_item_tenant_variance (tenant_id, variance_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存盘点行表';

-- Step 3: 幂等创建盘点历史表 finance_stocktake_history
CREATE TABLE IF NOT EXISTS finance_stocktake_history (
  history_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '历史ID',
  stocktake_id  BIGINT       NOT NULL                COMMENT '盘点任务ID',
  tenant_id     BIGINT       NOT NULL                COMMENT '租户ID',
  action        VARCHAR(32)  NOT NULL                COMMENT '动作: CREATE/ASSIGN/START/COUNT/SUBMIT/RECOUNT/APPROVE/POST/CANCEL/REVERSE',
  from_status   VARCHAR(24)  DEFAULT NULL            COMMENT '原状态',
  to_status     VARCHAR(24)  NOT NULL                COMMENT '目标状态',
  operator      VARCHAR(64)  NOT NULL                COMMENT '操作者',
  comment       VARCHAR(512) DEFAULT NULL            COMMENT '操作备注',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (history_id),
  KEY idx_stocktake_history_tenant_id (tenant_id, stocktake_id, create_time),
  KEY idx_stocktake_history_tenant_action (tenant_id, action, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存盘点动作历史（不可变审计）';

-- ==========================================================================
-- Step 4: 损耗原因字典种子
-- 挂在 sys_dict_type / sys_dict_data 下，可重复执行
-- ==========================================================================

-- 字典类型：盘点损耗原因
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '盘点损耗原因', 'finance_stocktake_loss_reason', '0', 'admin', sysdate(), '库存盘点损耗原因码字典'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type WHERE dict_type = 'finance_stocktake_loss_reason'
);

-- 字典数据：7 个标准原因码
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '过期', 'EXPIRED', 'finance_stocktake_loss_reason', '', 'danger', 'N', '0', 'admin', sysdate(), '商品过期损坏'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'finance_stocktake_loss_reason' AND dict_value = 'EXPIRED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '破损', 'DAMAGED', 'finance_stocktake_loss_reason', '', 'warning', 'N', '0', 'admin', sysdate(), '运输/搬运破损'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'finance_stocktake_loss_reason' AND dict_value = 'DAMAGED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '盗窃', 'THEFT', 'finance_stocktake_loss_reason', '', 'danger', 'N', '0', 'admin', sysdate(), '失窃'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'finance_stocktake_loss_reason' AND dict_value = 'THEFT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '称重误差', 'WEIGHING', 'finance_stocktake_loss_reason', '', 'info', 'N', '0', 'admin', sysdate(), '散装称重误差'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'finance_stocktake_loss_reason' AND dict_value = 'WEIGHING');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '运营损耗', 'OPERATION', 'finance_stocktake_loss_reason', '', 'warning', 'N', '0', 'admin', sysdate(), '运营过程损耗'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'finance_stocktake_loss_reason' AND dict_value = 'OPERATION');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '漏记交易', 'MISSING_TRANSACTION', 'finance_stocktake_loss_reason', '', 'danger', 'N', '0', 'admin', sysdate(), '未入账交易导致差异'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'finance_stocktake_loss_reason' AND dict_value = 'MISSING_TRANSACTION');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 7, '其他', 'OTHER', 'finance_stocktake_loss_reason', '', 'info', 'N', '0', 'admin', sysdate(), '其他原因'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'finance_stocktake_loss_reason' AND dict_value = 'OTHER');

-- ==========================================================================
-- Step 5: 菜单与 10 个分离权限
-- 挂在库存报表菜单 menu_id=2155（finance:report:stock）下作为子菜单
-- 权限码：finance:stocktake:list/query/add/assign/count/submit/recount/approve/post/reverse/export
-- 授权给超级管理员(role_id=1)和财务角色(role_id=100)
-- menu_id 从 2160 开始，避免与既有 2156/2157 冲突
-- ==========================================================================

-- 盘点工作台目录菜单（C 类型）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2160, '库存盘点', 2155, 10, 'stocktake', 'finance/stocktake/index', 1, 0, 'C', '0', '0', 'finance:stocktake:list', 'list', 'admin', sysdate(), '', NULL, '库存盘点工作台'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2160);

-- 查询权限（按钮）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2161, '盘点查询', 2160, 1, '', '', 1, 0, 'F', '0', '0', 'finance:stocktake:query', '#', 'admin', sysdate(), '', NULL, '盘点任务详情查询'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2161);

-- 新增权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2162, '盘点新增', 2160, 2, '', '', 1, 0, 'F', '0', '0', 'finance:stocktake:add', '#', 'admin', sysdate(), '', NULL, '创建盘点任务'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2162);

-- 分配权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2163, '盘点分配', 2160, 3, '', '', 1, 0, 'F', '0', '0', 'finance:stocktake:assign', '#', 'admin', sysdate(), '', NULL, '分配盘点人和复盘人'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2163);

-- 盘点录入权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2164, '盘点录入', 2160, 4, '', '', 1, 0, 'F', '0', '0', 'finance:stocktake:count', '#', 'admin', sysdate(), '', NULL, '盲盘数量录入'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2164);

-- 提交权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2165, '盘点提交', 2160, 5, '', '', 1, 0, 'F', '0', '0', 'finance:stocktake:submit', '#', 'admin', sysdate(), '', NULL, '提交盘点结果'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2165);

-- 复盘权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2166, '复盘', 2160, 6, '', '', 1, 0, 'F', '0', '0', 'finance:stocktake:recount', '#', 'admin', sysdate(), '', NULL, '阈值触发复盘'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2166);

-- 审批权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2167, '盘点审批', 2160, 7, '', '', 1, 0, 'F', '0', '0', 'finance:stocktake:approve', '#', 'admin', sysdate(), '', NULL, '审批盘点结果'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2167);

-- 过账权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2168, '盘点过账', 2160, 8, '', '', 1, 0, 'F', '0', '0', 'finance:stocktake:post', '#', 'admin', sysdate(), '', NULL, '数量与成本原子过账'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2168);

-- 冲销权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2169, '盘点冲销', 2160, 9, '', '', 1, 0, 'F', '0', '0', 'finance:stocktake:reverse', '#', 'admin', sysdate(), '', NULL, '整单冲销'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2169);

-- 导出权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2170, '盘点导出', 2160, 10, '', '', 1, 0, 'F', '0', '0', 'finance:stocktake:export', '#', 'admin', sysdate(), '', NULL, '导出盘点与损耗明细'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2170);

-- ==========================================================================
-- Step 6: 角色授权（超级管理员 role_id=1，财务角色 role_id=100）
-- ==========================================================================

-- 超级管理员授权全部 11 个菜单（2160-2170）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id BETWEEN 2160 AND 2170
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);

-- 财务角色授权全部 11 个菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 100, m.menu_id FROM sys_menu m
WHERE m.menu_id BETWEEN 2160 AND 2170
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 100 AND rm.menu_id = m.menu_id);

-- ==========================================================================
-- Step 7: 对账输出
-- ==========================================================================

-- 对账 1: 头表状态分布
SELECT 'stocktake_status_distribution' AS reconciliation_type,
       status, COUNT(*) AS cnt
FROM finance_stocktake
GROUP BY status;

-- 对账 2: 行表 reason_code 分布
SELECT 'stocktake_item_reason_distribution' AS reconciliation_type,
       reason_code, COUNT(*) AS cnt
FROM finance_stocktake_item
WHERE reason_code IS NOT NULL
GROUP BY reason_code;

-- 对账 3: 已过账但无 stock_ledger_id 的异常行
SELECT 'posted_without_stock_ledger' AS reconciliation_type,
       COUNT(*) AS mismatch_count
FROM finance_stocktake_item itm
JOIN finance_stocktake st ON st.stocktake_id = itm.stocktake_id AND st.tenant_id = itm.tenant_id
WHERE st.status = 'POSTED' AND itm.stock_ledger_id IS NULL;

-- 对账 4: 已冲销但无 reverse_stock_ledger_id 的异常行
SELECT 'reversed_without_reverse_ledger' AS reconciliation_type,
       COUNT(*) AS mismatch_count
FROM finance_stocktake_item itm
JOIN finance_stocktake st ON st.stocktake_id = itm.stocktake_id AND st.tenant_id = itm.tenant_id
WHERE st.status = 'REVERSED' AND itm.reverse_stock_ledger_id IS NULL;

-- 对账 5: count_idempotency_key 重复检测（应为 0，唯一键保证）
SELECT 'duplicate_count_idempotency_key' AS reconciliation_type,
       COUNT(*) AS duplicate_count
FROM finance_stocktake_item
WHERE count_idempotency_key IS NOT NULL
GROUP BY tenant_id, count_idempotency_key
HAVING COUNT(*) > 1;

-- ==========================================================================
-- Step 8: 中文编码验证（HEX 输出）
-- ==========================================================================

SELECT menu_id, menu_name, perms, HEX(menu_name) AS menu_name_hex
FROM sys_menu
WHERE menu_id BETWEEN 2160 AND 2170
ORDER BY menu_id;

SELECT dict_type, dict_label, dict_value, HEX(dict_label) AS dict_label_hex
FROM sys_dict_data
WHERE dict_type = 'finance_stocktake_loss_reason'
ORDER BY dict_sort;

-- ==========================================================================
-- 回滚说明（不在本脚本执行，仅记录）
-- ==========================================================================
-- 回滚步骤（仅当需要时手动执行）：
-- 1. DELETE FROM sys_role_menu WHERE menu_id BETWEEN 2160 AND 2170;
-- 2. DELETE FROM sys_menu WHERE menu_id BETWEEN 2160 AND 2170;
-- 3. DELETE FROM sys_dict_data WHERE dict_type = 'finance_stocktake_loss_reason';
-- 4. DELETE FROM sys_dict_type WHERE dict_type = 'finance_stocktake_loss_reason';
-- 5. DROP TABLE IF EXISTS finance_stocktake_history;
-- 6. DROP TABLE IF EXISTS finance_stocktake_item;
-- 7. DROP TABLE IF EXISTS finance_stocktake;
-- 注意：已过账的盘点数据不得回滚，需先冲销。
