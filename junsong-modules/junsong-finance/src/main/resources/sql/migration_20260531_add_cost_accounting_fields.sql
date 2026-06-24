-- 成本核算功能完整SQL
-- 执行前请先确认表结构

-- 1. 添加成本核算表的新字段
ALTER TABLE fin_cost_accounting ADD COLUMN total_invest DECIMAL(18, 2) DEFAULT 0 COMMENT '投资来源金额' AFTER total_payment;
ALTER TABLE fin_cost_accounting ADD COLUMN current_advance DECIMAL(18, 2) DEFAULT 0 COMMENT '当前借支金额' AFTER total_invest;
