SET NAMES utf8mb4;

-- ==========================================================================
-- Task 10: 库存成本层表结构与幂等迁移
-- 目标：为移动加权成本计算建立独立的成本层与成本流水表。
-- 原则：非破坏、可重复执行；不修改第一期 fin_stock_ledger /
--       fin_stock_position / fin_stock_snapshot 的表结构。
-- 成本层按 tenant + dept + product 唯一，记录移动加权平均成本。
-- 成本流水记录每一笔成本变动（采购入库 / 销售出库 / 采购冲销 / 销售冲销 / 调整），
-- 关联原库存流水，固化出库瞬间的成本，禁止用当前采购价回算历史成本。
-- ==========================================================================

-- Step 1: 幂等创建成本层表（移动加权平均成本当前值）
CREATE TABLE IF NOT EXISTS fin_stock_cost_layer (
  cost_layer_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '成本层ID',
  tenant_id        BIGINT       NOT NULL                COMMENT '租户ID',
  dept_id          BIGINT       NOT NULL                COMMENT '门店ID',
  product_id       BIGINT       NOT NULL                COMMENT '商品ID',
  avg_unit_cost    DECIMAL(18,6) NOT NULL DEFAULT 0.000000 COMMENT '移动加权平均单位成本（6位小数）',
  stock_quantity   INT          NOT NULL DEFAULT 0      COMMENT '成本层记录的库存数量（与 position 对账）',
  stock_amount     DECIMAL(18,2) NOT NULL DEFAULT 0.00  COMMENT '库存金额（2位小数）',
  version          INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  create_by        VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by        VARCHAR(64)  DEFAULT ''              COMMENT '更新者',
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (cost_layer_id),
  UNIQUE KEY uk_stock_cost_layer_tenant_dept_product (tenant_id, dept_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存成本层（移动加权平均成本）';

-- Step 2: 幂等创建成本流水表（每一笔成本变动的不可追溯流水）
CREATE TABLE IF NOT EXISTS fin_stock_cost_ledger (
  cost_ledger_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '成本流水ID',
  tenant_id        BIGINT       NOT NULL                COMMENT '租户ID',
  dept_id          BIGINT       NOT NULL                COMMENT '门店ID',
  product_id       BIGINT       NOT NULL                COMMENT '商品ID',
  source_type      VARCHAR(32)  NOT NULL                COMMENT '来源类型: PURCHASE / SALE / ADJUST',
  source_ledger_id BIGINT       DEFAULT NULL            COMMENT '关联的库存流水ID（fin_stock_ledger.ledger_id）',
  cost_change_type VARCHAR(32)  NOT NULL                COMMENT '成本变动类型: COST_IN / COST_OUT / COST_REVERSE_IN / COST_REVERSE_OUT / COST_ADJUST',
  quantity         INT          NOT NULL DEFAULT 0      COMMENT '变动数量（正增负减）',
  unit_cost        DECIMAL(18,6) NOT NULL DEFAULT 0.000000 COMMENT '单位成本（6位小数）',
  amount           DECIMAL(18,2) NOT NULL DEFAULT 0.00  COMMENT '金额（2位小数）',
  period_id        BIGINT       DEFAULT NULL            COMMENT '会计期间ID',
  adjust_reason    VARCHAR(256) DEFAULT NULL            COMMENT '调整原因（仅 COST_ADJUST 必填）',
  operator         VARCHAR(64)  DEFAULT NULL            COMMENT '操作者',
  del_flag         CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 2删除）',
  create_by        VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (cost_ledger_id),
  KEY idx_stock_cost_ledger_tenant_dept_product (tenant_id, dept_id, product_id, create_time),
  KEY idx_stock_cost_ledger_source (tenant_id, source_type, source_ledger_id),
  KEY idx_stock_cost_ledger_period (tenant_id, dept_id, period_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存成本流水（每一笔成本变动）';

-- Step 3: 对账输出 —— 成本层孤行（有成本层无库存结存）
-- 用于检测成本层与 fin_stock_position 的数量一致性
SELECT
  'cost_layer_without_position' AS reconciliation_type,
  COUNT(*) AS mismatch_count
FROM fin_stock_cost_layer cl
LEFT JOIN fin_stock_position pos
  ON pos.tenant_id = cl.tenant_id
  AND pos.dept_id = cl.dept_id
  AND pos.product_id = cl.product_id
WHERE pos.position_id IS NULL;

-- Step 4: 对账输出 —— 结存无成本层（有库存结存无成本层）
SELECT
  'position_without_cost_layer' AS reconciliation_type,
  COUNT(*) AS mismatch_count
FROM fin_stock_position pos
LEFT JOIN fin_stock_cost_layer cl
  ON cl.tenant_id = pos.tenant_id
  AND cl.dept_id = pos.dept_id
  AND cl.product_id = pos.product_id
WHERE cl.cost_layer_id IS NULL
  AND pos.quantity <> 0;

-- Step 5: 对账输出 —— 成本层数量与结存数量不一致
SELECT
  'cost_layer_quantity_mismatch' AS reconciliation_type,
  COUNT(*) AS mismatch_count
FROM fin_stock_cost_layer cl
JOIN fin_stock_position pos
  ON pos.tenant_id = cl.tenant_id
  AND pos.dept_id = cl.dept_id
  AND pos.product_id = cl.product_id
WHERE cl.stock_quantity <> pos.quantity;
