SET NAMES utf8mb4;

-- =====================================================================
-- 已删除销售单遗留 SALE_OUT 流水操作人回填
--
-- 背景：
--   finance_sale_stock_operator_backfill.sql 只回填有效销售单。PROD 中还有
--   8 条 SALE_OUT 流水关联的销售单已逻辑删除（fin_sale_record.del_flag='2'），
--   但 sys_oper_log 仍能按 saleId 唯一匹配原新增操作人。
--
-- 安全性：
--   1. 仅处理 create_by 为空的 SALE_OUT 流水。
--   2. 仅使用销售新增成功日志，且操作人唯一。
--   3. 不恢复/修改已删除销售单，只补库存流水审计字段。
-- =====================================================================

START TRANSACTION;

CREATE TEMPORARY TABLE tmp_deleted_sale_ledger_operator AS
SELECT
    l.ledger_id,
    MIN(o.oper_name) AS operator_name,
    COUNT(DISTINCT o.oper_name) AS operator_count
FROM fin_stock_ledger l
JOIN fin_sale_record s
  ON s.tenant_id = l.tenant_id
 AND s.sale_id = l.reference_id
JOIN sys_oper_log o
  ON o.tenant_id = l.tenant_id
 AND o.method = 'com.junsong.finance.controller.FinSaleRecordController.add()'
 AND o.status = 0
 AND o.oper_name IS NOT NULL
 AND o.oper_name <> ''
 AND LOCATE(CONCAT('"saleId":', l.reference_id), o.oper_param) > 0
WHERE l.reference_type = 'SALE'
  AND l.change_type = 'SALE_OUT'
  AND l.del_flag = '0'
  AND (l.create_by IS NULL OR l.create_by = '')
  AND s.del_flag = '2'
GROUP BY l.ledger_id
HAVING operator_count = 1;

SELECT COUNT(*)
INTO @matched_ledger_count
FROM tmp_deleted_sale_ledger_operator;

UPDATE fin_stock_ledger l
JOIN tmp_deleted_sale_ledger_operator m
  ON m.ledger_id = l.ledger_id
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
    @matched_ledger_count AS matched_ledger_count,
    @updated_ledger_count AS updated_sale_out_ledger_count;

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
