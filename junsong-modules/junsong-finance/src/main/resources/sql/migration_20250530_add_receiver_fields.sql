-- 为进货单添加收货人信息字段
ALTER TABLE fin_purchase 
ADD COLUMN receiver_name VARCHAR(64) COMMENT '收货人姓名' AFTER status,
ADD COLUMN receiver_phone VARCHAR(32) COMMENT '收货人电话' AFTER receiver_name,
ADD COLUMN receiver_address VARCHAR(256) COMMENT '收货人地址' AFTER receiver_phone;
