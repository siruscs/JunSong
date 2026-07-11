CREATE TABLE IF NOT EXISTS finance_cashflow_forecast_snapshot (
  snapshot_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '预测快照ID',
  dept_id BIGINT DEFAULT NULL COMMENT '门店ID，NULL表示授权范围聚合',
  forecast_date DATE NOT NULL COMMENT '预测日期',
  window_days INT NOT NULL COMMENT '预测窗口天数',
  forecast_receivable_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '预计回款金额',
  actual_receivable_amount DECIMAL(18,2) DEFAULT NULL COMMENT '实际回款金额',
  deviation_amount DECIMAL(18,2) DEFAULT NULL COMMENT '偏差金额',
  deviation_rate DECIMAL(10,4) DEFAULT NULL COMMENT '偏差率',
  pressure_score INT NOT NULL DEFAULT 0 COMMENT '现金压力分',
  pressure_level VARCHAR(16) NOT NULL DEFAULT 'LOW' COMMENT '压力等级',
  forecast_basis VARCHAR(1000) DEFAULT NULL COMMENT '预测依据',
  tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (snapshot_id),
  KEY idx_cashflow_forecast_snapshot_date (forecast_date, window_days),
  KEY idx_cashflow_forecast_snapshot_dept (dept_id, forecast_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='现金流预测快照表';

SET @financeRootId := (SELECT menu_id FROM sys_menu WHERE path = 'finance' AND menu_type = 'M' LIMIT 1);
SET @forecastMenuId := 2690;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT @forecastMenuId, '现金流预测', @financeRootId, 91, 'cashflowForecast', 'finance/cashflowForecast/index', '', '', 1, 0, 'C', '0', '0', 'finance:cashflowForecast:view', 'chart', 'admin', NOW(), '现金流预测'
WHERE @financeRootId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:cashflowForecast:view');

SET @forecastMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'finance:cashflowForecast:view' AND menu_type = 'C' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '生成预测快照', @forecastMenuId, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'finance:cashflowForecast:snapshot', '#', 'admin', NOW(), '生成预测快照'
WHERE @forecastMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:cashflowForecast:snapshot');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('finance:cashflowForecast:view', 'finance:cashflowForecast:snapshot')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );
