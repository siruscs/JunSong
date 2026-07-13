SET NAMES utf8mb4;

-- =====================================================================
-- 盛和里 46245416（凝胶）历史销售出库流水补录
--
-- 背景：
--   PROD 中 sys_dept.dept_id=202（盛和里）、fin_product.product_code=46245416
--   对应 product_id=1。该商品存在采购入库流水 +360，但两条已完成销售单
--   SX202606170000(185) / SX202606300000(144) 缺少 SALE_OUT 库存流水，
--   导致 fin_stock_position.quantity 仍为 360，实际应为 31。
--
-- 安全性：
--   1. 仅处理 tenant_id=1、dept_id=202、product_id=1、status='2' 的销售单。
--   2. 仅补缺少 SALE_OUT 流水的销售单；重复执行不会重复插入。
--   3. 先锁定 fin_stock_position 目标行，再基于缺失销售数量更新结存。
--   4. 输出补录前后对账结果，不删除、不覆盖历史销售单。
-- =====================================================================

START TRANSACTION;

SELECT quantity
INTO @before_position_quantity
FROM fin_stock_position
WHERE tenant_id = 1
  AND dept_id = 202
  AND product_id = 1
FOR UPDATE;

CREATE TEMPORARY TABLE tmp_shengheli_46245416_missing_sales AS
SELECT
    s.sale_id,
    s.sale_no,
    s.total_quantity,
    s.sale_date,
    s.create_by
FROM fin_sale_record s
WHERE s.tenant_id = 1
  AND s.dept_id = 202
  AND s.product_id = 1
  AND s.status = '2'
  AND s.del_flag = '0'
  AND NOT EXISTS (
      SELECT 1
      FROM fin_stock_ledger l
      WHERE l.tenant_id = s.tenant_id
        AND l.dept_id = s.dept_id
        AND l.product_id = s.product_id
        AND l.reference_type = 'SALE'
        AND l.reference_id = s.sale_id
        AND l.change_type = 'SALE_OUT'
        AND l.del_flag = '0'
  )
ORDER BY s.sale_id;

SELECT COUNT(*), COALESCE(SUM(total_quantity), 0)
INTO @missing_sale_count, @missing_sale_quantity
FROM tmp_shengheli_46245416_missing_sales;

SET @running_quantity := @before_position_quantity;

INSERT INTO fin_stock_ledger (
    tenant_id,
    dept_id,
    product_id,
    product_name,
    change_type,
    change_quantity,
    before_quantity,
    after_quantity,
    unit_cost,
    reference_type,
    reference_id,
    reference_no,
    remark,
    create_by,
    create_time,
    del_flag
)
SELECT
    1 AS tenant_id,
    202 AS dept_id,
    1 AS product_id,
    '凝胶' AS product_name,
    'SALE_OUT' AS change_type,
    -m.total_quantity AS change_quantity,
    @running_quantity AS before_quantity,
    (@running_quantity := @running_quantity - m.total_quantity) AS after_quantity,
    NULL AS unit_cost,
    'SALE' AS reference_type,
    m.sale_id AS reference_id,
    m.sale_no AS reference_no,
    CONCAT('历史销售出库流水补录；原销售日期=', DATE_FORMAT(m.sale_date, '%Y-%m-%d')) AS remark,
    'stock-backfill' AS create_by,
    NOW() AS create_time,
    '0' AS del_flag
FROM tmp_shengheli_46245416_missing_sales m
ORDER BY m.sale_id;

SELECT ROW_COUNT()
INTO @inserted_ledger_count;

UPDATE fin_stock_position
SET quantity = quantity - @missing_sale_quantity,
    update_time = NOW()
WHERE tenant_id = 1
  AND dept_id = 202
  AND product_id = 1
  AND @missing_sale_count > 0;

SELECT ROW_COUNT()
INTO @updated_position_count;

COMMIT;

SELECT
    'backfill_result' AS section,
    @before_position_quantity AS before_position_quantity,
    @missing_sale_count AS missing_sale_count,
    @missing_sale_quantity AS missing_sale_quantity,
    @inserted_ledger_count AS inserted_ledger_count,
    @updated_position_count AS updated_position_count;

SELECT
    'position_after' AS section,
    tenant_id,
    dept_id,
    product_id,
    quantity,
    update_time
FROM fin_stock_position
WHERE tenant_id = 1
  AND dept_id = 202
  AND product_id = 1;

SELECT
    'ledger_after' AS section,
    reference_type,
    change_type,
    COUNT(*) AS ledger_count,
    SUM(change_quantity) AS ledger_quantity
FROM fin_stock_ledger
WHERE tenant_id = 1
  AND dept_id = 202
  AND product_id = 1
  AND del_flag = '0'
GROUP BY reference_type, change_type
ORDER BY reference_type, change_type;

SELECT
    'still_missing_sales' AS section,
    s.sale_id,
    s.sale_no,
    s.total_quantity
FROM fin_sale_record s
LEFT JOIN fin_stock_ledger l
  ON l.tenant_id = s.tenant_id
 AND l.dept_id = s.dept_id
 AND l.product_id = s.product_id
 AND l.reference_type = 'SALE'
 AND l.reference_id = s.sale_id
 AND l.change_type = 'SALE_OUT'
 AND l.del_flag = '0'
WHERE s.tenant_id = 1
  AND s.dept_id = 202
  AND s.product_id = 1
  AND s.status = '2'
  AND s.del_flag = '0'
  AND l.ledger_id IS NULL;
