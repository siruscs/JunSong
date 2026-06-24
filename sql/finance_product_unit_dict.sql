INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
SELECT '商品计量单位', 'finance_product_unit', '0', 'admin', NOW(), '商品维护计量单位字典'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'finance_product_unit');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 1, '个', '个', 'finance_product_unit', '', 'default', 'Y', '0', 'admin', NOW(), '个'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'finance_product_unit' AND `dict_value` = '个');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 2, '件', '件', 'finance_product_unit', '', 'default', 'N', '0', 'admin', NOW(), '件'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'finance_product_unit' AND `dict_value` = '件');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 3, '箱', '箱', 'finance_product_unit', '', 'default', 'N', '0', 'admin', NOW(), '箱'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'finance_product_unit' AND `dict_value` = '箱');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 4, '盒', '盒', 'finance_product_unit', '', 'default', 'N', '0', 'admin', NOW(), '盒'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'finance_product_unit' AND `dict_value` = '盒');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 5, '袋', '袋', 'finance_product_unit', '', 'default', 'N', '0', 'admin', NOW(), '袋'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'finance_product_unit' AND `dict_value` = '袋');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 6, '瓶', '瓶', 'finance_product_unit', '', 'default', 'N', '0', 'admin', NOW(), '瓶'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'finance_product_unit' AND `dict_value` = '瓶');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 7, 'kg', 'kg', 'finance_product_unit', '', 'default', 'N', '0', 'admin', NOW(), 'kg'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'finance_product_unit' AND `dict_value` = 'kg');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 8, 'g', 'g', 'finance_product_unit', '', 'default', 'N', '0', 'admin', NOW(), 'g'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'finance_product_unit' AND `dict_value` = 'g');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 9, '升', '升', 'finance_product_unit', '', 'default', 'N', '0', 'admin', NOW(), '升'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'finance_product_unit' AND `dict_value` = '升');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 10, '米', '米', 'finance_product_unit', '', 'default', 'N', '0', 'admin', NOW(), '米'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'finance_product_unit' AND `dict_value` = '米');
