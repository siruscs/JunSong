-- R22: 动作中心与触达闭环
-- 幂等，可重复执行；不写入真实外部凭据。

CREATE TABLE IF NOT EXISTS sys_action_center_touch_log (
  log_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '触达日志ID',
  action_id VARCHAR(96) NOT NULL COMMENT '动作中心稳定ID',
  source_type VARCHAR(64) NOT NULL COMMENT '动作来源',
  source_id VARCHAR(64) NOT NULL COMMENT '来源业务ID',
  channel VARCHAR(32) NOT NULL COMMENT 'WEWORK_BOT',
  target_type VARCHAR(32) NOT NULL COMMENT 'GROUP/USER',
  target_ref VARCHAR(128) DEFAULT '' COMMENT '目标引用，不存敏感明文',
  touch_status VARCHAR(32) NOT NULL COMMENT 'DRY_RUN/SUCCESS/FAILED/SKIPPED_RATE_LIMIT/SKIPPED_DUPLICATE/DISABLED',
  request_digest VARCHAR(64) NOT NULL COMMENT '请求摘要，用于去重',
  message_summary VARCHAR(1000) DEFAULT NULL COMMENT '消息摘要',
  provider_response VARCHAR(2000) DEFAULT NULL COMMENT '通道响应摘要',
  error_message VARCHAR(2000) DEFAULT NULL COMMENT '失败原因',
  operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
  operator_name VARCHAR(64) DEFAULT '' COMMENT '操作人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (log_id),
  KEY idx_r22_touch_action_time (action_id, create_time),
  KEY idx_r22_touch_source_time (source_type, source_id, create_time),
  KEY idx_r22_touch_status_time (touch_status, create_time),
  KEY idx_r22_touch_digest_time (request_digest, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='R22动作触达日志';

CREATE TABLE IF NOT EXISTS sys_action_center_touch_throttle (
  throttle_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '节流ID',
  throttle_key VARCHAR(160) NOT NULL COMMENT '节流键：channel+target+source',
  channel VARCHAR(32) NOT NULL COMMENT '触达通道',
  target_ref VARCHAR(128) DEFAULT '' COMMENT '目标引用',
  last_touch_time DATETIME NOT NULL COMMENT '最近触达时间',
  touch_count_24h INT NOT NULL DEFAULT 0 COMMENT '近24小时触达次数',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (throttle_id),
  UNIQUE KEY uk_r22_touch_throttle_key (throttle_key),
  KEY idx_r22_touch_throttle_time (last_touch_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='R22触达节流记录';

-- DEV 默认 dry-run 配置。真实 webhook URL 必须由部署人员在环境中单独配置。
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'R22企业微信群机器人启用', 'r22.touch.wework.enabled', 'false', 'N', 'admin', NOW(), 'R22触达通道开关'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'r22.touch.wework.enabled');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'R22企业微信群机器人DryRun', 'r22.touch.wework.dryRun', 'true', 'N', 'admin', NOW(), 'DEV默认不真实发送'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'r22.touch.wework.dryRun');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'R22企业微信群机器人Webhook', 'r22.touch.wework.webhookUrl', '', 'N', 'admin', NOW(), '真实环境由部署人员配置'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'r22.touch.wework.webhookUrl');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'R22触达单目标24小时上限', 'r22.touch.rateLimit.perTarget24h', '3', 'N', 'admin', NOW(), '防刷屏'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'r22.touch.rateLimit.perTarget24h');

SET @systemParentId := (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0 LIMIT 1);
SET @menuId := GREATEST(3100, (SELECT IFNULL(MAX(menu_id) + 100, 3100) FROM sys_menu));

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT @menuId, '动作中心', COALESCE(@systemParentId, 0), 92, 'actionCenter', 'system/actionCenter/index', '', 'ActionCenter', 1, 0, 'C', '0', '0', 'system:action-center:view', 'connection', 'admin', NOW(), 'R22统一动作中心'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:action-center:view');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '动作中心查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'system:action-center:view' LIMIT 1),
       1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:action-center:query', '#', 'admin', NOW(), '动作中心查询按钮'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:action-center:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '动作中心触达',
       (SELECT menu_id FROM sys_menu WHERE perms = 'system:action-center:view' LIMIT 1),
       2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:action-center:touch', '#', 'admin', NOW(), '动作触达按钮'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:action-center:touch');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.perms IN ('system:action-center:view', 'system:action-center:query', 'system:action-center:touch')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
