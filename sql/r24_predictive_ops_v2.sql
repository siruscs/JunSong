-- R24 Predictive Ops V2 (Explainable forecasting)
-- Adds prediction sample, factor and what-if simulation tables.
-- All tables are idempotent CREATE TABLE IF NOT EXISTS and indexed by tenant/dept.

CREATE TABLE IF NOT EXISTS finance_prediction_sample (
  sample_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '样本ID',
  tenant_id BIGINT DEFAULT NULL COMMENT '租户ID',
  dept_id BIGINT DEFAULT NULL COMMENT '门店/部门ID',
  prediction_type VARCHAR(32) NOT NULL COMMENT 'CASHFLOW/RECEIVABLE/MEMBER_ACTION/STOCK',
  source_id VARCHAR(64) DEFAULT NULL COMMENT '来源业务ID',
  sample_date DATE NOT NULL COMMENT '样本日期',
  window_days INT DEFAULT 7 COMMENT '预测窗口',
  score INT DEFAULT 0 COMMENT '可解释规则分',
  level VARCHAR(16) DEFAULT 'LOW' COMMENT 'LOW/MEDIUM/HIGH/CRITICAL',
  forecast_amount DECIMAL(18,2) DEFAULT 0.00 COMMENT '预测金额',
  actual_amount DECIMAL(18,2) DEFAULT 0.00 COMMENT '实际金额',
  deviation_amount DECIMAL(18,2) DEFAULT 0.00 COMMENT '偏差金额',
  deviation_rate DECIMAL(10,4) DEFAULT 0.0000 COMMENT '偏差率',
  basis VARCHAR(512) DEFAULT NULL COMMENT '预测口径说明',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (sample_id),
  KEY idx_r24_prediction_sample_scope (tenant_id, dept_id, prediction_type, sample_date),
  KEY idx_r24_prediction_sample_source (prediction_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='R24 预测样本';

CREATE TABLE IF NOT EXISTS finance_prediction_factor (
  factor_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '因子ID',
  sample_id BIGINT NOT NULL COMMENT '样本ID',
  factor_code VARCHAR(64) NOT NULL COMMENT '因子编码',
  factor_name VARCHAR(128) NOT NULL COMMENT '因子名称',
  factor_value VARCHAR(128) DEFAULT NULL COMMENT '因子值',
  factor_weight INT DEFAULT 0 COMMENT '因子分值',
  explanation VARCHAR(512) DEFAULT NULL COMMENT '解释文本',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (factor_id),
  KEY idx_r24_prediction_factor_sample (sample_id),
  KEY idx_r24_prediction_factor_code (factor_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='R24 预测解释因子';

CREATE TABLE IF NOT EXISTS finance_what_if_simulation (
  simulation_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '模拟ID',
  tenant_id BIGINT DEFAULT NULL COMMENT '租户ID',
  dept_id BIGINT DEFAULT NULL COMMENT '门店/部门ID',
  simulation_date DATE NOT NULL COMMENT '模拟日期',
  base_pressure_score INT DEFAULT 0 COMMENT '基线压力分',
  simulated_pressure_score INT DEFAULT 0 COMMENT '模拟后压力分',
  delta_score INT DEFAULT 0 COMMENT '变化分',
  input_json TEXT COMMENT '模拟输入',
  result_json TEXT COMMENT '模拟结果',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (simulation_id),
  KEY idx_r24_what_if_scope (tenant_id, dept_id, simulation_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='R24 what-if 模拟记录';

-- Menu and permission for R24 predictive ops dashboard
SET @financeRootId := (SELECT menu_id FROM sys_menu WHERE path = 'finance' AND menu_type = 'M' LIMIT 1);
SET @r24MenuId := 2700;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT @r24MenuId, '预测辅助V2', @financeRootId, 92, 'predictiveOps', 'finance/predictiveOps/index', '', '', 1, 0, 'C', '0', '0', 'finance:predictiveOps:view', 'data-analysis', 'admin', NOW(), 'R24 预测辅助V2 - 可解释规则和what-if模拟'
WHERE @financeRootId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:predictiveOps:view');

SET @r24MenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'finance:predictiveOps:view' AND menu_type = 'C' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '生成预测快照', @r24MenuId, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:predictiveOps:snapshot', '#', 'admin', NOW(), 'R24 生成预测快照'
WHERE @r24MenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:predictiveOps:snapshot');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'what-if模拟', @r24MenuId, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:predictiveOps:simulate', '#', 'admin', NOW(), 'R24 what-if模拟'
WHERE @r24MenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:predictiveOps:simulate');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('finance:predictiveOps:view', 'finance:predictiveOps:snapshot', 'finance:predictiveOps:simulate')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );
