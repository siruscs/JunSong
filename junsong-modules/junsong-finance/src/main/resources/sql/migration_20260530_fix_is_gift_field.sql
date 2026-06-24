-- ==========================================
-- 修复 is_gift 字段长度问题
-- ==========================================

-- 修改 is_gift 字段为 VARCHAR(10) 以支持 true/false 或 0/1 等值
ALTER TABLE fin_purchase_detail MODIFY COLUMN is_gift VARCHAR(10) DEFAULT '0' COMMENT '是否赠品（0否 1是）';
