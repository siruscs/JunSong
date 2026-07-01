-- ================================================================
-- 开放平台 ISV 注册 + 合约管理 DDL
-- 执行环境：MySQL 8.x
-- ================================================================

-- ISV 注册表
CREATE TABLE IF NOT EXISTS `open_isv` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`        BIGINT       DEFAULT NULL            COMMENT '租户ID',
  `isv_name`         VARCHAR(100) NOT NULL DEFAULT ''     COMMENT 'ISV名称',
  `contact_name`     VARCHAR(50)  DEFAULT ''              COMMENT '联系人',
  `contact_phone`    VARCHAR(20)  DEFAULT ''              COMMENT '联系电话',
  `contact_email`    VARCHAR(100) DEFAULT ''              COMMENT '联系邮箱',
  `company_name`     VARCHAR(200) DEFAULT ''              COMMENT '公司名称',
  `business_license` VARCHAR(50)  DEFAULT ''              COMMENT '统一社会信用代码',
  `website_url`      VARCHAR(300) DEFAULT ''              COMMENT '网站地址',
  `access_type`      VARCHAR(30)  DEFAULT 'merchant'      COMMENT '接入类型(merchant/service_provider/internal)',
  `status`           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '审核状态(PENDING/APPROVED/REJECTED)',
  `reject_reason`    VARCHAR(500) DEFAULT ''              COMMENT '驳回原因',
  `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_isv_status` (`status`),
  KEY `idx_isv_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ISV注册表';

-- 开放平台合约表
CREATE TABLE IF NOT EXISTS `open_contract` (
  `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `contract_no`          VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '合约编号',
  `isv_id`               BIGINT       NOT NULL                COMMENT 'ISV-ID',
  `isv_name`             VARCHAR(100) DEFAULT ''              COMMENT 'ISV名称(冗余)',
  `contract_type`        VARCHAR(20)  DEFAULT 'standard'      COMMENT '合约类型(standard/custom)',
  `title`                VARCHAR(200) DEFAULT ''              COMMENT '合约标题',
  `terms`                TEXT                                 COMMENT '合约条款(JSON)',
  `start_date`           DATE         DEFAULT NULL            COMMENT '生效日期',
  `end_date`             DATE         DEFAULT NULL            COMMENT '到期日期',
  `daily_quota`          INT          DEFAULT 1000            COMMENT '日调用配额',
  `allowed_capabilities` VARCHAR(500) DEFAULT ''              COMMENT '可用能力(逗号分隔: member,workflow,request,foundation)',
  `status`               VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '合约状态(DRAFT/ACTIVE/EXPIRING/EXPIRED/TERMINATED)',
  `create_time`          DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`          DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_contract_isv` (`isv_id`),
  KEY `idx_contract_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放平台合约表';
