-- 会员购买域第一阶段表结构
-- 只新增会员模块表，不修改财务销售记录和财务销售统计。
-- 执行前请使用项目标准 SQL 部署流程，并在目标环境核对 tenant_id、dept_id 和 period_id。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mem_member_no_sequence` (
  `sequence_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会员编号序列ID',
  `dept_id` BIGINT NOT NULL DEFAULT 0 COMMENT '机构ID，按机构和前缀独立编号；历史默认值0仅用于兼容旧数据',
  `prefix` VARCHAR(16) NOT NULL COMMENT '编号前缀',
  `next_value` BIGINT NOT NULL COMMENT '下一个编号序号',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sequence_id`),
  UNIQUE KEY `uk_mem_member_no_sequence` (`dept_id`, `prefix`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员编号并发序列';

CREATE TABLE IF NOT EXISTS `mem_member_event` (
  `event_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会员事件ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门/门店ID',
  `member_id` BIGINT DEFAULT NULL COMMENT '会员ID，匿名事件为空',
  `event_type` VARCHAR(64) NOT NULL COMMENT '事件类型',
  `source_type` VARCHAR(32) NOT NULL COMMENT '来源类型',
  `source_id` VARCHAR(64) NOT NULL COMMENT '来源单号',
  `event_time` DATETIME NOT NULL COMMENT '事件发生时间',
  `payload` JSON DEFAULT NULL COMMENT '事件快照',
  `dedup_key` VARCHAR(128) NOT NULL COMMENT '幂等键',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`),
  UNIQUE KEY `uk_mem_member_event_dedup` (`tenant_id`, `dedup_key`),
  KEY `idx_mem_member_event_member` (`tenant_id`, `dept_id`, `member_id`, `event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员事件流水';

CREATE TABLE IF NOT EXISTS `mem_member_tag_rule` (
  `rule_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签规则ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门/门店ID',
  `rule_code` VARCHAR(64) NOT NULL COMMENT '规则编码',
  `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `rule_version` INT NOT NULL DEFAULT 1 COMMENT '规则版本',
  `expression` JSON NOT NULL COMMENT '规则表达式快照',
  `status` CHAR(1) NOT NULL DEFAULT '1' COMMENT '1生效 0停用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_mem_member_tag_rule_version` (`tenant_id`, `dept_id`, `rule_code`, `rule_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员标签规则版本';

CREATE TABLE IF NOT EXISTS `mem_member_tag` (
  `tag_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会员标签ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门/门店ID',
  `member_id` BIGINT NOT NULL COMMENT '会员ID',
  `tag_code` VARCHAR(64) NOT NULL COMMENT '标签编码',
  `rule_id` BIGINT DEFAULT NULL COMMENT '命中规则ID',
  `rule_version` INT DEFAULT NULL COMMENT '命中规则版本',
  `calculated_at` DATETIME NOT NULL COMMENT '计算时间',
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `uk_mem_member_tag` (`tenant_id`, `member_id`, `tag_code`),
  KEY `idx_mem_member_tag_dept` (`tenant_id`, `dept_id`, `tag_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员标签结果';

CREATE TABLE IF NOT EXISTS `mem_member_metric_snapshot` (
  `snapshot_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '指标快照ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门/门店ID',
  `metric_date` DATE NOT NULL COMMENT '指标日期',
  `metric_code` VARCHAR(64) NOT NULL COMMENT '指标编码',
  `metric_value` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '指标值',
  `metric_version` INT NOT NULL DEFAULT 1 COMMENT '指标口径版本',
  `calculated_at` DATETIME NOT NULL COMMENT '计算时间',
  PRIMARY KEY (`snapshot_id`),
  UNIQUE KEY `uk_mem_member_metric_snapshot` (`tenant_id`, `dept_id`, `metric_date`, `metric_code`, `metric_version`),
  KEY `idx_mem_member_metric_date` (`tenant_id`, `dept_id`, `metric_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员运营指标快照';

CREATE TABLE IF NOT EXISTS `mem_identity_policy` (
  `policy_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '身份策略ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '门店/部门ID',
  `identity_mode` VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT 'NAME/PHONE/MEMBER_NO/MANUAL',
  `allow_anonymous` TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许匿名非会员',
  `allow_member_without_phone` TINYINT NOT NULL DEFAULT 1 COMMENT '会员手机号是否可为空',
  `status` CHAR(1) NOT NULL DEFAULT '1' COMMENT '1生效 0停用',
  `create_by` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`policy_id`),
  UNIQUE KEY `uk_mem_identity_policy_dept` (`tenant_id`, `dept_id`),
  KEY `idx_mem_identity_policy_status` (`tenant_id`, `dept_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店顾客身份识别策略';

CREATE TABLE IF NOT EXISTS `mem_member_status_history` (
  `history_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '状态历史ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门/门店ID',
  `member_id` BIGINT NOT NULL COMMENT '会员ID',
  `from_status` VARCHAR(32) NOT NULL COMMENT '原状态',
  `to_status` VARCHAR(32) NOT NULL COMMENT '目标状态',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
  `operator` VARCHAR(64) NOT NULL COMMENT '操作人',
  `changed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  PRIMARY KEY (`history_id`),
  KEY `idx_mem_member_status_history_member` (`tenant_id`, `dept_id`, `member_id`, `changed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员生命周期状态历史';

CREATE TABLE IF NOT EXISTS `mem_member_account` (
  `account_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账户ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门/门店ID',
  `member_id` BIGINT NOT NULL COMMENT '会员ID',
  `points_balance` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '积分余额',
  `growth_balance` BIGINT NOT NULL DEFAULT 0 COMMENT '成长值余额',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `uk_mem_member_account_member` (`tenant_id`, `member_id`),
  KEY `idx_mem_member_account_dept` (`tenant_id`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员权益账户快照';

CREATE TABLE IF NOT EXISTS `mem_points_ledger` (
  `ledger_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '积分流水ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门/门店ID',
  `member_id` BIGINT NOT NULL COMMENT '会员ID',
  `source_type` VARCHAR(32) NOT NULL COMMENT '业务来源',
  `source_id` VARCHAR(64) NOT NULL COMMENT '业务单号',
  `dedup_key` VARCHAR(128) NOT NULL COMMENT '幂等键',
  `delta` DECIMAL(12,2) NOT NULL COMMENT '积分变动',
  `balance_after` DECIMAL(12,2) NOT NULL COMMENT '变动后余额',
  `reverse_of` BIGINT DEFAULT NULL COMMENT '被冲正流水ID',
  `operator` VARCHAR(64) NOT NULL COMMENT '操作人',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ledger_id`),
  UNIQUE KEY `uk_mem_points_ledger_dedup` (`tenant_id`, `dedup_key`),
  KEY `idx_mem_points_ledger_member` (`tenant_id`, `member_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员积分不可变流水';

CREATE TABLE IF NOT EXISTS `mem_growth_ledger` (
  `ledger_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成长值流水ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门/门店ID',
  `member_id` BIGINT NOT NULL COMMENT '会员ID',
  `source_type` VARCHAR(32) NOT NULL COMMENT '业务来源',
  `source_id` VARCHAR(64) NOT NULL COMMENT '业务单号',
  `dedup_key` VARCHAR(128) NOT NULL COMMENT '幂等键',
  `delta` BIGINT NOT NULL COMMENT '成长值变动',
  `balance_after` BIGINT NOT NULL COMMENT '变动后余额',
  `reverse_of` BIGINT DEFAULT NULL COMMENT '被冲正流水ID',
  `operator` VARCHAR(64) NOT NULL COMMENT '操作人',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ledger_id`),
  UNIQUE KEY `uk_mem_growth_ledger_dedup` (`tenant_id`, `dedup_key`),
  KEY `idx_mem_growth_ledger_member` (`tenant_id`, `member_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员成长值不可变流水';

CREATE TABLE IF NOT EXISTS `mem_campaign_policy` (
  `policy_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '政策ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门/门店ID',
  `period_id` BIGINT NOT NULL COMMENT '核算周期ID',
  `product_id` BIGINT NOT NULL COMMENT '政策商品ID',
  `policy_no` VARCHAR(64) NOT NULL COMMENT '政策编号',
  `policy_name` VARCHAR(128) NOT NULL COMMENT '政策名称',
  `version` INT NOT NULL DEFAULT 1 COMMENT '政策版本',
  `customer_scope` VARCHAR(16) NOT NULL DEFAULT 'ALL' COMMENT '适用顾客 MEMBER/CUSTOMER/WALK_IN/ALL',
  `effective_start` DATETIME NOT NULL COMMENT '生效时间',
  `effective_end` DATETIME DEFAULT NULL COMMENT '失效时间',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '0草稿 1生效 2停用 3归档',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '0存在 2删除',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`policy_id`),
  UNIQUE KEY `uk_mem_campaign_policy_dept_no` (`tenant_id`, `dept_id`, `policy_no`, `version`),
  KEY `idx_mem_campaign_policy_scope` (`tenant_id`, `dept_id`, `period_id`, `product_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员周期商品政策';

CREATE TABLE IF NOT EXISTS `mem_campaign_policy_package` (
  `package_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  `policy_id` BIGINT NOT NULL COMMENT '政策ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '机构ID',
  `package_name` VARCHAR(128) NOT NULL COMMENT '套餐名称',
  `purchase_quantity` DECIMAL(12,3) NOT NULL COMMENT '购买数量',
  `gift_quantity` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '赠送数量',
  `total_quantity` DECIMAL(12,3) NOT NULL COMMENT '可领取总量',
  `package_price` DECIMAL(12,2) DEFAULT NULL COMMENT '套餐价，空表示按商品单价计算',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '0存在 2删除',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`package_id`),
  UNIQUE KEY `uk_mem_campaign_package_quantity` (`policy_id`, `purchase_quantity`),
  KEY `idx_mem_campaign_package_tenant` (`tenant_id`, `policy_id`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员周期商品政策套餐';

CREATE TABLE IF NOT EXISTS `mem_purchase_order` (
  `purchase_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购买单ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门/门店ID',
  `period_id` BIGINT DEFAULT NULL COMMENT '核算周期ID',
  `purchase_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '购买业务日期',
  `purchase_no` VARCHAR(64) NOT NULL COMMENT '购买单号',
  `customer_type` VARCHAR(16) NOT NULL COMMENT 'MEMBER/CUSTOMER/WALK_IN',
  `member_id` BIGINT DEFAULT NULL COMMENT '会员ID',
  `customer_id` BIGINT DEFAULT NULL COMMENT '非会员客户ID',
  `customer_name` VARCHAR(100) DEFAULT NULL COMMENT '顾客姓名快照',
  `customer_phone` VARCHAR(32) DEFAULT NULL COMMENT '顾客手机号快照',
  `identity_mode` VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT 'PHONE/NAME/MEMBER_NO/MANUAL/ANONYMOUS',
  `identity_confirmed` TINYINT NOT NULL DEFAULT 0 COMMENT '身份是否确认',
  `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '应收金额',
  `paid_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '已收金额',
  `receivable_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '欠款金额',
  `payment_status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '0未付款 1部分付款 2已付清 3已退款',
  `delivery_status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '0未领取 1部分领取 2已全部领取',
  `order_status` CHAR(1) NOT NULL DEFAULT '1' COMMENT '0草稿 1已确认 2已完成 3关闭 4作废',
  `idempotency_key` VARCHAR(128) DEFAULT NULL COMMENT '幂等键',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '0存在 2删除',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`purchase_id`),
  UNIQUE KEY `uk_mem_purchase_no` (`tenant_id`, `purchase_no`),
  UNIQUE KEY `uk_mem_purchase_idempotency` (`tenant_id`, `idempotency_key`),
  KEY `idx_mem_purchase_customer` (`tenant_id`, `dept_id`, `customer_type`, `member_id`, `customer_id`),
  KEY `idx_mem_purchase_status` (`tenant_id`, `dept_id`, `payment_status`, `delivery_status`),
  KEY `idx_mem_purchase_purchase_date` (`tenant_id`, `dept_id`, `purchase_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员购买单';

CREATE TABLE IF NOT EXISTS `mem_purchase_item` (
  `item_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购买明细ID',
  `purchase_id` BIGINT NOT NULL COMMENT '购买单ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门/门店ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name_snapshot` VARCHAR(128) NOT NULL COMMENT '商品名称快照',
  `policy_id` BIGINT DEFAULT NULL COMMENT '政策ID',
  `policy_version` INT DEFAULT NULL COMMENT '政策版本',
  `package_id` BIGINT DEFAULT NULL COMMENT '套餐ID',
  `package_name_snapshot` VARCHAR(128) DEFAULT NULL COMMENT '套餐名称快照',
  `purchase_quantity` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '购买数量',
  `gift_quantity` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '赠送数量',
  `total_quantity` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '可领取总量',
  `delivered_quantity` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '已领取数量',
  `delivered_sale_quantity` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '已领取正品数量',
  `delivered_gift_quantity` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '已领取赠品数量',
  `remaining_quantity` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '待领取数量',
  `unit_price` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '单价',
  `item_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '明细金额',
  `policy_snapshot` JSON DEFAULT NULL COMMENT '政策快照',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '0存在 2删除',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`item_id`),
  KEY `idx_mem_purchase_item_purchase` (`tenant_id`, `purchase_id`),
  KEY `idx_mem_purchase_item_product` (`tenant_id`, `dept_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员购买明细';

CREATE TABLE IF NOT EXISTS `mem_purchase_delivery` (
  `delivery_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '领取记录ID',
  `purchase_id` BIGINT NOT NULL COMMENT '购买单ID',
  `item_id` BIGINT NOT NULL COMMENT '购买明细ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门/门店ID',
  `delivery_no` VARCHAR(64) NOT NULL COMMENT '领取单号',
  `sale_delivery_quantity` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '正品领取数量',
  `gift_delivery_quantity` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '赠品领取数量',
  `total_delivery_quantity` DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '本次领取总量',
  `delivery_date` DATETIME NOT NULL COMMENT '领取时间',
  `receiver_name` VARCHAR(64) DEFAULT NULL COMMENT '领取人',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人',
  `status` CHAR(1) NOT NULL DEFAULT '1' COMMENT '1有效 2已撤销',
  `idempotency_key` VARCHAR(128) DEFAULT NULL COMMENT '幂等键',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`delivery_id`),
  UNIQUE KEY `uk_mem_purchase_delivery_no` (`tenant_id`, `delivery_no`),
  UNIQUE KEY `uk_mem_purchase_delivery_idempotency` (`tenant_id`, `idempotency_key`),
  KEY `idx_mem_purchase_delivery_item` (`tenant_id`, `purchase_id`, `item_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员购买分批领取记录';

CREATE TABLE IF NOT EXISTS `mem_purchase_payment` (
  `payment_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购买域收款ID',
  `purchase_id` BIGINT NOT NULL COMMENT '购买单ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `dept_id` BIGINT NOT NULL COMMENT '部门/门店ID',
  `payment_no` VARCHAR(64) NOT NULL COMMENT '收款单号',
  `payment_amount` DECIMAL(12,2) NOT NULL COMMENT '收款金额',
  `payment_method` VARCHAR(32) DEFAULT NULL COMMENT '付款方式',
  `payment_date` DATETIME NOT NULL COMMENT '收款时间',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人',
  `status` CHAR(1) NOT NULL DEFAULT '1' COMMENT '1有效 2已撤销',
  `idempotency_key` VARCHAR(128) DEFAULT NULL COMMENT '幂等键',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `uk_mem_purchase_payment_no` (`tenant_id`, `payment_no`),
  UNIQUE KEY `uk_mem_purchase_payment_idempotency` (`tenant_id`, `idempotency_key`),
  KEY `idx_mem_purchase_payment_purchase` (`tenant_id`, `purchase_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员购买域收款记录';
