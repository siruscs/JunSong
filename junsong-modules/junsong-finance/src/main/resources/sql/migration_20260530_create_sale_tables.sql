-- ==========================================
-- 创建销售记录表和缴款记录表
-- ==========================================

-- 1. 销售记录表
DROP TABLE IF EXISTS `fin_sale_record`;
CREATE TABLE `fin_sale_record` (
  `sale_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '销售记录ID',
  `dept_id` BIGINT COMMENT '部门ID',
  `sale_no` VARCHAR(64) NOT NULL COMMENT '销售单号',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(128) COMMENT '商品名称',
  `sale_quantity` INT NOT NULL DEFAULT 0 COMMENT '销售数量',
  `gift_quantity` INT NOT NULL DEFAULT 0 COMMENT '赠品数量',
  `total_quantity` INT NOT NULL DEFAULT 0 COMMENT '总数量(销售+赠品)',
  `sale_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '销售金额',
  `unit_price` DECIMAL(12,2) DEFAULT 0.00 COMMENT '单价(销售金额/销售数量)',
  `paid_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '已缴金额',
  `sale_date` DATETIME NOT NULL COMMENT '销售日期',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态(0待缴款 1部分缴款 2已缴清)',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志(0代表存在 2代表删除)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`sale_id`),
  UNIQUE KEY `uk_sale_no` (`sale_no`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_sale_date` (`sale_date`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='销售记录表';

-- 2. 缴款记录表
DROP TABLE IF EXISTS `fin_sale_payment`;
CREATE TABLE `fin_sale_payment` (
  `payment_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '缴款记录ID',
  `dept_id` BIGINT COMMENT '部门ID',
  `sale_id` BIGINT NOT NULL COMMENT '销售记录ID',
  `payment_no` VARCHAR(64) NOT NULL COMMENT '缴款单号',
  `payment_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '缴款金额',
  `payment_method` VARCHAR(32) COMMENT '付款方式',
  `payment_date` DATETIME NOT NULL COMMENT '缴款日期',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志(0代表存在 2代表删除)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_sale_id` (`sale_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_payment_date` (`payment_date`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='缴款记录表';
