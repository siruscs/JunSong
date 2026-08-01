SET NAMES utf8mb4;

-- 库存调整类型字典；通过 INSERT IGNORE 保证可重复执行。
INSERT IGNORE INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
VALUES ('库存调整类型', 'finance_stock_adjustment_type', '0', 'admin', NOW(), '库存调整类型');

SET @dict_type_id := (SELECT dict_id FROM sys_dict_type WHERE dict_type = 'finance_stock_adjustment_type' LIMIT 1);

INSERT IGNORE INTO sys_dict_data
    (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES
    (1, '期初库存录入', 'OPENING_STOCK', 'finance_stock_adjustment_type', '', 'success', 'N', '0', 'admin', NOW(), '增加库存'),
    (2, '历史数据补录', 'HISTORY_REPLENISH', 'finance_stock_adjustment_type', '', 'success', 'N', '0', 'admin', NOW(), '增加库存'),
    (3, '试用消耗', 'TRIAL_CONSUMPTION', 'finance_stock_adjustment_type', '', 'warning', 'N', '0', 'admin', NOW(), '减少库存'),
    (4, '店面自用', 'STORE_USE', 'finance_stock_adjustment_type', '', 'warning', 'N', '0', 'admin', NOW(), '减少库存'),
    (5, '报损', 'DAMAGE_LOSS', 'finance_stock_adjustment_type', '', 'danger', 'N', '0', 'admin', NOW(), '减少库存'),
    (6, '其他', 'OTHER', 'finance_stock_adjustment_type', '', 'info', 'N', '0', 'admin', NOW(), '由表单选择方向');

SELECT '库存调整类型字典核验' AS reconciliation_type, COUNT(*) AS cnt
FROM sys_dict_data WHERE dict_type = 'finance_stock_adjustment_type' AND status = '0';
