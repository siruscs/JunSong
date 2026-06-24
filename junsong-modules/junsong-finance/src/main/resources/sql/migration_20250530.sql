-- =============================================
-- 进货功能优化 - 数据库迁移脚本
-- 日期: 2025-05-30
-- 描述: 添加付款方式、总数量、赠品标识字段
-- =============================================

-- 1. 添加进货单新字段
ALTER TABLE fin_purchase 
ADD COLUMN payment_method VARCHAR(32) COMMENT '付款方式' AFTER paid_amount,
ADD COLUMN total_quantity INT COMMENT '总数量（含赠品）' AFTER payment_method;

-- 2. 添加进货单明细赠品标识字段
ALTER TABLE fin_purchase_detail 
ADD COLUMN is_gift CHAR(1) DEFAULT '0' COMMENT '是否赠品（0否 1是）' AFTER amount;

-- 3. 添加索引（可选，提高查询性能）
-- CREATE INDEX idx_purchase_payment_method ON fin_purchase(payment_method);
-- CREATE INDEX idx_detail_is_gift ON fin_purchase_detail(is_gift);

-- =============================================
-- 迁移完成提示
-- =============================================
SELECT '数据库迁移成功！' AS message;
