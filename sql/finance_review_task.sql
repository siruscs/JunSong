-- R3-A: 复盘任务持久化表
-- Idempotent: safe to run multiple times

CREATE TABLE IF NOT EXISTS finance_review_task (
  task_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  task_type VARCHAR(64) NOT NULL COMMENT '任务类型，对应诊断规则ID',
  dept_id BIGINT NOT NULL COMMENT '门店ID',
  dept_name VARCHAR(128) DEFAULT NULL COMMENT '门店名称',
  period_id BIGINT DEFAULT NULL COMMENT '核算周期ID',
  task_date DATE NOT NULL COMMENT '任务日期',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/DONE/IGNORED',
  severity VARCHAR(32) DEFAULT NULL COMMENT 'HIGH/MEDIUM/LOW',
  title VARCHAR(128) NOT NULL COMMENT '任务标题',
  reason VARCHAR(500) DEFAULT NULL COMMENT '触发原因',
  suggestion VARCHAR(500) DEFAULT NULL COMMENT '建议动作',
  impact_amount DECIMAL(18,2) DEFAULT 0 COMMENT '影响金额',
  handler_id BIGINT DEFAULT NULL COMMENT '处理人ID',
  handler_name VARCHAR(64) DEFAULT NULL COMMENT '处理人姓名',
  handler_note VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
  ignore_reason VARCHAR(500) DEFAULT NULL COMMENT '忽略原因',
  alert_id VARCHAR(160) NOT NULL COMMENT '去重Key',
  target_route VARCHAR(256) DEFAULT NULL COMMENT '跳转路由',
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  del_flag CHAR(1) DEFAULT '0',
  PRIMARY KEY (task_id),
  UNIQUE KEY uk_review_task_dedup (alert_id, task_date, del_flag),
  KEY idx_review_task_dept_status (dept_id, status, del_flag),
  KEY idx_review_task_period (period_id, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务复盘任务表';

-- Menu entries for review task management (perms-based, idempotent)
-- 复盘任务管理页面必须存在，默认只授权 role_id=1。
-- parent_id 使用财务根菜单，不存在时 fallback 到 108。
-- component 必须是 finance/reviewTask/index。
-- path 必须是 reviewTask，对应访问 /finance/reviewTask。
-- 权限包含 finance:reviewTask:list、finance:reviewTask:add、finance:reviewTask:edit。

SET @financeRootId := (SELECT menu_id FROM sys_menu WHERE path = 'finance' AND menu_type = 'M' LIMIT 1);
SET @financeRootId := COALESCE(@financeRootId, 108);

-- 复盘任务管理页面 (type=C menu)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '复盘任务管理', @financeRootId, 10, 'reviewTask', 'finance/reviewTask/index', 1, 0, 'C', '0', '0', 'finance:reviewTask:list', 'list', 'admin', sysdate(), '财务复盘任务管理页面'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:reviewTask:list' AND menu_type = 'C');

-- Fix: if reviewTask menu exists but parent_id is NULL, correct it to finance root
UPDATE sys_menu SET parent_id = @financeRootId
WHERE perms = 'finance:reviewTask:list' AND menu_type = 'C' AND parent_id IS NULL;

SET @reviewTaskMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'finance:reviewTask:list' AND menu_type = 'C' LIMIT 1);

-- Button: add (generate)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '生成复盘任务', @reviewTaskMenuId, 1, '', NULL, 1, 0, 'F', '0', '0', 'finance:reviewTask:add', '#', 'admin', sysdate(), '生成复盘任务权限'
FROM DUAL
WHERE @reviewTaskMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:reviewTask:add');

-- Button: edit (status transition)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '处理复盘任务', @reviewTaskMenuId, 2, '', NULL, 1, 0, 'F', '0', '0', 'finance:reviewTask:edit', '#', 'admin', sysdate(), '处理复盘任务权限'
FROM DUAL
WHERE @reviewTaskMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'finance:reviewTask:edit');

-- Grant review task permissions to admin only (role_id=1)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('finance:reviewTask:list', 'finance:reviewTask:add', 'finance:reviewTask:edit')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu e WHERE e.role_id = 1 AND e.menu_id = m.menu_id
  );

-- Remove any non-admin grants for review task permissions
DELETE rm FROM sys_role_menu rm
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE rm.role_id <> 1
  AND m.perms IN ('finance:reviewTask:list', 'finance:reviewTask:add', 'finance:reviewTask:edit');

-- Rollback (commented):
-- DELETE rm FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id = rm.menu_id WHERE m.perms IN ('finance:reviewTask:list','finance:reviewTask:add','finance:reviewTask:edit');
-- DELETE FROM sys_menu WHERE perms IN ('finance:reviewTask:list','finance:reviewTask:add','finance:reviewTask:edit');
-- DROP TABLE IF EXISTS finance_review_task;
