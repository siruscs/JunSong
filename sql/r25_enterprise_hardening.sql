-- R25: 企业级硬化（操作审计快照 / 数据留存归档 / 操作告警闭环）
-- 幂等，可重复执行；不写入真实外部凭据。

-- ============================================================
-- 1. 操作审计快照表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_operation_audit_snapshot (
  audit_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '审计快照ID',
  tenant_id BIGINT DEFAULT NULL COMMENT '租户ID',
  biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
  biz_id VARCHAR(128) NOT NULL COMMENT '业务ID',
  operation VARCHAR(64) NOT NULL COMMENT '操作类型',
  risk_level VARCHAR(16) DEFAULT 'MEDIUM' COMMENT '风险级别 HIGH/MEDIUM/LOW',
  before_snapshot TEXT COMMENT '变更前快照JSON',
  after_snapshot TEXT COMMENT '变更后快照JSON',
  diff_summary VARCHAR(1024) DEFAULT '' COMMENT '差异摘要',
  operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
  operator_name VARCHAR(64) DEFAULT '' COMMENT '操作人',
  request_ip VARCHAR(64) DEFAULT '' COMMENT '请求IP',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (audit_id),
  KEY idx_r25_audit_scope (tenant_id, biz_type, biz_id, create_time),
  KEY idx_r25_audit_risk (risk_level, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='R25操作审计快照';

-- ============================================================
-- 2. 数据留存策略表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_data_retention_policy (
  policy_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '策略ID',
  table_name VARCHAR(128) NOT NULL COMMENT '表名',
  retention_days INT NOT NULL COMMENT '留存天数',
  archive_mode VARCHAR(32) DEFAULT 'SUMMARY_ONLY' COMMENT '归档模式 SUMMARY_ONLY/SOFT_ARCHIVE/DISABLED',
  enabled CHAR(1) DEFAULT '1' COMMENT '是否启用 1是 0否',
  remark VARCHAR(512) DEFAULT '' COMMENT '备注',
  create_by VARCHAR(64) DEFAULT 'admin' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (policy_id),
  UNIQUE KEY uk_r25_retention_table (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='R25数据留存策略';

-- ============================================================
-- 3. 数据归档执行记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_data_archive_run (
  run_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '执行ID',
  policy_id BIGINT DEFAULT NULL COMMENT '策略ID',
  table_name VARCHAR(128) NOT NULL COMMENT '表名',
  cutoff_time DATETIME NOT NULL COMMENT '归档截止时间',
  candidate_count BIGINT DEFAULT 0 COMMENT '候选数据量',
  archived_count BIGINT DEFAULT 0 COMMENT '已归档数据量',
  dry_run CHAR(1) DEFAULT '1' COMMENT '是否试运行 1是 0否',
  status VARCHAR(16) DEFAULT 'SUCCESS' COMMENT '执行状态 SUCCESS/FAILED',
  error_message VARCHAR(1024) DEFAULT '' COMMENT '错误信息',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (run_id),
  KEY idx_r25_archive_table_time (table_name, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='R25数据归档执行记录';

-- ============================================================
-- 4. 操作告警规则表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_operation_alert_rule (
  rule_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  rule_key VARCHAR(128) NOT NULL COMMENT '规则键',
  rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
  source_type VARCHAR(64) NOT NULL COMMENT '来源类型',
  severity VARCHAR(16) DEFAULT 'MEDIUM' COMMENT '严重级别 HIGH/MEDIUM/LOW/CRITICAL',
  threshold_json VARCHAR(1024) DEFAULT '{}' COMMENT '阈值配置JSON',
  enabled CHAR(1) DEFAULT '1' COMMENT '是否启用 1是 0否',
  create_by VARCHAR(64) DEFAULT 'admin' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (rule_id),
  UNIQUE KEY uk_r25_alert_rule_key (rule_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='R25操作告警规则';

-- ============================================================
-- 5. 操作告警事件表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_operation_alert_event (
  event_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '事件ID',
  rule_key VARCHAR(128) NOT NULL COMMENT '规则键',
  dedup_key VARCHAR(256) NOT NULL COMMENT '去重键',
  source_type VARCHAR(64) NOT NULL COMMENT '来源类型',
  source_id VARCHAR(128) DEFAULT '' COMMENT '来源业务ID',
  severity VARCHAR(16) DEFAULT 'MEDIUM' COMMENT '严重级别',
  status VARCHAR(16) DEFAULT 'OPEN' COMMENT '状态 OPEN/ACKED/RESOLVED',
  title VARCHAR(256) NOT NULL COMMENT '告警标题',
  content VARCHAR(1024) DEFAULT '' COMMENT '告警内容',
  first_seen_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次发现时间',
  last_seen_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最近发现时间',
  hit_count INT DEFAULT 1 COMMENT '命中次数',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (event_id),
  UNIQUE KEY uk_r25_alert_dedup (dedup_key),
  KEY idx_r25_alert_status (status, severity, last_seen_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='R25操作告警事件';

-- ============================================================
-- 默认留存策略（幂等）
-- ============================================================
INSERT INTO sys_data_retention_policy (table_name, retention_days, archive_mode, enabled, remark, create_by, create_time)
SELECT 'sys_oper_log', 180, 'SUMMARY_ONLY', '1', '操作日志默认留存180天', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_retention_policy WHERE table_name = 'sys_oper_log');

INSERT INTO sys_data_retention_policy (table_name, retention_days, archive_mode, enabled, remark, create_by, create_time)
SELECT 'sys_logininfor', 180, 'SUMMARY_ONLY', '1', '登录日志默认留存180天', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_retention_policy WHERE table_name = 'sys_logininfor');

INSERT INTO sys_data_retention_policy (table_name, retention_days, archive_mode, enabled, remark, create_by, create_time)
SELECT 'sys_notification', 180, 'SOFT_ARCHIVE', '1', '系统通知默认留存180天', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_retention_policy WHERE table_name = 'sys_notification');

INSERT INTO sys_data_retention_policy (table_name, retention_days, archive_mode, enabled, remark, create_by, create_time)
SELECT 'sys_action_center_touch_log', 180, 'SOFT_ARCHIVE', '1', '动作触达日志默认留存180天', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_retention_policy WHERE table_name = 'sys_action_center_touch_log');

INSERT INTO sys_data_retention_policy (table_name, retention_days, archive_mode, enabled, remark, create_by, create_time)
SELECT 'sys_operation_schedule_log', 90, 'SOFT_ARCHIVE', '1', '调度日志默认留存90天', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_retention_policy WHERE table_name = 'sys_operation_schedule_log');

INSERT INTO sys_data_retention_policy (table_name, retention_days, archive_mode, enabled, remark, create_by, create_time)
SELECT 'finance_prediction_sample', 365, 'SUMMARY_ONLY', '1', '预测样本默认留存365天', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_retention_policy WHERE table_name = 'finance_prediction_sample');

INSERT INTO sys_data_retention_policy (table_name, retention_days, archive_mode, enabled, remark, create_by, create_time)
SELECT 'finance_what_if_simulation', 365, 'SUMMARY_ONLY', '1', 'what-if模拟默认留存365天', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_retention_policy WHERE table_name = 'finance_what_if_simulation');

INSERT INTO sys_data_retention_policy (table_name, retention_days, archive_mode, enabled, remark, create_by, create_time)
SELECT 'open_api_log', 180, 'SUMMARY_ONLY', '1', '开放API日志默认留存180天', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_retention_policy WHERE table_name = 'open_api_log');

INSERT INTO sys_data_retention_policy (table_name, retention_days, archive_mode, enabled, remark, create_by, create_time)
SELECT 'open_webhook_subscription', 365, 'DISABLED', '1', 'webhook订阅默认不自动归档', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_data_retention_policy WHERE table_name = 'open_webhook_subscription');

-- ============================================================
-- 菜单与按钮权限（幂等）
-- ============================================================
SET @systemRootId := (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0 LIMIT 1);
SET @r25MenuId := 2800;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT @r25MenuId, '企业级硬化', COALESCE(@systemRootId, 0), 95, 'hardening', 'system/hardening/index', '', 'EnterpriseHardening', 1, 0, 'C', '0', '0', 'system:hardening:view', 'lock', 'admin', NOW(), 'R25企业级硬化看板'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:hardening:view');

SET @r25MenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'system:hardening:view' AND menu_type = 'C' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '审计快照查询', @r25MenuId, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:hardening:audit', '#', 'admin', NOW(), 'R25审计快照查询按钮'
WHERE @r25MenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:hardening:audit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据归档执行', @r25MenuId, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:hardening:archive', '#', 'admin', NOW(), 'R25数据归档执行按钮'
WHERE @r25MenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:hardening:archive');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '告警事件处理', @r25MenuId, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:hardening:alert', '#', 'admin', NOW(), 'R25告警事件处理按钮'
WHERE @r25MenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:hardening:alert');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('system:hardening:view', 'system:hardening:audit', 'system:hardening:archive', 'system:hardening:alert')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );
