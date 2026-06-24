-- ==========================================
-- 补充进货单表缺失字段
-- ==========================================

-- 1. 补充进货单表缺失的字段
-- ==========================================

-- 补充供应商名称
ALTER TABLE fin_purchase ADD COLUMN supplier_name VARCHAR(128) COMMENT '供应商名称' AFTER supplier_id;

-- 补充总数量
ALTER TABLE fin_purchase ADD COLUMN total_quantity INT DEFAULT 0 COMMENT '总数量（含赠品）' AFTER total_amount;

-- 补充付款方式
ALTER TABLE fin_purchase ADD COLUMN payment_method VARCHAR(32) COMMENT '付款方式' AFTER paid_amount;

-- 2. 补充收货人信息
-- ==========================================

-- 补充收货人姓名
ALTER TABLE fin_purchase ADD COLUMN receiver_name VARCHAR(64) COMMENT '收货人姓名' AFTER status;

-- 补充收货人电话
ALTER TABLE fin_purchase ADD COLUMN receiver_phone VARCHAR(32) COMMENT '收货人电话' AFTER receiver_name;

-- 补充收货人地址
ALTER TABLE fin_purchase ADD COLUMN receiver_address VARCHAR(256) COMMENT '收货人地址' AFTER receiver_phone;

-- 3. 补充进货单明细表赠品字段
-- ==========================================

-- 补充是否赠品字段
ALTER TABLE fin_purchase_detail ADD COLUMN is_gift CHAR(1) DEFAULT '0' COMMENT '是否赠品（0否 1是）' AFTER amount;
