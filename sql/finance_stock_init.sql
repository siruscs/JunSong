SET NAMES utf8mb4;

-- ============================================================
-- 期初库存模块 DDL（独立于普通盘点，可重复执行、租户隔离、非破坏）
-- 状态机：DRAFT → VALIDATED → SUBMITTED → APPROVED → POSTED
-- 表：fin_stock_init_batch（批次头）、fin_stock_init_item（批次行）
-- ============================================================

-- ---------- 1. 批次头表 ----------
CREATE TABLE IF NOT EXISTS fin_stock_init_batch (
    batch_id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '批次ID',
    tenant_id       BIGINT       NOT NULL                COMMENT '租户ID',
    batch_no        VARCHAR(64)  NOT NULL                COMMENT '期初批次号（系统生成 SI{yyyyMMdd}{流水}）',
    dept_id         BIGINT       NOT NULL                COMMENT '门店ID',
    init_date       DATE         NOT NULL                COMMENT '期初日期',
    status          VARCHAR(24)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/VALIDATED/SUBMITTED/APPROVED/POSTED',
    submitted_by    VARCHAR(64)           DEFAULT NULL   COMMENT '提交人',
    submitted_time  DATETIME              DEFAULT NULL   COMMENT '提交时间',
    approved_by     VARCHAR(64)           DEFAULT NULL   COMMENT '审批人',
    approved_time   DATETIME              DEFAULT NULL   COMMENT '审批时间',
    posted_by       VARCHAR(64)           DEFAULT NULL   COMMENT '过账人',
    posted_time     DATETIME              DEFAULT NULL   COMMENT '过账时间',
    post_idempotency_key VARCHAR(96)      DEFAULT NULL   COMMENT '过账幂等键（租户内唯一）',
    remark          VARCHAR(500)          DEFAULT NULL   COMMENT '备注',
    version         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    create_by       VARCHAR(64)           DEFAULT NULL   COMMENT '创建人',
    create_time     DATETIME              DEFAULT NULL   COMMENT '创建时间',
    update_by       VARCHAR(64)           DEFAULT NULL   COMMENT '更新人',
    update_time     DATETIME              DEFAULT NULL   COMMENT '更新时间',
    PRIMARY KEY (batch_id),
    UNIQUE KEY uk_stock_init_tenant_no (tenant_id, batch_no),
    UNIQUE KEY uk_stock_init_post_key (tenant_id, post_idempotency_key),
    KEY idx_stock_init_tenant_dept (tenant_id, dept_id, status),
    KEY idx_stock_init_tenant_status (tenant_id, status, init_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='期初库存批次头表';

-- ---------- 2. 批次行表 ----------
CREATE TABLE IF NOT EXISTS fin_stock_init_item (
    item_id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '行ID',
    batch_id        BIGINT       NOT NULL                COMMENT '批次ID',
    tenant_id       BIGINT       NOT NULL                COMMENT '租户ID',
    dept_id         BIGINT       NOT NULL                COMMENT '门店ID',
    product_id      BIGINT       NOT NULL                COMMENT '商品ID',
    product_name    VARCHAR(200)          DEFAULT NULL   COMMENT '商品名称（快照）',
    quantity        DECIMAL(18,2) NOT NULL DEFAULT 0     COMMENT '期初数量',
    unit_cost       DECIMAL(18,6) NOT NULL DEFAULT 0     COMMENT '单位成本（6位小数精度）',
    amount          DECIMAL(18,2) NOT NULL DEFAULT 0     COMMENT '金额 = quantity * unit_cost（2位 HALF_UP）',
    stock_ledger_id BIGINT                DEFAULT NULL   COMMENT '过账生成的库存流水ID',
    cost_ledger_id  BIGINT                DEFAULT NULL   COMMENT '过账生成的成本流水ID',
    version         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    create_by       VARCHAR(64)           DEFAULT NULL   COMMENT '创建人',
    create_time     DATETIME              DEFAULT NULL   COMMENT '创建时间',
    PRIMARY KEY (item_id),
    UNIQUE KEY uk_stock_init_item_batch_product (batch_id, product_id),
    KEY idx_stock_init_item_tenant_dept (tenant_id, dept_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='期初库存批次行表';

-- ---------- 3. 菜单与权限 ----------
-- 查找财务主菜单ID
SELECT @finance_parent_id := menu_id FROM sys_menu
    WHERE menu_name = '财务管理' AND parent_id = 0 AND status = '0' LIMIT 1;

-- 期初库存目录菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '期初库存', @finance_parent_id, 50, 'stockInit', 'finance/stockInit/index', 1, 0,
    'C', '0', '0', 'finance:stockInit:list', 'stock', 'admin', NOW(), '期初库存管理'
FROM DUAL WHERE @finance_parent_id IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '期初库存' AND parent_id = @finance_parent_id);

-- 获取期初库存菜单ID
SELECT @stock_init_menu_id := menu_id FROM sys_menu
    WHERE menu_name = '期初库存' AND parent_id = @finance_parent_id LIMIT 1;

-- 期初库存按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '期初库存查询', @stock_init_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'finance:stockInit:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE @stock_init_menu_id IS NOT NULL AND NOT EXISTS
    (SELECT 1 FROM sys_menu WHERE perms = 'finance:stockInit:query' AND parent_id = @stock_init_menu_id);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '期初库存新增', @stock_init_menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'finance:stockInit:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE @stock_init_menu_id IS NOT NULL AND NOT EXISTS
    (SELECT 1 FROM sys_menu WHERE perms = 'finance:stockInit:add' AND parent_id = @stock_init_menu_id);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '期初库存审批', @stock_init_menu_id, 3, '#', '', 1, 0, 'F', '0', '0', 'finance:stockInit:approve', '#', 'admin', NOW(), ''
FROM DUAL WHERE @stock_init_menu_id IS NOT NULL AND NOT EXISTS
    (SELECT 1 FROM sys_menu WHERE perms = 'finance:stockInit:approve' AND parent_id = @stock_init_menu_id);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '期初库存过账', @stock_init_menu_id, 4, '#', '', 1, 0, 'F', '0', '0', 'finance:stockInit:post', '#', 'admin', NOW(), ''
FROM DUAL WHERE @stock_init_menu_id IS NOT NULL AND NOT EXISTS
    (SELECT 1 FROM sys_menu WHERE perms = 'finance:stockInit:post' AND parent_id = @stock_init_menu_id);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '期初库存导出', @stock_init_menu_id, 5, '#', '', 1, 0, 'F', '0', '0', 'finance:stockInit:export', '#', 'admin', NOW(), ''
FROM DUAL WHERE @stock_init_menu_id IS NOT NULL AND NOT EXISTS
    (SELECT 1 FROM sys_menu WHERE perms = 'finance:stockInit:export' AND parent_id = @stock_init_menu_id);

-- ---------- 4. 角色授权（财务角色） ----------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
    SELECT r.role_id, m.menu_id FROM sys_role r, sys_menu m
    WHERE r.role_key IN ('finance', 'finance_staff', 'finance_manager')
    AND m.perms LIKE 'finance:stockInit:%'
    AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id);

-- ---------- 5. 对账输出 ----------
SELECT 'fin_stock_init_batch 表创建' AS reconciliation_type,
       (SELECT COUNT(*) FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_stock_init_batch') AS expected_1;

SELECT 'fin_stock_init_item 表创建' AS reconciliation_type,
       (SELECT COUNT(*) FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_stock_init_item') AS expected_1;

SELECT '期初库存菜单创建' AS reconciliation_type,
       COUNT(*) AS menu_count FROM sys_menu WHERE perms LIKE 'finance:stockInit:%';

SELECT '期初库存唯一索引核验' AS reconciliation_type,
       COUNT(*) AS unique_index_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME IN ('fin_stock_init_batch', 'fin_stock_init_item')
    AND NON_UNIQUE = 0;

SELECT '期初批次状态分布' AS reconciliation_type, IFNULL(status, 'NULL') AS status, COUNT(*) AS cnt
    FROM fin_stock_init_batch GROUP BY status;

-- ---------- 6. HEX 编码验证 ----------
SELECT menu_id, menu_name, perms, HEX(menu_name) AS menu_name_hex
    FROM sys_menu WHERE perms LIKE 'finance:stockInit:%' ORDER BY menu_id;

-- 回滚说明（不执行）：
-- DROP TABLE IF EXISTS fin_stock_init_item;
-- DROP TABLE IF EXISTS fin_stock_init_batch;
-- DELETE FROM sys_role_menu WHERE menu_id IN (SELECT menu_id FROM sys_menu WHERE perms LIKE 'finance:stockInit:%');
-- DELETE FROM sys_menu WHERE perms LIKE 'finance:stockInit:%';
