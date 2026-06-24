-- 低代码-业务配置模板表
CREATE TABLE IF NOT EXISTS `lc_biz_template` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_code`   VARCHAR(64)  NOT NULL                COMMENT '模板编码（唯一）',
  `template_name`   VARCHAR(128) NOT NULL                COMMENT '模板名称',
  `category`        VARCHAR(32)  DEFAULT NULL            COMMENT '分类：APPROVAL审批单/PURCHASE采购单/REFUND退款单',
  `description`     VARCHAR(500) DEFAULT NULL             COMMENT '模板描述/说明',
  `thumbnail`       VARCHAR(256) DEFAULT NULL             COMMENT '缩略图URL（预留）',
  `process_key`     VARCHAR(64)  DEFAULT NULL             COMMENT '关联流程Key',
  `config_json`     LONGTEXT     NOT NULL                 COMMENT 'LcBizConfigDTO 完整 JSON',
  `usage_count`     INT          DEFAULT 0                COMMENT '被克隆次数',
  `is_starter`      CHAR(1)      DEFAULT '0'              COMMENT '是否内置模板：0否 1是',
  `del_flag`        CHAR(1)      DEFAULT '0'               COMMENT '删除标志',
  `create_by`       VARCHAR(64)  DEFAULT '',
  `create_time`     DATETIME     DEFAULT NULL,
  `update_by`       VARCHAR(64)  DEFAULT '',
  `update_time`     DATETIME     DEFAULT NULL,
  `remark`          VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码-业务配置模板';

