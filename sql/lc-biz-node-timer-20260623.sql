-- ============================================================
-- B-1: 低代码节点定时器配置表
-- 用于配置流程节点的边界定时器（审核超时自动取消 / 补材料限期等）
-- 场景：门店开业流程 - 大区总监审核 3 天超时取消 / 履约补材料 45 天限期
-- ============================================================

DROP TABLE IF EXISTS `lc_biz_node_timer`;
CREATE TABLE `lc_biz_node_timer` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_code`       VARCHAR(64)  NOT NULL                COMMENT '业务编码',
  `task_key`       VARCHAR(128) NOT NULL                COMMENT '绑定的用户任务 key（BPMN userTask id）',
  `task_name`      VARCHAR(128) DEFAULT NULL            COMMENT '任务名称（冗余展示用）',
  `duration`       VARCHAR(32)  NOT NULL                COMMENT 'ISO-8601 时长：P3D(3天)/P45D(45天)',
  `timeout_action` VARCHAR(32)  NOT NULL DEFAULT 'AUTO_CANCEL' COMMENT '超时动作：AUTO_CANCEL/AUTO_PASS/NOTIFY',
  `target_status`  VARCHAR(32) DEFAULT NULL            COMMENT '超时后业务状态：CANCELLED/EXPIRED 等',
  `del_flag`       CHAR(1)      DEFAULT '0'             COMMENT '删除标志(0存在 2删除)',
  `create_by`      VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`    DATETIME     DEFAULT NULL            COMMENT '创建时间',
  `update_by`      VARCHAR(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`    DATETIME     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz_code` (`biz_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码节点定时器配置';
