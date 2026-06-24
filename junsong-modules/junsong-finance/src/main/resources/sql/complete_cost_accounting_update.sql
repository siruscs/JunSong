-- =====================================================
-- 成本核算功能 完整数据库更新脚本
-- 请务必按顺序执行以下操作
-- =====================================================

-- 1. 查看 fin_cost_accounting 表当前结构（先确认）
-- DESC fin_cost_accounting;

-- 2. 添加投资来源金额字段（如不存在）
ALTER TABLE fin_cost_accounting ADD COLUMN IF NOT EXISTS total_invest DECIMAL(18, 2) DEFAULT 0 COMMENT '投资来源金额' AFTER total_payment;

-- 3. 添加当前借支金额字段（如不存在）
ALTER TABLE fin_cost_accounting ADD COLUMN IF NOT EXISTS current_advance DECIMAL(18, 2) DEFAULT 0 COMMENT '当前借支金额' AFTER total_invest;

-- 4. 验证字段已存在
-- SELECT * FROM information_schema.COLUMNS WHERE TABLE_NAME='fin_cost_accounting' ORDER BY ORDINAL_POSITION;

-- 5. 测试查询各项数据
-- 测试总花销
-- SELECT ifnull(sum(expense_amount), 0) FROM fin_expense WHERE del_flag = '0';

-- 测试总进货
-- SELECT ifnull(sum(total_amount), 0) FROM fin_purchase WHERE del_flag = '0';

-- 测试总销售
-- SELECT ifnull(sum(sale_amount), 0) FROM fin_sale_record WHERE del_flag = '0';

-- 测试总缴款
-- SELECT ifnull(sum(payment_amount), 0) FROM fin_sale_payment WHERE del_flag = '0';

-- 测试投资来源金额
-- SELECT ifnull(sum(invest_amount), 0) FROM fin_invest_record WHERE del_flag = '0';

-- 测试当前借支
-- SELECT ifnull(sum(advance_amount), 0) FROM fin_advance WHERE status = '0' AND del_flag = '0';

-- 测试未核销费用
-- SELECT count(*) FROM fin_expense WHERE status = '0' AND del_flag = '0';

-- 测试回本检查
-- SELECT exists(select 1 from fin_cost_accounting where return_situation >= 0 and del_flag = '0' limit 1);
