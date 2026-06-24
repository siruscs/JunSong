-- 商品表
DROP TABLE IF EXISTS `fin_product`;
CREATE TABLE `fin_product` (
  `product_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `product_code` varchar(64) DEFAULT NULL COMMENT '商品编码',
  `product_name` varchar(128) NOT NULL COMMENT '商品名称',
  `category_id` bigint(20) DEFAULT NULL COMMENT '商品分类ID',
  `unit` varchar(32) DEFAULT NULL COMMENT '计量单位',
  `purchase_price` decimal(12,2) DEFAULT '0.00' COMMENT '进货价格',
  `sale_price` decimal(12,2) DEFAULT '0.00' COMMENT '销售价格',
  `stock_num` int(11) DEFAULT '0' COMMENT '库存数量',
  `min_stock` int(11) DEFAULT '0' COMMENT '最低库存预警',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`product_id`),
  KEY `idx_product_code` (`product_code`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 商品分类表
DROP TABLE IF EXISTS `fin_product_category`;
CREATE TABLE `fin_product_category` (
  `category_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` varchar(128) NOT NULL COMMENT '分类名称',
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父分类ID',
  `order_num` int(4) DEFAULT '0' COMMENT '显示顺序',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`category_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 供应商表
DROP TABLE IF EXISTS `fin_supplier`;
CREATE TABLE `fin_supplier` (
  `supplier_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '供应商ID',
  `supplier_code` varchar(64) DEFAULT NULL COMMENT '供应商编码',
  `supplier_name` varchar(128) NOT NULL COMMENT '供应商名称',
  `contact_person` varchar(64) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `address` varchar(256) DEFAULT NULL COMMENT '地址',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`supplier_id`),
  KEY `idx_supplier_code` (`supplier_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- 进货单表
DROP TABLE IF EXISTS `fin_purchase`;
CREATE TABLE `fin_purchase` (
  `purchase_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '进货单ID',
  `purchase_no` varchar(64) NOT NULL COMMENT '进货单号',
  `supplier_id` bigint(20) NOT NULL COMMENT '供应商ID',
  `purchase_date` datetime NOT NULL COMMENT '进货日期',
  `total_amount` decimal(12,2) DEFAULT '0.00' COMMENT '总金额',
  `paid_amount` decimal(12,2) DEFAULT '0.00' COMMENT '已付金额',
  `status` char(1) DEFAULT '0' COMMENT '状态（0草稿 1已确认 2已完成）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`purchase_id`),
  UNIQUE KEY `uk_purchase_no` (`purchase_no`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_purchase_date` (`purchase_date`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='进货单表';

-- 进货单明细表
DROP TABLE IF EXISTS `fin_purchase_detail`;
CREATE TABLE `fin_purchase_detail` (
  `detail_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `purchase_id` bigint(20) NOT NULL COMMENT '进货单ID',
  `product_id` bigint(20) NOT NULL COMMENT '商品ID',
  `product_name` varchar(128) DEFAULT NULL COMMENT '商品名称',
  `unit` varchar(32) DEFAULT NULL COMMENT '计量单位',
  `quantity` int(11) NOT NULL COMMENT '数量',
  `price` decimal(12,2) NOT NULL COMMENT '单价',
  `amount` decimal(12,2) NOT NULL COMMENT '金额',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`detail_id`),
  KEY `idx_purchase_id` (`purchase_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='进货单明细表';
