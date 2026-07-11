-- sys_governance_task_log.sql
-- 幂等创建系统治理任务处理轨迹表
-- 回滚：DROP TABLE IF EXISTS sys_governance_task_log;

CREATE TABLE IF NOT EXISTS sys_governance_task_log (
  log_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  task_type VARCHAR(64) NOT NULL COMMENT '治理任务类型',
  severity VARCHAR(16) DEFAULT NULL COMMENT '严重程度',
  count_value INT DEFAULT 0 COMMENT '关联数量',
  action_type VARCHAR(32) NOT NULL COMMENT 'ACK/DONE/IGNORED',
  handler_id BIGINT DEFAULT NULL COMMENT '处理人ID',
  handler_name VARCHAR(64) DEFAULT NULL COMMENT '处理人姓名',
  handler_note VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
  action_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  KEY idx_gov_task_log_type_time (task_type, action_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统治理任务处理轨迹';

-- 幂等补列：为已有表补充 tenant_id（多租户拦截器需要）
SET @colExists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_governance_task_log' AND COLUMN_NAME = 'tenant_id');
SET @sql := IF(@colExists = 0,
  'ALTER TABLE sys_governance_task_log ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 动态查找系统管理根菜单（path='system', menu_type='M', parent_id=0）
SET @sysRootId := (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0 LIMIT 1);
SET @sysRootId := COALESCE(@sysRootId, 1);

-- 1. 治理动作按钮权限（挂在系统管理根菜单下）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '治理动作', @sysRootId, 98, 'governance-action', '', 1, 0, 'F', '0', '0', 'system:dashboard:governance', '#', 'admin', sysdate(), '系统治理任务处理动作'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:dashboard:governance');

SET @govActionMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'system:dashboard:governance' LIMIT 1);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, @govActionMenuId FROM DUAL
WHERE @govActionMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = @govActionMenuId);

-- 2. 治理轨迹查询按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '治理轨迹', @sysRootId, 99, 'governance-log', '', 1, 0, 'F', '0', '0', 'system:governanceLog:list', '#', 'admin', sysdate(), '治理任务处理轨迹查询'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:governanceLog:list');

SET @govLogMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'system:governanceLog:list' LIMIT 1);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, @govLogMenuId FROM DUAL
WHERE @govLogMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = @govLogMenuId);
