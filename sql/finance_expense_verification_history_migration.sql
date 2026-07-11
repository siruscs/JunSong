-- 历史费用核销安全回填（可重复执行）
-- 旧数据没有可信核销批次来源：每笔已核销费用只能建立独立、不可反核销的 LEGACY 批次。
-- advance_id 仅作为费用当时字段快照保存，绝不据此创建借支核销关系。

SELECT 'BEFORE_VERIFIED_EXPENSE_COUNT' AS reconciliation_item,
       e.tenant_id, e.dept_id, COUNT(*) AS reconciliation_value
FROM fin_expense e
WHERE e.status = '1' AND e.del_flag = '0'
GROUP BY e.tenant_id, e.dept_id;

SELECT 'BEFORE_VERIFIED_EXPENSE_AMOUNT' AS reconciliation_item,
       e.tenant_id, e.dept_id, COALESCE(SUM(e.expense_amount), 0.00) AS reconciliation_value
FROM fin_expense e
WHERE e.status = '1' AND e.del_flag = '0'
GROUP BY e.tenant_id, e.dept_id;

SELECT 'BEFORE_VERIFY_BATCH_COUNT' AS reconciliation_item,
       b.tenant_id, b.dept_id, COUNT(*) AS reconciliation_value
FROM fin_expense_verify_batch b
GROUP BY b.tenant_id, b.dept_id;

SELECT 'BEFORE_VERIFY_DETAIL_COUNT' AS reconciliation_item,
       d.tenant_id, d.dept_id, COUNT(*) AS reconciliation_value
FROM fin_expense_verify_detail d
GROUP BY d.tenant_id, d.dept_id;

SELECT 'BEFORE_UNVERIFIED_ADVANCE_AMOUNT' AS reconciliation_item,
       a.tenant_id, a.dept_id, COALESCE(SUM(a.advance_amount), 0.00) AS reconciliation_value
FROM fin_advance a
WHERE a.status = '0' AND a.del_flag = '0'
GROUP BY a.tenant_id, a.dept_id;

SELECT 'BEFORE_ACTIVE_ADVANCE_AMOUNT' AS reconciliation_item,
       a.tenant_id, a.dept_id, COALESCE(SUM(a.advance_amount), 0.00) AS reconciliation_value
FROM fin_advance a
WHERE a.del_flag = '0'
GROUP BY a.tenant_id, a.dept_id;

START TRANSACTION;

CREATE TEMPORARY TABLE tmp_expense_history_reconciliation AS
SELECT scope.tenant_id, scope.dept_id,
       (SELECT COUNT(*) FROM fin_expense e WHERE e.tenant_id = scope.tenant_id AND e.dept_id = scope.dept_id AND e.status = '1' AND e.del_flag = '0') AS before_verified_count,
       (SELECT COALESCE(SUM(e.expense_amount), 0.00) FROM fin_expense e WHERE e.tenant_id = scope.tenant_id AND e.dept_id = scope.dept_id AND e.status = '1' AND e.del_flag = '0') AS before_verified_amount,
       (SELECT COUNT(*) FROM fin_expense_verify_batch b WHERE b.tenant_id = scope.tenant_id AND b.dept_id = scope.dept_id) AS before_verify_batch_count,
       (SELECT COUNT(*) FROM fin_expense_verify_detail d WHERE d.tenant_id = scope.tenant_id AND d.dept_id = scope.dept_id) AS before_verify_detail_count,
       (SELECT COALESCE(SUM(a.advance_amount), 0.00) FROM fin_advance a WHERE a.tenant_id = scope.tenant_id AND a.dept_id = scope.dept_id AND a.status = '0' AND a.del_flag = '0') AS before_unverified_advance_amount,
       (SELECT COALESCE(SUM(a.advance_amount), 0.00) FROM fin_advance a WHERE a.tenant_id = scope.tenant_id AND a.dept_id = scope.dept_id AND a.del_flag = '0') AS before_active_advance_amount
FROM (
  SELECT tenant_id, dept_id FROM fin_expense WHERE del_flag = '0'
  UNION
  SELECT tenant_id, dept_id FROM fin_advance WHERE del_flag = '0'
) scope;

INSERT INTO fin_expense_verify_batch
  (batch_no, request_id, tenant_id, dept_id, total_expense_amount,
   total_advance_amount, difference_amount, status, source_type,
   verify_by, verify_time, version, create_time, update_time)
SELECT CONCAT('HXL-', LEFT(SHA2(CONCAT_WS(':', e.tenant_id, e.dept_id, e.expense_id), 256), 48)),
       CONCAT('HXR-', LEFT(SHA2(CONCAT_WS(':', e.tenant_id, e.dept_id, e.expense_id), 256), 48)),
       e.tenant_id, e.dept_id, e.expense_amount, 0.00, e.expense_amount,
       'VERIFIED', 'LEGACY',
       COALESCE(NULLIF(e.verify_by, ''), 'history-migration'),
       COALESCE(e.verify_time, e.update_time, e.create_time, NOW()),
       0, NOW(), NOW()
FROM fin_expense e
WHERE e.status = '1'
  AND e.del_flag = '0'
  AND NOT EXISTS (
    SELECT 1
    FROM fin_expense_verify_batch existing_batch
    WHERE existing_batch.tenant_id = e.tenant_id
      AND existing_batch.request_id = CONCAT('HXR-', LEFT(SHA2(CONCAT_WS(':', e.tenant_id, e.dept_id, e.expense_id), 256), 48))
  );

-- 只有完全匹配确定性身份、租户、部门、来源和状态的批次，才允许补写费用明细。
-- 若确定性 request_id 被错误数据占用，JOIN 不成立，费用会保留在末尾异常清单中。
INSERT INTO fin_expense_verify_detail
  (batch_id, expense_id, tenant_id, dept_id, expense_amount,
   original_status, original_advance_id, period_id, create_time)
SELECT b.batch_id, e.expense_id, e.tenant_id, e.dept_id, e.expense_amount,
       '1', e.advance_id, e.period_id, NOW()
FROM fin_expense e
JOIN fin_expense_verify_batch b
  ON b.tenant_id = e.tenant_id
 AND b.dept_id = e.dept_id
 AND b.request_id = CONCAT('HXR-', LEFT(SHA2(CONCAT_WS(':', e.tenant_id, e.dept_id, e.expense_id), 256), 48))
 AND b.batch_no = CONCAT('HXL-', LEFT(SHA2(CONCAT_WS(':', e.tenant_id, e.dept_id, e.expense_id), 256), 48))
 AND b.source_type = 'LEGACY'
 AND b.status = 'VERIFIED'
WHERE e.status = '1'
  AND e.del_flag = '0'
  AND NOT EXISTS (
    SELECT 1
    FROM fin_expense_verify_detail existing_detail
    WHERE existing_detail.batch_id = b.batch_id
      AND existing_detail.expense_id = e.expense_id
  );

COMMIT;

SELECT 'AFTER_VERIFIED_EXPENSE_COUNT' AS reconciliation_item,
       e.tenant_id, e.dept_id, COUNT(*) AS reconciliation_value
FROM fin_expense e
WHERE e.status = '1' AND e.del_flag = '0'
GROUP BY e.tenant_id, e.dept_id;

SELECT 'AFTER_VERIFIED_EXPENSE_AMOUNT' AS reconciliation_item,
       e.tenant_id, e.dept_id, COALESCE(SUM(e.expense_amount), 0.00) AS reconciliation_value
FROM fin_expense e
WHERE e.status = '1' AND e.del_flag = '0'
GROUP BY e.tenant_id, e.dept_id;

SELECT 'AFTER_VERIFY_BATCH_COUNT' AS reconciliation_item,
       b.tenant_id, b.dept_id, COUNT(*) AS reconciliation_value
FROM fin_expense_verify_batch b
GROUP BY b.tenant_id, b.dept_id;

SELECT 'AFTER_VERIFY_DETAIL_COUNT' AS reconciliation_item,
       d.tenant_id, d.dept_id, COUNT(*) AS reconciliation_value
FROM fin_expense_verify_detail d
GROUP BY d.tenant_id, d.dept_id;

SELECT 'AFTER_UNVERIFIED_ADVANCE_AMOUNT' AS reconciliation_item,
       a.tenant_id, a.dept_id, COALESCE(SUM(a.advance_amount), 0.00) AS reconciliation_value
FROM fin_advance a
WHERE a.status = '0' AND a.del_flag = '0'
GROUP BY a.tenant_id, a.dept_id;

SELECT 'AFTER_ACTIVE_ADVANCE_AMOUNT' AS reconciliation_item,
       a.tenant_id, a.dept_id, COALESCE(SUM(a.advance_amount), 0.00) AS reconciliation_value
FROM fin_advance a
WHERE a.del_flag = '0'
GROUP BY a.tenant_id, a.dept_id;

SELECT r.tenant_id, r.dept_id,
       ((SELECT COUNT(*) FROM fin_expense e WHERE e.tenant_id = r.tenant_id AND e.dept_id = r.dept_id AND e.status = '1' AND e.del_flag = '0') - r.before_verified_count) AS verified_count_reconciliation_delta,
       ((SELECT COALESCE(SUM(e.expense_amount), 0.00) FROM fin_expense e WHERE e.tenant_id = r.tenant_id AND e.dept_id = r.dept_id AND e.status = '1' AND e.del_flag = '0') - r.before_verified_amount) AS verified_amount_reconciliation_delta,
       ((SELECT COUNT(*) FROM fin_expense_verify_batch b WHERE b.tenant_id = r.tenant_id AND b.dept_id = r.dept_id) - r.before_verify_batch_count) AS verify_batch_reconciliation_delta,
       ((SELECT COUNT(*) FROM fin_expense_verify_detail d WHERE d.tenant_id = r.tenant_id AND d.dept_id = r.dept_id) - r.before_verify_detail_count) AS verify_detail_reconciliation_delta,
       ((SELECT COALESCE(SUM(a.advance_amount), 0.00) FROM fin_advance a WHERE a.tenant_id = r.tenant_id AND a.dept_id = r.dept_id AND a.status = '0' AND a.del_flag = '0') - r.before_unverified_advance_amount) AS unverified_advance_reconciliation_delta,
       ((SELECT COALESCE(SUM(a.advance_amount), 0.00) FROM fin_advance a WHERE a.tenant_id = r.tenant_id AND a.dept_id = r.dept_id AND a.del_flag = '0') - r.before_active_advance_amount) AS active_advance_reconciliation_delta
FROM tmp_expense_history_reconciliation r;

-- 完整性必须由合法批次与其合法明细共同证明；孤立明细不能掩盖迁移失败。
SELECT 'HISTORY_MIGRATION_EXCEPTION' AS result_type,
       'unresolved_verified_expense' AS exception_type,
       e.tenant_id, e.dept_id, e.expense_id,
       CONCAT('HXR-', LEFT(SHA2(CONCAT_WS(':', e.tenant_id, e.dept_id, e.expense_id), 256), 48)) AS expected_request_id
FROM fin_expense e
WHERE e.status = '1'
  AND e.del_flag = '0'
  AND NOT EXISTS (
    SELECT 1
    FROM fin_expense_verify_batch b
    JOIN fin_expense_verify_detail d
      ON d.batch_id = b.batch_id
     AND d.tenant_id = b.tenant_id
     AND d.dept_id = b.dept_id
     AND d.expense_id = e.expense_id
    WHERE b.tenant_id = e.tenant_id
      AND b.dept_id = e.dept_id
      AND b.request_id = CONCAT('HXR-', LEFT(SHA2(CONCAT_WS(':', e.tenant_id, e.dept_id, e.expense_id), 256), 48))
      AND b.batch_no = CONCAT('HXL-', LEFT(SHA2(CONCAT_WS(':', e.tenant_id, e.dept_id, e.expense_id), 256), 48))
      AND b.source_type = 'LEGACY'
      AND b.status = 'VERIFIED'
  )
UNION ALL
SELECT 'HISTORY_MIGRATION_EXCEPTION', 'reconciliation_mismatch',
       r.tenant_id, r.dept_id, NULL,
       CONCAT('reconciliation_delta:',
         (SELECT COUNT(*) FROM fin_expense e WHERE e.tenant_id = r.tenant_id AND e.dept_id = r.dept_id AND e.status = '1' AND e.del_flag = '0') - r.before_verified_count, ':',
         (SELECT COALESCE(SUM(e.expense_amount), 0.00) FROM fin_expense e WHERE e.tenant_id = r.tenant_id AND e.dept_id = r.dept_id AND e.status = '1' AND e.del_flag = '0') - r.before_verified_amount, ':',
         (SELECT COALESCE(SUM(a.advance_amount), 0.00) FROM fin_advance a WHERE a.tenant_id = r.tenant_id AND a.dept_id = r.dept_id AND a.status = '0' AND a.del_flag = '0') - r.before_unverified_advance_amount, ':',
         (SELECT COALESCE(SUM(a.advance_amount), 0.00) FROM fin_advance a WHERE a.tenant_id = r.tenant_id AND a.dept_id = r.dept_id AND a.del_flag = '0') - r.before_active_advance_amount)
FROM tmp_expense_history_reconciliation r
WHERE (SELECT COUNT(*) FROM fin_expense e WHERE e.tenant_id = r.tenant_id AND e.dept_id = r.dept_id AND e.status = '1' AND e.del_flag = '0') <> r.before_verified_count
   OR (SELECT COALESCE(SUM(e.expense_amount), 0.00) FROM fin_expense e WHERE e.tenant_id = r.tenant_id AND e.dept_id = r.dept_id AND e.status = '1' AND e.del_flag = '0') <> r.before_verified_amount
   OR (SELECT COALESCE(SUM(a.advance_amount), 0.00) FROM fin_advance a WHERE a.tenant_id = r.tenant_id AND a.dept_id = r.dept_id AND a.status = '0' AND a.del_flag = '0') <> r.before_unverified_advance_amount
   OR (SELECT COALESCE(SUM(a.advance_amount), 0.00) FROM fin_advance a WHERE a.tenant_id = r.tenant_id AND a.dept_id = r.dept_id AND a.del_flag = '0') <> r.before_active_advance_amount;

DROP TEMPORARY TABLE tmp_expense_history_reconciliation;
