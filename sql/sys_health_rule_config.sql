-- R10-A: 自检规则配置表
-- DROP TABLE IF EXISTS sys_health_rule_config;

CREATE TABLE IF NOT EXISTS sys_health_rule_config (
  rule_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  rule_code VARCHAR(64) NOT NULL COMMENT '规则编码',
  rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
  rule_domain VARCHAR(32) NOT NULL COMMENT '规则领域 FINANCE/MEMBER/SYSTEM/STORE',
  metric_key VARCHAR(64) NOT NULL COMMENT '指标键',
  compare_op VARCHAR(16) NOT NULL COMMENT '比较符 GT/GTE/LT/LTE/EQ',
  threshold_value DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '阈值',
  severity VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' COMMENT 'HIGH/MEDIUM/LOW',
  enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT '是否启用 1启用 0停用',
  notify_enabled CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否通知 1是 0否',
  dept_scope VARCHAR(32) NOT NULL DEFAULT 'AUTHORIZED' COMMENT '适用范围 AUTHORIZED/ALL',
  suggestion VARCHAR(500) DEFAULT NULL COMMENT '处理建议',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (rule_id),
  UNIQUE KEY uk_health_rule_tenant_code (tenant_id, rule_code),
  KEY idx_health_rule_domain_enabled (rule_domain, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自检规则配置';

-- 初始化8条核心规则（幂等）
INSERT INTO sys_health_rule_config
(rule_code, rule_name, rule_domain, metric_key, compare_op, threshold_value, severity, enabled, notify_enabled, suggestion, sort_order, create_by, create_time)
SELECT 'FIN_REVIEW_FIRST_RESPONSE_HOURS', '复盘首次响应超时', 'FINANCE', 'avgFirstResponseHours', 'GT', 24, 'HIGH', '1', '1', '复盘任务超过24小时未响应，请优先分派处理人。', 10, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_health_rule_config WHERE rule_code = 'FIN_REVIEW_FIRST_RESPONSE_HOURS');

INSERT INTO sys_health_rule_config
(rule_code, rule_name, rule_domain, metric_key, compare_op, threshold_value, severity, enabled, notify_enabled, suggestion, sort_order, create_by, create_time)
SELECT 'FIN_REVIEW_CLOSE_HOURS', '复盘关闭超时', 'FINANCE', 'avgCloseHours', 'GT', 72, 'HIGH', '1', '1', '复盘任务超过72小时未关闭，请确认阻塞原因。', 20, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_health_rule_config WHERE rule_code = 'FIN_REVIEW_CLOSE_HOURS');

INSERT INTO sys_health_rule_config
(rule_code, rule_name, rule_domain, metric_key, compare_op, threshold_value, severity, enabled, notify_enabled, suggestion, sort_order, create_by, create_time)
SELECT 'FIN_REVIEW_OVERDUE_RATIO', '复盘逾期比例过高', 'FINANCE', 'overdueRatio', 'GT', 20, 'HIGH', '1', '1', '逾期复盘比例超过20%，请减少未关闭任务堆积。', 30, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_health_rule_config WHERE rule_code = 'FIN_REVIEW_OVERDUE_RATIO');

INSERT INTO sys_health_rule_config
(rule_code, rule_name, rule_domain, metric_key, compare_op, threshold_value, severity, enabled, notify_enabled, suggestion, sort_order, create_by, create_time)
SELECT 'SYS_LOGIN_FAIL_24H', '24小时登录失败过高', 'SYSTEM', 'recentLoginFailCount', 'GT', 20, 'HIGH', '1', '1', '登录失败次数过高，请检查异常账号和IP。', 40, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_health_rule_config WHERE rule_code = 'SYS_LOGIN_FAIL_24H');

INSERT INTO sys_health_rule_config
(rule_code, rule_name, rule_domain, metric_key, compare_op, threshold_value, severity, enabled, notify_enabled, suggestion, sort_order, create_by, create_time)
SELECT 'SYS_EMPTY_MENU_COUNT', '空组件菜单', 'SYSTEM', 'emptyMenuCount', 'GT', 0, 'HIGH', '1', '0', '存在菜单组件路径为空，可能导致白屏。', 50, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_health_rule_config WHERE rule_code = 'SYS_EMPTY_MENU_COUNT');

INSERT INTO sys_health_rule_config
(rule_code, rule_name, rule_domain, metric_key, compare_op, threshold_value, severity, enabled, notify_enabled, suggestion, sort_order, create_by, create_time)
SELECT 'MEM_MEMBER_LINK_QUALITY', '会员销售精确关联率偏低', 'MEMBER', 'memberLinkQualityRate', 'LT', 80, 'MEDIUM', '1', '0', '会员销售精确关联率低于80%，建议销售录入时选择会员。', 60, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_health_rule_config WHERE rule_code = 'MEM_MEMBER_LINK_QUALITY');

INSERT INTO sys_health_rule_config
(rule_code, rule_name, rule_domain, metric_key, compare_op, threshold_value, severity, enabled, notify_enabled, suggestion, sort_order, create_by, create_time)
SELECT 'MEM_POINTS_LIABILITY', '积分负债偏高', 'MEMBER', 'pointsLiability', 'GT', 1000, 'MEDIUM', '1', '0', '积分余额折算金额偏高，请关注兑换压力。', 70, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_health_rule_config WHERE rule_code = 'MEM_POINTS_LIABILITY');

INSERT INTO sys_health_rule_config
(rule_code, rule_name, rule_domain, metric_key, compare_op, threshold_value, severity, enabled, notify_enabled, suggestion, sort_order, create_by, create_time)
SELECT 'STORE_CASHFLOW_NEGATIVE', '门店净现金流为负', 'STORE', 'netCashflowAmount', 'LT', 0, 'HIGH', '1', '1', '门店净现金流为负，请优先复核实收与费用支出。', 80, 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_health_rule_config WHERE rule_code = 'STORE_CASHFLOW_NEGATIVE');
