SET NAMES utf8mb4;

-- =====================================================================
-- 历史库存成本层初始化：从 fin_stock_ledger 重建移动加权成本
--
-- 背景：
--   PROD 已有库存数量流水，但 fin_stock_cost_layer / fin_stock_cost_ledger 为空，
--   导致价值报表 costReady=false。该脚本按历史库存流水顺序重放成本计算。
--
-- 口径：
--   1. 采购入库 COST_IN：金额取采购明细 amount，若同一采购商品拆成多条
--      PURCHASE_IN 流水，则按流水数量占比分摊采购金额。
--   2. 采购赠品：库存流水数量包含赠品，采购金额不含赠品，天然摊薄平均成本。
--   3. 销售出库 COST_OUT：按出库瞬间移动平均成本固化，并回填
--      fin_stock_ledger.unit_cost。
--   4. 销售冲销 COST_REVERSE_IN：按原 SALE_OUT 固化成本回补。
--   5. 历史负库存/先销售后采购：按现有历史顺序重放，不篡改业务流水。
--
-- 安全性：
--   - 仅当成本层和成本流水均为空时执行，否则 SIGNAL 中止。
--   - 不删除、不修改库存数量流水，只补成本层、成本流水和 SALE_OUT unit_cost。
-- =====================================================================

DROP PROCEDURE IF EXISTS rebuild_stock_cost_from_ledger;

DELIMITER $$

CREATE PROCEDURE rebuild_stock_cost_from_ledger()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_ledger_id BIGINT;
    DECLARE v_tenant_id BIGINT;
    DECLARE v_dept_id BIGINT;
    DECLARE v_product_id BIGINT;
    DECLARE v_change_type VARCHAR(32);
    DECLARE v_change_quantity INT;
    DECLARE v_reference_type VARCHAR(64);
    DECLARE v_reference_id BIGINT;
    DECLARE v_create_by VARCHAR(64);
    DECLARE v_create_time DATETIME;

    DECLARE v_layer_id BIGINT;
    DECLARE v_qty INT;
    DECLARE v_amount DECIMAL(18,2);
    DECLARE v_avg DECIMAL(18,6);
    DECLARE v_delta_qty INT;
    DECLARE v_cost_amount DECIMAL(18,2);
    DECLARE v_unit_cost DECIMAL(18,6);
    DECLARE v_source_type VARCHAR(32);
    DECLARE v_cost_change_type VARCHAR(32);

    DECLARE ledger_cursor CURSOR FOR
        SELECT ledger_id, tenant_id, dept_id, product_id, change_type, change_quantity,
               reference_type, reference_id, COALESCE(create_by, 'cost-rebuild'), create_time
        FROM fin_stock_ledger
        WHERE del_flag = '0'
          AND change_type IN ('PURCHASE_IN', 'PURCHASE_REVERSE', 'SALE_OUT', 'SALE_REVERSE')
        ORDER BY create_time, ledger_id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    IF (SELECT COUNT(*) FROM fin_stock_cost_layer) > 0
       OR (SELECT COUNT(*) FROM fin_stock_cost_ledger) > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '成本层或成本流水已存在，拒绝重复重建';
    END IF;

    CREATE TEMPORARY TABLE tmp_purchase_amount_by_ledger AS
    SELECT
        l.ledger_id,
        ROUND(COALESCE(pa.purchase_amount, 0)
            * l.change_quantity
            / NULLIF(pt.total_purchase_qty, 0), 2) AS allocated_amount
    FROM fin_stock_ledger l
    JOIN (
        SELECT tenant_id, reference_id, product_id, SUM(change_quantity) AS total_purchase_qty
        FROM fin_stock_ledger
        WHERE del_flag = '0'
          AND reference_type = 'PURCHASE'
          AND change_type = 'PURCHASE_IN'
        GROUP BY tenant_id, reference_id, product_id
    ) pt
      ON pt.tenant_id = l.tenant_id
     AND pt.reference_id = l.reference_id
     AND pt.product_id = l.product_id
    LEFT JOIN (
        SELECT tenant_id, purchase_id, product_id, SUM(amount) AS purchase_amount
        FROM fin_purchase_detail
        WHERE del_flag = '0'
        GROUP BY tenant_id, purchase_id, product_id
    ) pa
      ON pa.tenant_id = l.tenant_id
     AND pa.purchase_id = l.reference_id
     AND pa.product_id = l.product_id
    WHERE l.del_flag = '0'
      AND l.reference_type = 'PURCHASE'
      AND l.change_type = 'PURCHASE_IN';

    OPEN ledger_cursor;

    read_loop: LOOP
        FETCH ledger_cursor INTO v_ledger_id, v_tenant_id, v_dept_id, v_product_id,
            v_change_type, v_change_quantity, v_reference_type, v_reference_id,
            v_create_by, v_create_time;
        IF done = 1 THEN
            LEAVE read_loop;
        END IF;

        INSERT INTO fin_stock_cost_layer
            (tenant_id, dept_id, product_id, avg_unit_cost, stock_quantity,
             stock_amount, version, create_by, create_time, update_by, update_time)
        VALUES
            (v_tenant_id, v_dept_id, v_product_id, 0.000000, 0,
             0.00, 0, 'cost-rebuild', v_create_time, 'cost-rebuild', v_create_time)
        ON DUPLICATE KEY UPDATE cost_layer_id = LAST_INSERT_ID(cost_layer_id);

        SELECT cost_layer_id, stock_quantity, stock_amount, avg_unit_cost
        INTO v_layer_id, v_qty, v_amount, v_avg
        FROM fin_stock_cost_layer
        WHERE tenant_id = v_tenant_id
          AND dept_id = v_dept_id
          AND product_id = v_product_id
        FOR UPDATE;

        SET v_delta_qty = v_change_quantity;
        SET v_source_type = CASE
            WHEN v_reference_type = 'PURCHASE' THEN 'PURCHASE'
            WHEN v_reference_type = 'SALE' THEN 'SALE'
            ELSE v_reference_type
        END;

        IF v_change_type = 'PURCHASE_IN' THEN
            SET v_cost_change_type = 'COST_IN';
            SELECT COALESCE(allocated_amount, 0.00)
            INTO v_cost_amount
            FROM tmp_purchase_amount_by_ledger
            WHERE ledger_id = v_ledger_id;
            SET v_unit_cost = CASE
                WHEN (v_qty + v_delta_qty) = 0 THEN 0.000000
                ELSE ROUND((v_amount + v_cost_amount) / (v_qty + v_delta_qty), 6)
            END;
            SET v_qty = v_qty + v_delta_qty;
            SET v_amount = ROUND(v_amount + v_cost_amount, 2);
            SET v_avg = v_unit_cost;

        ELSEIF v_change_type = 'PURCHASE_REVERSE' THEN
            SET v_cost_change_type = 'COST_REVERSE_OUT';
            SET v_delta_qty = v_change_quantity;
            SET v_unit_cost = v_avg;
            SET v_cost_amount = ROUND(v_avg * ABS(v_delta_qty), 2);
            SET v_qty = v_qty + v_delta_qty;
            SET v_amount = ROUND(v_amount - v_cost_amount, 2);
            IF v_amount < 0 THEN SET v_amount = 0.00; END IF;
            SET v_avg = CASE WHEN v_qty = 0 THEN 0.000000 ELSE ROUND(v_amount / v_qty, 6) END;

        ELSEIF v_change_type = 'SALE_OUT' THEN
            SET v_cost_change_type = 'COST_OUT';
            SET v_delta_qty = v_change_quantity;
            SET v_unit_cost = v_avg;
            SET v_cost_amount = ROUND(v_unit_cost * ABS(v_delta_qty), 2);
            SET v_qty = v_qty + v_delta_qty;
            SET v_amount = ROUND(v_amount - v_cost_amount, 2);
            SET v_avg = v_unit_cost;

            UPDATE fin_stock_ledger
            SET unit_cost = v_unit_cost
            WHERE ledger_id = v_ledger_id
              AND unit_cost IS NULL;

        ELSEIF v_change_type = 'SALE_REVERSE' THEN
            SET v_cost_change_type = 'COST_REVERSE_IN';
            SET v_delta_qty = v_change_quantity;

            SELECT COALESCE((
                SELECT unit_cost
                FROM fin_stock_ledger
                WHERE tenant_id = v_tenant_id
                  AND reference_type = 'SALE'
                  AND reference_id = v_reference_id
                  AND product_id = v_product_id
                  AND change_type = 'SALE_OUT'
                  AND del_flag = '0'
                  AND ledger_id < v_ledger_id
                ORDER BY ledger_id DESC
                LIMIT 1
            ), v_avg)
            INTO v_unit_cost;

            SET v_cost_amount = ROUND(v_unit_cost * v_delta_qty, 2);
            SET v_qty = v_qty + v_delta_qty;
            SET v_amount = ROUND(v_amount + v_cost_amount, 2);
            SET v_avg = CASE WHEN v_qty = 0 THEN 0.000000 ELSE ROUND(v_amount / v_qty, 6) END;
        END IF;

        UPDATE fin_stock_cost_layer
        SET stock_quantity = v_qty,
            stock_amount = v_amount,
            avg_unit_cost = v_avg,
            version = version + 1,
            update_by = 'cost-rebuild',
            update_time = v_create_time
        WHERE cost_layer_id = v_layer_id;

        INSERT INTO fin_stock_cost_ledger (
            tenant_id, dept_id, product_id, source_type, source_ledger_id,
            cost_change_type, quantity, unit_cost, amount, period_id,
            adjust_reason, operator, del_flag, create_by, create_time
        ) VALUES (
            v_tenant_id, v_dept_id, v_product_id, v_source_type, v_ledger_id,
            v_cost_change_type, v_delta_qty, v_unit_cost, ABS(v_cost_amount), NULL,
            '历史库存成本层初始化', v_create_by, '0', 'cost-rebuild', v_create_time
        );
    END LOOP;

    CLOSE ledger_cursor;
END$$

DELIMITER ;

CALL rebuild_stock_cost_from_ledger();

DROP PROCEDURE rebuild_stock_cost_from_ledger;

SELECT
    'cost_rebuild_result' AS section,
    (SELECT COUNT(*) FROM fin_stock_cost_layer) AS cost_layer_count,
    (SELECT COUNT(*) FROM fin_stock_cost_ledger WHERE del_flag = '0') AS cost_ledger_count,
    (SELECT COUNT(*) FROM fin_stock_ledger
      WHERE del_flag = '0'
        AND change_type = 'SALE_OUT'
        AND unit_cost IS NULL) AS sale_out_without_unit_cost;

SELECT
    'cost_layer_quantity_mismatch' AS section,
    COUNT(*) AS mismatch_count
FROM fin_stock_cost_layer cl
JOIN fin_stock_position pos
  ON pos.tenant_id = cl.tenant_id
 AND pos.dept_id = cl.dept_id
 AND pos.product_id = cl.product_id
WHERE cl.stock_quantity <> pos.quantity;

SELECT
    'stock_scope_without_cost_layer' AS section,
    COUNT(*) AS missing_count
FROM (
    SELECT DISTINCT tenant_id, dept_id, product_id
    FROM fin_stock_ledger
    WHERE del_flag = '0'
) l
LEFT JOIN fin_stock_cost_layer cl
  ON cl.tenant_id = l.tenant_id
 AND cl.dept_id = l.dept_id
 AND cl.product_id = l.product_id
WHERE cl.cost_layer_id IS NULL;

SELECT
    'negative_cost_layer_quantity' AS section,
    cl.tenant_id,
    cl.dept_id,
    d.dept_name,
    cl.product_id,
    p.product_code,
    p.product_name,
    cl.stock_quantity,
    cl.avg_unit_cost,
    cl.stock_amount
FROM fin_stock_cost_layer cl
LEFT JOIN sys_dept d ON d.dept_id = cl.dept_id
LEFT JOIN fin_product p ON p.product_id = cl.product_id
WHERE cl.stock_quantity < 0
ORDER BY cl.dept_id, cl.product_id;
