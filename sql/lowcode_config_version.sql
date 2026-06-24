-- sql/lowcode_config_version.sql

-- ----------------------------
-- 1. lc_biz_object 表扩展：配置状态 + 已发布版本号
-- ----------------------------
ALTER TABLE `lc_biz_object`
  ADD COLUMN `config_status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT'
      COMMENT '配置状态: DRAFT草稿/PUBLISHED已发布' AFTER `status`,
  ADD COLUMN `published_version` INT NOT NULL DEFAULT 0
      COMMENT '已发布版本号(0表示从未发布)' AFTER `config_status`;

-- ----------------------------
-- 2. 配置发布快照表
-- ----------------------------
DROP TABLE IF EXISTS `lc_biz_config_snapshot`;
CREATE TABLE `lc_biz_config_snapshot` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_code`        VARCHAR(64)  NOT NULL                COMMENT '业务编码',
  `version_no`      INT          NOT NULL                COMMENT '版本号(从1递增)',
  `config_json`     LONGTEXT     NOT NULL                COMMENT 'LcBizConfigDTO 完整 JSON 快照',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'PUBLISHED' COMMENT '快照状态(PUBLISHED)',
  `publish_remark`  VARCHAR(500) DEFAULT NULL            COMMENT '发布说明',
  `del_flag`        CHAR(1)      DEFAULT '0'              COMMENT '删除标志(0存在 2删除)',
  `create_by`       VARCHAR(64)  DEFAULT '',
  `create_time`     DATETIME     DEFAULT NULL,
  `update_by`       VARCHAR(64)  DEFAULT '',
  `update_time`     DATETIME     DEFAULT NULL,
  `remark`          VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_version` (`biz_code`, `version_no`),
  KEY `idx_biz_status` (`biz_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码-配置发布快照';

