-- 会员服务模块数据库表结构
-- 创建日期: 2026-05-31

-- 1. 会员信息表
CREATE TABLE `mem_member` (
  `member_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '会员ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `member_no` varchar(64) NOT NULL COMMENT '会员编号',
  `member_name` varchar(64) NOT NULL COMMENT '会员姓名',
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号码',
  `age` int(11) DEFAULT NULL COMMENT '年龄',
  `address` varchar(256) DEFAULT NULL COMMENT '住址',
  `id_card` varchar(32) DEFAULT NULL COMMENT '身份证号',
  `card_type` varchar(32) DEFAULT NULL COMMENT '会员卡类型(体验卡/正式卡/一星会员/二星会员/三星会员/四星会员/五星会员)',
  `card_id` bigint(20) DEFAULT NULL COMMENT '会员卡ID',
  `join_date` datetime DEFAULT NULL COMMENT '入会日期',
  `expire_date` datetime DEFAULT NULL COMMENT '有效期至',
  `total_points` decimal(12,2) DEFAULT 0.00 COMMENT '总积分',
  `available_points` decimal(12,2) DEFAULT 0.00 COMMENT '可用积分',
  `status` char(1) DEFAULT '0' COMMENT '状态(0正常/1无效/2已退卡)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0存在/2删除)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uk_member_no` (`member_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='会员信息表';

-- 2. 会员卡类型表
CREATE TABLE `mem_member_card_type` (
  `type_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '类型ID',
  `type_name` varchar(64) NOT NULL COMMENT '类型名称',
  `type_code` varchar(32) NOT NULL COMMENT '类型代码',
  `card_fee` decimal(10,2) DEFAULT 9.90 COMMENT '办卡费用',
  `discount_rate` decimal(5,2) DEFAULT 1.00 COMMENT '折扣率',
  `points_rate` decimal(5,2) DEFAULT 1.00 COMMENT '积分倍率',
  `status` char(1) DEFAULT '0' COMMENT '状态(0正常/1禁用)',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`type_id`),
  UNIQUE KEY `uk_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='会员卡类型表';

-- 3. 秒杀/团购活动表
CREATE TABLE `mem_seckill` (
  `seckill_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '秒杀ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `seckill_no` varchar(64) NOT NULL COMMENT '秒杀编号',
  `seckill_name` varchar(128) NOT NULL COMMENT '秒杀名称',
  `seckill_type` varchar(32) NOT NULL COMMENT '类型(秒杀/团购)',
  `seckill_date` date DEFAULT NULL COMMENT '秒杀日期',
  `seckill_time` varchar(32) DEFAULT NULL COMMENT '秒杀时间段',
  `seckill_amount` decimal(10,2) NOT NULL COMMENT '秒杀金额',
  `seckill_price` decimal(10,2) NOT NULL COMMENT '秒杀单价',
  `total_shares` int(11) DEFAULT 0 COMMENT '总份额',
  `remain_shares` int(11) DEFAULT 0 COMMENT '剩余份额',
  `policy` varchar(256) DEFAULT NULL COMMENT '秒杀政策',
  `status` char(1) DEFAULT '0' COMMENT '状态(0进行中/1已结束/2已取消)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`seckill_id`),
  UNIQUE KEY `uk_seckill_no` (`seckill_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='秒杀/团购活动表';

-- 4. 秒杀记录表
CREATE TABLE `mem_seckill_record` (
  `record_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `seckill_id` bigint(20) NOT NULL COMMENT '秒杀ID',
  `member_id` bigint(20) DEFAULT NULL COMMENT '会员ID',
  `member_no` varchar(64) DEFAULT NULL COMMENT '会员编号',
  `member_name` varchar(64) DEFAULT NULL COMMENT '会员姓名',
  `seckill_date` date DEFAULT NULL COMMENT '秒杀日期',
  `payment_method` varchar(32) DEFAULT NULL COMMENT '交费方式',
  `shares` int(11) DEFAULT 1 COMMENT '秒杀份额',
  `total_amount` decimal(10,2) DEFAULT 0.00 COMMENT '总金额',
  `status` char(1) DEFAULT '0' COMMENT '状态(0待领取/1已领取/2已取消/3部分领取)',
  `claim_time` datetime DEFAULT NULL COMMENT '领取时间',
  `claim_by` varchar(64) DEFAULT NULL COMMENT '领取人',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='秒杀记录表';

-- 5. 秒杀领取明细表
CREATE TABLE `mem_seckill_claim_record` (
  `claim_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '领取ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `record_id` bigint(20) NOT NULL COMMENT '秒杀记录ID',
  `seckill_id` bigint(20) NOT NULL COMMENT '秒杀活动ID',
  `member_id` bigint(20) DEFAULT NULL COMMENT '会员ID',
  `member_no` varchar(64) DEFAULT NULL COMMENT '会员编号',
  `member_name` varchar(64) DEFAULT NULL COMMENT '会员姓名',
  `claim_shares` int(11) DEFAULT 1 COMMENT '本次领取份额',
  `claim_time` datetime DEFAULT NULL COMMENT '领取时间',
  `claim_by` varchar(64) DEFAULT NULL COMMENT '领取人',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`claim_id`),
  KEY `idx_record_id` (`record_id`),
  KEY `idx_seckill_id` (`seckill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='秒杀领取明细表';

-- 6. 积分物品表
CREATE TABLE `mem_points_goods` (
  `goods_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '物品ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `goods_name` varchar(128) NOT NULL COMMENT '物品名称',
  `goods_code` varchar(64) DEFAULT NULL COMMENT '物品编码',
  `goods_value` decimal(10,2) DEFAULT 0.00 COMMENT '物品价值(元)',
  `points_price` decimal(12,2) NOT NULL COMMENT '积分价格',
  `stock` int(11) DEFAULT 0 COMMENT '库存',
  `exchanged` int(11) DEFAULT 0 COMMENT '已兑换数量',
  `goods_image` varchar(256) DEFAULT NULL COMMENT '物品图片',
  `status` char(1) DEFAULT '0' COMMENT '状态(0上架/1下架)',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`goods_id`),
  UNIQUE KEY `uk_goods_code` (`goods_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='积分物品表';

-- 6. 积分规则表
CREATE TABLE `mem_points_rule` (
  `rule_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_name` varchar(64) NOT NULL COMMENT '规则名称',
  `rule_code` varchar(32) NOT NULL COMMENT '规则代码',
  `rule_type` varchar(32) NOT NULL COMMENT '计算方式(进一法/四舍五入/舍零取整)',
  `points_per_yuan` decimal(5,2) DEFAULT 1.00 COMMENT '每元积分',
  `validity_days` int(11) DEFAULT 99 COMMENT '积分有效期(天)',
  `status` char(1) DEFAULT '0' COMMENT '状态(0启用/1禁用)',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='积分规则表';

-- 7. 积分记录表
CREATE TABLE `mem_points_record` (
  `record_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `member_id` bigint(20) NOT NULL COMMENT '会员ID',
  `member_no` varchar(64) DEFAULT NULL COMMENT '会员编号',
  `member_name` varchar(64) DEFAULT NULL COMMENT '会员姓名',
  `record_type` varchar(32) NOT NULL COMMENT '类型(消费得积分/兑换扣积分/过期清零/手动调整)',
  `consume_amount` decimal(10,2) DEFAULT 0.00 COMMENT '消费金额',
  `points` decimal(12,2) NOT NULL COMMENT '积分变动',
  `balance` decimal(12,2) DEFAULT 0.00 COMMENT '变动后余额',
  `rule_code` varchar(32) DEFAULT NULL COMMENT '使用的规则代码',
  `expire_date` datetime DEFAULT NULL COMMENT '积分过期日期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='积分记录表';

-- 8. 积分兑换记录表
CREATE TABLE `mem_points_exchange` (
  `exchange_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '兑换ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `exchange_no` varchar(64) NOT NULL COMMENT '兑换单号',
  `member_id` bigint(20) NOT NULL COMMENT '会员ID',
  `member_no` varchar(64) DEFAULT NULL COMMENT '会员编号',
  `member_name` varchar(64) DEFAULT NULL COMMENT '会员姓名',
  `goods_id` bigint(20) NOT NULL COMMENT '物品ID',
  `goods_name` varchar(128) DEFAULT NULL COMMENT '物品名称',
  `exchange_date` datetime DEFAULT NULL COMMENT '兑换日期',
  `quantity` int(11) DEFAULT 1 COMMENT '兑换数量',
  `points_deducted` decimal(12,2) NOT NULL COMMENT '扣减积分',
  `payment_method` varchar(32) DEFAULT NULL COMMENT '补差价方式(现金/微信/支付宝)',
  `extra_amount` decimal(10,2) DEFAULT 0.00 COMMENT '补差价金额',
  `status` char(1) DEFAULT '0' COMMENT '状态(0待领取/1已领取/2已取消)',
  `claim_time` datetime DEFAULT NULL COMMENT '领取时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`exchange_id`),
  UNIQUE KEY `uk_exchange_no` (`exchange_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='积分兑换记录表';

-- 插入默认会员卡类型
INSERT INTO `mem_member_card_type` (`type_name`, `type_code`, `card_fee`, `discount_rate`, `points_rate`, `status`, `del_flag`, `create_by`, `create_time`) VALUES
('体验卡', 'experience', 9.90, 1.00, 1.00, '0', '0', 'admin', NOW()),
('正式会员卡', 'formal', 9.90, 0.95, 1.00, '0', '0', 'admin', NOW()),
('一星会员', 'star1', 9.90, 0.90, 1.20, '0', '0', 'admin', NOW()),
('二星会员', 'star2', 9.90, 0.85, 1.40, '0', '0', 'admin', NOW()),
('三星会员', 'star3', 9.90, 0.80, 1.60, '0', '0', 'admin', NOW()),
('四星会员', 'star4', 9.90, 0.75, 1.80, '0', '0', 'admin', NOW()),
('五星会员', 'star5', 9.90, 0.70, 2.00, '0', '0', 'admin', NOW());

-- 插入默认积分规则
INSERT INTO `mem_points_rule` (`rule_name`, `rule_code`, `rule_type`, `points_per_yuan`, `validity_days`, `status`, `del_flag`, `create_by`, `create_time`) VALUES
('进一法', 'ceil', '进一法', 1.00, 99, '0', '0', 'admin', NOW()),
('四舍五入', 'round', '四舍五入', 1.00, 99, '0', '0', 'admin', NOW()),
('舍零取整', 'floor', '舍零取整', 1.00, 99, '0', '0', 'admin', NOW());
