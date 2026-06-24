-- 付款方式字典数据初始化
-- 执行日期: 2026-06-06

-- 1. 插入字典类型（付款方式）
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
VALUES 
('付款方式', 'finance_payment_method', '0', 'admin', NOW(), '付款方式字典');

-- 2. 插入字典数据（付款方式选项）
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
VALUES 
(1, '现金', '现金', 'finance_payment_method', 'N', '0', 'admin', NOW(), '现金'),
(2, '银行转账', '银行转账', 'finance_payment_method', 'N', '0', 'admin', NOW(), '银行转账'),
(3, '微信支付', '微信支付', 'finance_payment_method', 'N', '0', 'admin', NOW(), '微信支付'),
(4, '支付宝', '支付宝', 'finance_payment_method', 'N', '0', 'admin', NOW(), '支付宝'),
(5, '月结', '月结', 'finance_payment_method', 'N', '0', 'admin', NOW(), '月结'),
(6, '其他', '其他', 'finance_payment_method', 'N', '0', 'admin', NOW(), '其他'),
(7, '直接付款', '直接付款', 'finance_payment_method', 'N', '0', 'admin', NOW(), '直接付款'),
(8, '预支资金', '预支资金', 'finance_payment_method', 'N', '0', 'admin', NOW(), '预支资金'),
(9, '自行垫付', '自行垫付', 'finance_payment_method', 'N', '0', 'admin', NOW(), '自行垫付'),
(10, '收入', '收入', 'finance_payment_method', 'N', '0', 'admin', NOW(), '收入');

