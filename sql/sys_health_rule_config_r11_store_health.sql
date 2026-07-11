-- R11 门店健康分 V2 默认规则。只插入规则配置，不创建新权限。
-- Idempotent: safe to run multiple times

INSERT INTO sys_health_rule_config
(rule_code, rule_name, rule_domain, metric_key, compare_op, threshold_value, severity, enabled, notify_enabled, suggestion, sort_order, create_by, create_time)
SELECT 'STORE_PROFIT_RATE_LOW', '门店利润率偏低', 'STORE', 'operatingProfitRate', 'LT', 5, 'HIGH', '1', '1', '利润率低于5%，建议复核售价、折扣、费用和进货成本。', 110, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_health_rule_config WHERE rule_code = 'STORE_PROFIT_RATE_LOW');

INSERT INTO sys_health_rule_config
(rule_code, rule_name, rule_domain, metric_key, compare_op, threshold_value, severity, enabled, notify_enabled, suggestion, sort_order, create_by, create_time)
SELECT 'STORE_SALES_DROP_RATE', '门店销售下滑', 'STORE', 'salesChangeRate', 'LT', -20, 'HIGH', '1', '1', '销售较上期下降超过20%，建议复盘客流、活动和会员触达。', 120, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_health_rule_config WHERE rule_code = 'STORE_SALES_DROP_RATE');

INSERT INTO sys_health_rule_config
(rule_code, rule_name, rule_domain, metric_key, compare_op, threshold_value, severity, enabled, notify_enabled, suggestion, sort_order, create_by, create_time)
SELECT 'STORE_EXPENSE_RATE_HIGH', '门店费用率偏高', 'STORE', 'expenseRate', 'GT', 35, 'MEDIUM', '1', '0', '费用率超过35%，建议查看费用分类和未核销费用。', 130, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_health_rule_config WHERE rule_code = 'STORE_EXPENSE_RATE_HIGH');

INSERT INTO sys_health_rule_config
(rule_code, rule_name, rule_domain, metric_key, compare_op, threshold_value, severity, enabled, notify_enabled, suggestion, sort_order, create_by, create_time)
SELECT 'STORE_PENDING_AMOUNT_HIGH', '门店未核销金额偏高', 'STORE', 'pendingAmount', 'GT', 1000, 'MEDIUM', '1', '0', '未核销金额偏高，建议优先处理费用和借支核销。', 140, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_health_rule_config WHERE rule_code = 'STORE_PENDING_AMOUNT_HIGH');

INSERT INTO sys_health_rule_config
(rule_code, rule_name, rule_domain, metric_key, compare_op, threshold_value, severity, enabled, notify_enabled, suggestion, sort_order, create_by, create_time)
SELECT 'STORE_REVIEW_SCORE_LOW', '门店复盘质量偏低', 'STORE', 'reviewQualityScore', 'LT', 70, 'MEDIUM', '1', '0', '复盘质量分低于70，建议及时响应并补充处理说明。', 150, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_health_rule_config WHERE rule_code = 'STORE_REVIEW_SCORE_LOW');
