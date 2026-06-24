-- 会员卡类型字典数据初始化
-- 执行日期: 2026-01-20

-- 1. 插入字典类型（会员卡类型）
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
VALUES 
('会员卡类型', 'mem_card_type', '0', 'admin', NOW(), '会员卡类型字典');

-- 2. 插入字典数据（会员卡类型选项）
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
VALUES 
(1, '体验卡', 'experience', 'mem_card_type', 'Y', '0', 'admin', NOW(), '体验卡'),
(2, '正式会员卡', 'formal', 'mem_card_type', 'N', '0', 'admin', NOW(), '正式会员卡'),
(3, '一星会员', 'star1', 'mem_card_type', 'N', '0', 'admin', NOW(), '一星会员'),
(4, '二星会员', 'star2', 'mem_card_type', 'N', '0', 'admin', NOW(), '二星会员'),
(5, '三星会员', 'star3', 'mem_card_type', 'N', '0', 'admin', NOW(), '三星会员'),
(6, '四星会员', 'star4', 'mem_card_type', 'N', '0', 'admin', NOW(), '四星会员'),
(7, '五星会员', 'star5', 'mem_card_type', 'N', '0', 'admin', NOW(), '五星会员');
