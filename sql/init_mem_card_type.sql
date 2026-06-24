-- =============================================
-- JunSong Cloud 字典数据初始化脚本
-- 用于配置会员卡类型等字典数据
-- =============================================

-- 1. 首先检查字典类型是否存在，如果不存在则创建
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
SELECT 100, '会员卡类型', 'mem_card_type', '0', 'admin', NOW(), '会员卡类型字典'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mem_card_type');

-- 2. 插入会员卡类型字典数据
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES 
(1001, 1, '年卡', '1', 'mem_card_type', NULL, 'primary', 'N', '0', 'admin', NOW(), '年卡会员'),
(1002, 2, '半年卡', '2', 'mem_card_type', NULL, 'success', 'N', '0', 'admin', NOW(), '半年卡会员'),
(1003, 3, '季度卡', '3', 'mem_card_type', NULL, 'info', 'N', '0', 'admin', NOW(), '季度卡会员'),
(1004, 4, '月卡', '4', 'mem_card_type', NULL, 'warning', 'N', '0', 'admin', NOW(), '月卡会员'),
(1005, 5, '次卡', '5', 'mem_card_type', NULL, 'danger', 'N', '0', 'admin', NOW(), '次卡会员');

-- 3. 查询验证
SELECT 
    dt.dict_name,
    dt.dict_type,
    dd.dict_label,
    dd.dict_value,
    dd.status
FROM sys_dict_type dt
LEFT JOIN sys_dict_data dd ON dt.dict_type = dd.dict_type
WHERE dt.dict_type = 'mem_card_type'
ORDER BY dd.dict_sort;

-- =============================================
-- 说明：
-- 1. 会员卡类型字典(mem_card_type)包含以下类型：
--    - 年卡(1): 年费会员卡
--    - 半年卡(2): 半年期会员卡
--    - 季度卡(3): 季度会员卡
--    - 月卡(4): 月度会员卡
--    - 次卡(5): 按次数消费的会员卡
--
-- 2. 使用方法：
--    在Vue组件中通过以下方式使用：
--    <el-select v-model="form.cardType">
--      <el-option
--        v-for="dict in dict.type.mem_card_type"
--        :key="dict.value"
--        :label="dict.label"
--        :value="dict.value"
--      />
--    </el-select>
--
-- 3. 注意事项：
--    - 组件需要引入字典：dicts: ['mem_card_type']
--    - 确保字典状态为正常(status='0')
--    - 可以根据需要添加更多会员卡类型
-- =============================================
