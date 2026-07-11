SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- =============================================================
-- 复合核算 / 多店统一核算 DDL
-- 更新日期: 2026-07-07
-- 说明: 新增 4 张表,支持共享投资人/共享出资款/多店统一回本
-- 安全: 使用 CREATE TABLE IF NOT EXISTS,重复执行不会清空数据
-- =============================================================

-- -------------------------------------------------------------
-- 1. 复合核算池主表(有 tenant_id,受 TenantSqlInterceptor 保护)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `fin_composite_accounting_pool` (
  `pool_id` bigint NOT NULL AUTO_INCREMENT COMMENT '复合核算池ID',
  `pool_no` varchar(64) NOT NULL COMMENT '复合核算池编号',
  `pool_name` varchar(128) NOT NULL COMMENT '复合核算池名称',
  `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户ID',
  `total_invest_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '共享投资人总出资',
  `total_return_amount` decimal(18,2) NOT NULL DEFAULT '0.00' comment '累计回本金额',
  `break_even_gap` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '回本缺口',
  `over_return_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '超额收益',
  `break_even_time` datetime DEFAULT NULL COMMENT '达到回本时间',
  `confirmed_time` datetime DEFAULT NULL COMMENT '财务确认回本时间',
  `confirmed_by` varchar(64) DEFAULT '' COMMENT '财务确认回本操作人',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态(0进行中 1已达回本 2已确认回本 3已关闭 4草稿)',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`pool_id`),
  UNIQUE KEY `uk_pool_no` (`pool_no`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='复合核算池主表';

-- -------------------------------------------------------------
-- 2. 复合池店面关系表(无 tenant_id,通过 pool_id 关联主表隔离)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `fin_composite_pool_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pool_id` bigint NOT NULL COMMENT '复合核算池ID',
  `dept_id` bigint NOT NULL COMMENT '店面ID',
  `dept_name` varchar(128) NOT NULL DEFAULT '' COMMENT '店面名称快照',
  `join_time` datetime DEFAULT NULL COMMENT '加入时间',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态(0有效 1停用)',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pool_dept` (`pool_id`,`dept_id`,`del_flag`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_pool_id` (`pool_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='复合池店面关系表';

-- -------------------------------------------------------------
-- 3. 复合池共享投资人表(无 tenant_id,通过 pool_id 关联主表隔离)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `fin_composite_pool_investor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pool_id` bigint NOT NULL COMMENT '复合核算池ID',
  `investor_id` bigint NOT NULL COMMENT '投资人ID',
  `investor_name` varchar(128) NOT NULL DEFAULT '' COMMENT '投资人姓名快照',
  `invest_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '该投资人共享出资款',
  `invest_ratio` decimal(10,4) NOT NULL DEFAULT '0.0000' COMMENT '出资占比',
  `returned_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '已按比例分摊回本金额',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态(0有效 1停用)',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pool_investor` (`pool_id`,`investor_id`,`del_flag`),
  KEY `idx_investor_id` (`investor_id`),
  KEY `idx_pool_id` (`pool_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='复合池共享投资人表';

-- -------------------------------------------------------------
-- 4. 复合核算周期纳入明细表(无 tenant_id,通过 pool_id 关联主表隔离)
--    全局唯一约束:同一周期在有效状态(status='0' and del_flag='0')下
--    只能被一个复合池纳入一次。撤销(status='1')或软删除(del_flag='2')
--    后,active_period_key 为 NULL,不再占用唯一名额,允许重新纳入。
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `fin_composite_period_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pool_id` bigint NOT NULL COMMENT '复合核算池ID',
  `dept_id` bigint NOT NULL COMMENT '店面ID',
  `period_id` bigint NOT NULL COMMENT '单店核算周期ID',
  `period_no` varchar(64) NOT NULL DEFAULT '' COMMENT '周期编号快照',
  `net_profit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '周期净利快照',
  `manager_profit_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '店长分润金额快照',
  `investor_profit_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '纳入复合核算金额',
  `included_mode` char(1) NOT NULL DEFAULT '0' COMMENT '纳入方式(0自动 1手动)',
  `included_time` datetime DEFAULT NULL COMMENT '纳入时间',
  `included_by` varchar(64) DEFAULT '' COMMENT '操作人',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态(0有效 1撤销)',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  -- 生成列:仅有效记录(status='0' and del_flag='0')时为 period_id,否则 NULL
  -- MySQL NULL 不参与唯一约束,实现"有效记录全局唯一,撤销/删除后可重新纳入"
  `active_period_key` bigint GENERATED ALWAYS AS
    (CASE WHEN `status` = '0' AND `del_flag` = '0' THEN `period_id` ELSE NULL END) VIRTUAL,
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_pool_period` (`pool_id`,`period_id`,`del_flag`),
  UNIQUE KEY `uk_active_period` (`active_period_key`),
  KEY `idx_pool_id` (`pool_id`),
  KEY `idx_period_id` (`period_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='复合核算周期纳入明细表';
