-- ============================================================
-- 注册功能增强 DDL
-- 1. sys_user 表新增 id_card 字段
-- 2. 新增 sys_invite_code 邀请码表
-- 3. sys_config 新增注册相关参数
-- ============================================================

-- 1. sys_user 表新增 id_card 字段（身份证号）
ALTER TABLE `sys_user` ADD COLUMN `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号' AFTER `phonenumber`;

-- 2. 新增 sys_invite_code 邀请码表
CREATE TABLE `sys_invite_code` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`            VARCHAR(32)  NOT NULL COMMENT '邀请码',
  `inviter_user_id` BIGINT       NOT NULL COMMENT '邀请人用户ID',
  `inviter_name`    VARCHAR(64)  DEFAULT NULL COMMENT '邀请人用户名',
  `status`          CHAR(1)      DEFAULT '0' COMMENT '状态（0未使用 1已使用 2已失效）',
  `invitee_user_id` BIGINT       DEFAULT NULL COMMENT '被邀请人用户ID',
  `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `use_time`        DATETIME     DEFAULT NULL COMMENT '使用时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_inviter` (`inviter_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户邀请码表';

-- 3. sys_config 新增注册相关参数
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`) VALUES
('注册-身份证必填', 'sys.account.registerIdCardRequired', 'false', 'Y', 'admin', NOW(), '注册时是否必填身份证号（true必填 false选填）'),
('注册-需要邀请码', 'sys.account.registerInviteCode', 'false', 'Y', 'admin', NOW(), '注册时是否需要邀请码（true需要 false不需要）'),
('注册-新用户需审核', 'sys.account.registerNeedAudit', 'true', 'Y', 'admin', NOW(), '新注册用户是否需要管理员审核（true需要 false直接激活）');
