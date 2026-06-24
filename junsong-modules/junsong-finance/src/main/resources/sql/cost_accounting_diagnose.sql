-- =====================================================
-- 成本核算诊断脚本
-- 请在数据库管理工具中执行以下SQL，查看结果
-- =====================================================

-- 1. 检查 fin_expense 表结构和数据
SELECT '=== 1. fin_expense 表数据 ===' AS info;
SELECT expense_id, expense_date, expense_amount, expense_type, status, del_flag 
FROM fin_expense 
WHERE del_flag = '0'
LIMIT 10;

-- 2. 检查费用表按日期统计
SELECT '=== 2. 按日期统计费用 ===' AS info;
SELECT 
    expense_date,
    SUM(expense_amount) as total_amount,
    COUNT(*) as record_count
FROM fin_expense 
WHERE del_flag = '0'
GROUP BY expense_date
ORDER BY expense_date DESC
LIMIT 10;

-- 3. 检查投资人付款表
SELECT '=== 3. fin_investor_payment 表数据 ===' AS info;
SELECT payment_id, payment_date, payment_type, amount, investor_name, del_flag
FROM fin_investor_payment 
WHERE del_flag = '0'
LIMIT 10;

-- 4. 检查投资人付款类型分布
SELECT '=== 4. 投资人付款类型分布 ===' AS info;
SELECT payment_type, SUM(amount) as total_amount, COUNT(*) as record_count
FROM fin_investor_payment 
WHERE del_flag = '0'
GROUP BY payment_type;

-- 5. 检查未核销费用数量
SELECT '=== 5. 未核销费用数量 ===' AS info;
SELECT COUNT(*) as unverified_count
FROM fin_expense 
WHERE status = '0' AND del_flag = '0';

-- 6. 检查当前借支（未核销借支）
SELECT '=== 6. 当前借支（未核销） ===' AS info;
SELECT COUNT(*) as unverified_advance_count, SUM(advance_amount) as total_unverified_advance
FROM fin_advance 
WHERE status = '0' AND del_flag = '0';

-- 7. 测试按日期范围查询费用
SELECT '=== 7. 测试按日期范围查询费用（请根据实际日期调整）===' AS info;
SELECT ifnull(SUM(expense_amount), 0) as total_expense
FROM fin_expense 
WHERE del_flag = '0' 
AND expense_date BETWEEN '2025-01-01' AND '2025-12-31';

-- 8. 测试按日期范围查询投资人打款
SELECT '=== 8. 测试按日期范围查询投资人打款 ===' AS info;
SELECT ifnull(SUM(amount), 0) as total_invest
FROM fin_investor_payment 
WHERE payment_type = '打款' 
AND del_flag = '0' 
AND payment_date BETWEEN '2025-01-01' AND '2025-12-31';

-- 9. 检查 fin_cost_accounting 表结构
SELECT '=== 9. fin_cost_accounting 表字段 ===' AS info;
DESC fin_cost_accounting;

-- 10. 检查现有成本核算记录
SELECT '=== 10. 现有成本核算记录 ===' AS info;
SELECT accounting_id, accounting_no, start_date, end_date, 
       total_expense, total_purchase, total_invest, current_advance, return_situation
FROM fin_cost_accounting 
WHERE del_flag = '0';
