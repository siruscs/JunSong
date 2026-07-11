SET @poolNo = 'CP20260707183719926148';
SET @poolId = (
    SELECT pool_id
    FROM fin_composite_accounting_pool
    WHERE pool_no = @poolNo
      AND del_flag = '0'
    LIMIT 1
);

INSERT INTO fin_composite_period_item (
    pool_id,
    dept_id,
    period_id,
    period_no,
    net_profit,
    manager_profit_amount,
    investor_profit_amount,
    included_mode,
    included_time,
    included_by,
    status,
    del_flag,
    create_by,
    create_time,
    remark
)
SELECT
    p.pool_id,
    ap.dept_id,
    ap.period_id,
    ap.period_no,
    COALESCE(ap.net_profit, 0),
    COALESCE(ap.manager_profit_amount, 0),
    CASE
        WHEN COALESCE(ap.investor_profit_amount, 0) > 0 THEN COALESCE(ap.investor_profit_amount, 0)
        ELSE GREATEST(COALESCE(ap.net_profit, 0) - COALESCE(ap.manager_profit_amount, 0), 0)
    END,
    '0',
    NOW(),
    'system_backfill',
    '0',
    '0',
    'system_backfill',
    NOW(),
    '复合核算池未回本周期补齐'
FROM fin_composite_accounting_pool p
JOIN fin_composite_pool_dept d
    ON d.pool_id = p.pool_id
   AND d.del_flag = '0'
   AND d.status = '0'
JOIN fin_accounting_period ap
    ON ap.dept_id = d.dept_id
   AND ap.del_flag = '0'
LEFT JOIN fin_composite_period_item existing
    ON existing.period_id = ap.period_id
   AND existing.del_flag = '0'
   AND existing.status = '0'
WHERE p.pool_no = @poolNo
  AND p.del_flag = '0'
  AND p.status = '0'
  AND existing.item_id IS NULL;

UPDATE fin_composite_accounting_pool p
LEFT JOIN (
    SELECT
        pool_id,
        COALESCE(SUM(investor_profit_amount), 0) AS total_return
    FROM fin_composite_period_item
    WHERE pool_id = @poolId
      AND del_flag = '0'
      AND status = '0'
    GROUP BY pool_id
) s ON s.pool_id = p.pool_id
SET p.total_return_amount = COALESCE(s.total_return, 0),
    p.break_even_gap = GREATEST(p.total_invest_amount - COALESCE(s.total_return, 0), 0),
    p.over_return_amount = GREATEST(COALESCE(s.total_return, 0) - p.total_invest_amount, 0),
    p.status = CASE
        WHEN COALESCE(s.total_return, 0) >= p.total_invest_amount
             AND p.total_invest_amount > 0
             AND p.status = '0' THEN '1'
        ELSE p.status
    END,
    p.break_even_time = CASE
        WHEN COALESCE(s.total_return, 0) >= p.total_invest_amount
             AND p.total_invest_amount > 0
             AND p.status = '0' THEN NOW()
        WHEN COALESCE(s.total_return, 0) < p.total_invest_amount
             AND p.status = '1' THEN NULL
        ELSE p.break_even_time
    END,
    p.update_by = 'system_backfill',
    p.update_time = NOW()
WHERE p.pool_id = @poolId;

UPDATE fin_composite_pool_investor i
JOIN fin_composite_accounting_pool p
    ON p.pool_id = i.pool_id
SET i.returned_amount = ROUND(COALESCE(p.total_return_amount, 0) * COALESCE(i.invest_ratio, 0), 2),
    i.update_by = 'system_backfill',
    i.update_time = NOW()
WHERE i.pool_id = @poolId
  AND i.del_flag = '0';

SELECT
    p.pool_id,
    p.pool_no,
    p.status,
    p.total_invest_amount,
    p.total_return_amount,
    p.break_even_gap
FROM fin_composite_accounting_pool p
WHERE p.pool_id = @poolId;

SELECT
    item.item_id,
    item.pool_id,
    item.dept_id,
    item.period_id,
    item.period_no,
    item.net_profit,
    item.manager_profit_amount,
    item.investor_profit_amount,
    item.status,
    item.del_flag
FROM fin_composite_period_item item
WHERE item.pool_id = @poolId
ORDER BY item.dept_id, item.period_id;
