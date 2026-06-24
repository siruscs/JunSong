-- ==========================================
-- 创建费用记录相关表
-- ==========================================

-- 1. 费用记录表
DROP TABLE IF EXISTS `fin_expense`;
CREATE TABLE `fin_expense` (
  `expense_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '费用记录ID',
  `dept_id` BIGINT COMMENT '部门ID',
  `expense_no` VARCHAR(64) NOT NULL COMMENT '费用单号',
  `expense_date` DATETIME NOT NULL COMMENT '费用日期',
  `expense_type` VARCHAR(32) NOT NULL COMMENT '费用类别(预支 开支 收入)',
  `expense_content` TEXT COMMENT '花销内容',
  `payment_method` VARCHAR(32) NOT NULL COMMENT '付款方式(直接付款 预支资金 自行垫付 收入)',
  `expense_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '费用金额',
  `advance_id` BIGINT COMMENT '关联借支ID',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态(0未核销 1已核销)',
  `verify_by` VARCHAR(64) DEFAULT '' COMMENT '核销人',
  `verify_time` DATETIME DEFAULT NULL COMMENT '核销时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志(0代表存在 2代表删除)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`expense_id`),
  UNIQUE KEY `uk_expense_no` (`expense_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_expense_date` (`expense_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='费用记录表';

-- 2. 借支记录表
DROP TABLE IF EXISTS `fin_advance`;
CREATE TABLE `fin_advance` (
  `advance_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '借支记录ID',
  `dept_id` BIGINT COMMENT '部门ID',
  `advance_no` VARCHAR(64) NOT NULL COMMENT '借支单号',
  `advance_date` DATETIME NOT NULL COMMENT '借支日期',
  `advance_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '借支金额',
  `borrower` VARCHAR(64) COMMENT '借款人',
  `purpose` VARCHAR(256) COMMENT '借支用途',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态(0未核销 1已核销)',
  `verify_by` VARCHAR(64) DEFAULT '' COMMENT '核销人',
  `verify_time` DATETIME DEFAULT NULL COMMENT '核销时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志(0代表存在 2代表删除)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`advance_id`),
  UNIQUE KEY `uk_advance_no` (`advance_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_advance_date` (`advance_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='借支记录表';

-- 3. 成本核算表
DROP TABLE IF EXISTS `fin_cost_accounting`;
CREATE TABLE `fin_cost_accounting` (
  `accounting_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成本核算ID',
  `dept_id` BIGINT COMMENT '部门ID',
  `accounting_no` VARCHAR(64) NOT NULL COMMENT '核算单号',
  `start_date` DATETIME NOT NULL COMMENT '核算开始日期',
  `end_date` DATETIME NOT NULL COMMENT '核算结束日期',
  `total_expense` DECIMAL(12,2) DEFAULT 0.00 COMMENT '总花销费用',
  `total_purchase` DECIMAL(12,2) DEFAULT 0.00 COMMENT '总进货金额',
  `total_sale` DECIMAL(12,2) DEFAULT 0.00 COMMENT '总销售金额',
  `total_payment` DECIMAL(12,2) DEFAULT 0.00 COMMENT '总缴款金额',
  `return_situation` DECIMAL(12,2) DEFAULT 0.00 COMMENT '回本情况(总缴款-总花销-总进货)',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志(0代表存在 2代表删除)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`accounting_id`),
  UNIQUE KEY `uk_accounting_no` (`accounting_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_start_date` (`start_date`),
  KEY `idx_end_date` (`end_date`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='成本核算表';

-- 4. 投资人返款记录表
DROP TABLE IF EXISTS `fin_investor_payment`;
CREATE TABLE `fin_investor_payment` (
  `payment_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '返款记录ID',
  `dept_id` BIGINT COMMENT '部门ID',
  `payment_no` VARCHAR(64) NOT NULL COMMENT '返款单号',
  `payment_date` DATETIME NOT NULL COMMENT '日期',
  `payment_type` VARCHAR(32) NOT NULL COMMENT '类型(打款 返款)',
  `investor_name` VARCHAR(64) COMMENT '投资人姓名',
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志(0代表存在 2代表删除)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_payment_date` (`payment_date`),
  KEY `idx_payment_type` (`payment_type`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='投资人返款记录表';
