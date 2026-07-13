SET NAMES utf8mb4;

-- =====================================================================
-- 历史销售单与销售出库流水操作人回填
--
-- 背景：
--   旧版 FinSaleRecordController 新增销售单时未写入 create_by，导致
--   fin_sale_record.create_by 为空；销售出库流水继承销售单 create_by，
--   因此 fin_stock_ledger 中 SALE_OUT 的 create_by 也为空。
--
-- 修复策略：
--   1. 仅使用 sys_oper_log 中销售新增成功日志
--      com.junsong.finance.controller.FinSaleRecordController.add()。
--   2. 仅回填能按 saleId 唯一匹配且操作人唯一的历史销售单。
--   3. 同步回填这些销售单对应的 SALE_OUT 库存流水操作人。
--   4. 脚本可重复执行；已回填的数据不会被覆盖。
-- =====================================================================

START TRANSACTION;

CREATE TEMPORARY TABLE tmp_sale_operator_backfill AS
SELECT
    s.tenant_id,
    s.sale_id,
    MIN(o.oper_name) AS operator_name,
    COUNT(*) AS log_count,
    COUNT(DISTINCT o.oper_name) AS operator_count
FROM fin_sale_record s
JOIN sys_oper_log o
  ON o.tenant_id = s.tenant_id
 AND o.method = 'com.junsong.finance.controller.FinSaleRecordController.add()'
 AND o.status = 0
 AND o.oper_name IS NOT NULL
 AND o.oper_name <> ''
 AND LOCATE(CONCAT('"saleId":', s.sale_id), o.oper_param) > 0
WHERE s.del_flag = '0'
  AND (s.create_by IS NULL OR s.create_by = '')
GROUP BY s.tenant_id, s.sale_id
HAVING operator_count = 1;

SELECT COUNT(*)
INTO @matched_sale_count
FROM tmp_sale_operator_backfill;

UPDATE fin_sale_record s
JOIN tmp_sale_operator_backfill m
  ON m.tenant_id = s.tenant_id
 AND m.sale_id = s.sale_id
SET s.create_by = m.operator_name
WHERE s.del_flag = '0'
  AND (s.create_by IS NULL OR s.create_by = '');

SELECT ROW_COUNT()
INTO @updated_sale_count;

UPDATE fin_stock_ledger l
JOIN tmp_sale_operator_backfill m
  ON m.tenant_id = l.tenant_id
 AND m.sale_id = l.reference_id
SET l.create_by = m.operator_name
WHERE l.reference_type = 'SALE'
  AND l.change_type = 'SALE_OUT'
  AND l.del_flag = '0'
  AND (l.create_by IS NULL OR l.create_by = '');

SELECT ROW_COUNT()
INTO @updated_ledger_count;

COMMIT;

SELECT
    'backfill_result' AS section,
    @matched_sale_count AS matched_sale_count,
    @updated_sale_count AS updated_sale_count,
    @updated_ledger_count AS updated_sale_out_ledger_count;

SELECT
    'remaining_blank_sales' AS section,
    COUNT(*) AS blank_count
FROM fin_sale_record
WHERE del_flag = '0'
  AND (create_by IS NULL OR create_by = '');

SELECT
    'remaining_blank_sale_out_ledgers' AS section,
    COUNT(*) AS blank_count
FROM fin_stock_ledger
WHERE reference_type = 'SALE'
  AND change_type = 'SALE_OUT'
  AND del_flag = '0'
  AND (create_by IS NULL OR create_by = '');

SELECT
    'sale_out_operator_summary' AS section,
    create_by,
    COUNT(*) AS ledger_count
FROM fin_stock_ledger
WHERE reference_type = 'SALE'
  AND change_type = 'SALE_OUT'
  AND del_flag = '0'
GROUP BY create_by
ORDER BY create_by;
