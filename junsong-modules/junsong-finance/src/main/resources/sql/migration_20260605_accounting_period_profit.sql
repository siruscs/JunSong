-- 财务核算周期、投资人、投资款、分润结转相关表

CREATE TABLE IF NOT EXISTS `fin_dept_profit_config` (
  `config_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `dept_id` bigint(20) NOT NULL COMMENT '机构ID',
  `manager_profit_rate` decimal(10,4) NOT NULL DEFAULT '0.0000' COMMENT '店长分润比例',
  `auto_create_investor_payment` char(1) NOT NULL DEFAULT '1' COMMENT '是否自动生成投资人返款（0否 1是）',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店面分润配置表';

CREATE TABLE IF NOT EXISTS `fin_accounting_period` (
  `period_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '周期ID',
  `dept_id` bigint(20) NOT NULL COMMENT '机构ID',
  `period_no` varchar(64) NOT NULL COMMENT '周期编号',
  `start_time` datetime NOT NULL COMMENT '周期开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '周期结束时间',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0进行中 1已回本待结转 2已结转）',
  `break_even_time` datetime DEFAULT NULL COMMENT '回本时间',
  `carry_forward_time` datetime DEFAULT NULL COMMENT '结转时间',
  `total_verified_expense` decimal(18,2) DEFAULT '0.00' COMMENT '已核销费用',
  `total_purchase` decimal(18,2) DEFAULT '0.00' COMMENT '进货款',
  `total_sale_payment` decimal(18,2) DEFAULT '0.00' COMMENT '销售缴款总额',
  `total_unverified_advance` decimal(18,2) DEFAULT '0.00' COMMENT '借支未核销',
  `net_profit` decimal(18,2) DEFAULT '0.00' COMMENT '净利',
  `manager_profit_rate` decimal(10,4) DEFAULT '0.0000' COMMENT '店长分润比例',
  `manager_profit_amount` decimal(18,2) DEFAULT '0.00' COMMENT '店长分润金额',
  `investor_profit_amount` decimal(18,2) DEFAULT '0.00' COMMENT '投资人分润总额',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`period_id`),
  UNIQUE KEY `uk_dept_period_no` (`dept_id`,`period_no`),
  KEY `idx_dept_status` (`dept_id`,`status`),
  KEY `idx_dept_time` (`dept_id`,`start_time`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务核算周期表';

CREATE TABLE IF NOT EXISTS `fin_investor` (
  `investor_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '投资人ID',
  `dept_id` bigint(20) NOT NULL COMMENT '机构ID',
  `investor_name` varchar(64) NOT NULL COMMENT '投资人姓名',
  `phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`investor_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投资人表';

CREATE TABLE IF NOT EXISTS `fin_invest_record` (
  `invest_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '投资记录ID',
  `dept_id` bigint(20) NOT NULL COMMENT '机构ID',
  `period_id` bigint(20) DEFAULT NULL COMMENT '周期ID',
  `investor_id` bigint(20) NOT NULL COMMENT '投资人ID',
  `investor_name` varchar(64) NOT NULL COMMENT '投资人姓名',
  `invest_amount` decimal(18,2) NOT NULL COMMENT '投资金额',
  `invest_time` datetime NOT NULL COMMENT '投资时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`invest_id`),
  KEY `idx_dept_period` (`dept_id`,`period_id`),
  KEY `idx_investor_id` (`investor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投资来源记录表';

CREATE TABLE IF NOT EXISTS `fin_profit_share_record` (
  `share_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '分润ID',
  `dept_id` bigint(20) NOT NULL COMMENT '机构ID',
  `period_id` bigint(20) NOT NULL COMMENT '周期ID',
  `share_no` varchar(64) NOT NULL COMMENT '分润单号',
  `net_profit` decimal(18,2) NOT NULL COMMENT '净利',
  `manager_profit_rate` decimal(10,4) NOT NULL COMMENT '店长分润比例',
  `manager_profit_amount` decimal(18,2) NOT NULL COMMENT '店长分润金额',
  `investor_profit_amount` decimal(18,2) NOT NULL COMMENT '投资人分润金额',
  `status` char(1) NOT NULL DEFAULT '1' COMMENT '状态（0草稿 1已完成）',
  `share_time` datetime NOT NULL COMMENT '分润时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`share_id`),
  UNIQUE KEY `uk_share_no` (`share_no`),
  KEY `idx_dept_period` (`dept_id`,`period_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分润记录主表';

CREATE TABLE IF NOT EXISTS `fin_profit_share_detail` (
  `detail_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '分润明细ID',
  `share_id` bigint(20) NOT NULL COMMENT '分润ID',
  `dept_id` bigint(20) NOT NULL COMMENT '机构ID',
  `period_id` bigint(20) NOT NULL COMMENT '周期ID',
  `receiver_type` char(1) NOT NULL COMMENT '接收方类型（1店长 2投资人）',
  `receiver_id` bigint(20) DEFAULT NULL COMMENT '接收方ID',
  `receiver_name` varchar(64) NOT NULL COMMENT '接收方名称',
  `invest_amount` decimal(18,2) DEFAULT '0.00' COMMENT '投资金额',
  `invest_ratio` decimal(10,4) DEFAULT '0.0000' COMMENT '投资占比',
  `share_amount` decimal(18,2) NOT NULL COMMENT '分润金额',
  `payment_id` bigint(20) DEFAULT NULL COMMENT '自动生成的投资返款ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_share_id` (`share_id`),
  KEY `idx_dept_period` (`dept_id`,`period_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分润记录明细表';

SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `fin_expense` ADD COLUMN `period_id` bigint(20) DEFAULT NULL COMMENT ''周期ID''', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fin_expense' AND column_name = 'period_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `fin_advance` ADD COLUMN `period_id` bigint(20) DEFAULT NULL COMMENT ''周期ID''', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fin_advance' AND column_name = 'period_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `fin_purchase` ADD COLUMN `period_id` bigint(20) DEFAULT NULL COMMENT ''周期ID''', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fin_purchase' AND column_name = 'period_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `fin_sale_record` ADD COLUMN `period_id` bigint(20) DEFAULT NULL COMMENT ''周期ID''', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fin_sale_record' AND column_name = 'period_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `fin_sale_payment` ADD COLUMN `period_id` bigint(20) DEFAULT NULL COMMENT ''周期ID''', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fin_sale_payment' AND column_name = 'period_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `fin_investor_payment` ADD COLUMN `period_id` bigint(20) DEFAULT NULL COMMENT ''周期ID''', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fin_investor_payment' AND column_name = 'period_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `fin_investor_payment` ADD COLUMN `share_id` bigint(20) DEFAULT NULL COMMENT ''分润单ID''', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fin_investor_payment' AND column_name = 'share_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `fin_investor_payment` ADD COLUMN `share_detail_id` bigint(20) DEFAULT NULL COMMENT ''分润明细ID''', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fin_investor_payment' AND column_name = 'share_detail_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `fin_investor_payment` ADD COLUMN `investor_id` bigint(20) DEFAULT NULL COMMENT ''投资人ID''', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fin_investor_payment' AND column_name = 'investor_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `fin_investor_payment` ADD COLUMN `source_type` char(1) DEFAULT ''0'' COMMENT ''来源类型（0手工返款 1结转分润自动返款）''', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fin_investor_payment' AND column_name = 'source_type');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `fin_investor_payment` ADD COLUMN `payment_status` char(1) DEFAULT ''1'' COMMENT ''返款状态（0待返款 1已返款）''', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fin_investor_payment' AND column_name = 'payment_status');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE `fin_investor_payment` ADD COLUMN `invest_ratio` decimal(10,4) DEFAULT ''0.0000'' COMMENT ''本次投资占比''', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fin_investor_payment' AND column_name = 'invest_ratio');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `fin_profit_share_detail`
SET `receiver_type` = '1'
WHERE `receiver_type` IN ('manager', 'm');

UPDATE `fin_profit_share_detail`
SET `receiver_type` = '2'
WHERE `receiver_type` IN ('investor', 'i');
