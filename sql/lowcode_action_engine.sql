-- sql/lowcode_action_engine.sql

-- ----------------------------
-- 1. 动作定义表 lc_biz_action
-- ----------------------------
DROP TABLE IF EXISTS `lc_biz_action`;
CREATE TABLE `lc_biz_action` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_code`        VARCHAR(64)  NOT NULL                COMMENT '业务编码',
  `action_code`     VARCHAR(64)  NOT NULL                COMMENT '动作编码(SUBMIT/WITHDRAW/FULFILL/EXPORT/VIEW_FLOW/VIEW_HISTORY)',
  `action_name`     VARCHAR(64)  NOT NULL                COMMENT '动作名称(按钮文字)',
  `action_type`     VARCHAR(20)  NOT NULL DEFAULT 'BUILTIN' COMMENT '动作类型(BUILTIN内置/API自定义)',
  `trigger_status`  VARCHAR(200) NOT NULL                COMMENT '触发状态(逗号分隔,如 DRAFT,REJECTED)',
  `api_endpoint`    VARCHAR(500) DEFAULT NULL            COMMENT 'API端点(BUILTIN为预设,API为自定义路径)',
  `button_style`    VARCHAR(20)  DEFAULT 'primary'       COMMENT '按钮样式(primary/success/warning/danger/info)',
  `button_icon`     VARCHAR(50)  DEFAULT NULL            COMMENT '按钮图标',
  `sort_order`      INT          DEFAULT 0                COMMENT '排序',
  `status`          CHAR(1)      DEFAULT '0'              COMMENT '状态(0启用 1停用)',
  `del_flag`        CHAR(1)      DEFAULT '0'              COMMENT '删除标志(0存在 2删除)',
  `create_by`       VARCHAR(64)  DEFAULT '',
  `create_time`     DATETIME     DEFAULT NULL,
  `update_by`       VARCHAR(64)  DEFAULT '',
  `update_time`     DATETIME     DEFAULT NULL,
  `remark`          VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lc_action_biz_code` (`biz_code`, `action_code`, `del_flag`),
  KEY `idx_lc_action_biz_code` (`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码业务动作定义';

-- ----------------------------
-- 2. 后置动作表 lc_biz_post_action
-- ----------------------------
DROP TABLE IF EXISTS `lc_biz_post_action`;
CREATE TABLE `lc_biz_post_action` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_code`        VARCHAR(64)  NOT NULL                COMMENT '业务编码',
  `trigger_event`   VARCHAR(30)  NOT NULL                COMMENT '触发事件(AFTER_SUBMIT/AFTER_APPROVE/AFTER_REJECT/AFTER_WITHDRAW/AFTER_FULFILL)',
  `action_type`     VARCHAR(20)  NOT NULL                COMMENT '动作类型(UPDATE_FIELD/UPDATE_STATUS/SEND_MESSAGE/CALL_SERVICE)',
  `target_field`    VARCHAR(100) DEFAULT NULL            COMMENT '目标字段(UPDATE_FIELD用)',
  `target_value`    VARCHAR(200) DEFAULT NULL            COMMENT '目标值(UPDATE_FIELD/UPDATE_STATUS用)',
  `condition_expr`  VARCHAR(500) DEFAULT NULL            COMMENT '条件表达式(为空则无条件执行)',
  `callback_url`    VARCHAR(500) DEFAULT NULL            COMMENT '回调URL(CALL_SERVICE用)',
  `sort_order`      INT          DEFAULT 0                COMMENT '排序',
  `status`          CHAR(1)      DEFAULT '0'              COMMENT '状态(0启用 1停用)',
  `del_flag`        CHAR(1)      DEFAULT '0'              COMMENT '删除标志(0存在 2删除)',
  `create_by`       VARCHAR(64)  DEFAULT '',
  `create_time`     DATETIME     DEFAULT NULL,
  `update_by`       VARCHAR(64)  DEFAULT '',
  `update_time`     DATETIME     DEFAULT NULL,
  `remark`          VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_lc_post_action_biz_code` (`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码业务后置动作';

-- ----------------------------
-- 3. 种子数据：按需通过低代码管理后台配置
-- ----------------------------
