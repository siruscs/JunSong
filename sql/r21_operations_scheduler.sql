-- R21: 经营调度执行日志表 + 菜单权限
-- 幂等，可重复执行

-- ==================== 日志表 ====================
CREATE TABLE IF NOT EXISTS sys_operation_schedule_log (
  log_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  job_code VARCHAR(64) NOT NULL COMMENT 'R21任务编码',
  job_name VARCHAR(128) NOT NULL COMMENT '任务名称',
  trigger_type VARCHAR(16) NOT NULL COMMENT 'MANUAL/SCHEDULED',
  status VARCHAR(16) NOT NULL COMMENT 'SUCCESS/FAILED/SKIPPED/PARTIAL',
  started_at DATETIME NOT NULL COMMENT '开始时间',
  finished_at DATETIME NULL COMMENT '结束时间',
  duration_ms BIGINT NULL COMMENT '耗时毫秒',
  affected_rows INT NOT NULL DEFAULT 0 COMMENT '影响行数',
  result_summary VARCHAR(1000) NULL COMMENT '结果摘要',
  error_message VARCHAR(2000) NULL COMMENT '错误信息',
  tenant_id BIGINT DEFAULT 0,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (log_id),
  KEY idx_r21_schedule_job_time (job_code, started_at),
  KEY idx_r21_schedule_status_time (status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='R21经营调度执行日志';

-- ==================== 菜单权限 ====================

-- 动态查找系统管理父菜单
SET @systemParentId := (SELECT menu_id FROM sys_menu WHERE path = 'system' AND menu_type = 'M' AND parent_id = 0 LIMIT 1);

-- 动态分配 menu_id：取当前最大值 + 200，保底 2900（避免与 R20 的 2800+ 冲突）
SET @menuId := GREATEST(2900, (SELECT IFNULL(MAX(menu_id) + 200, 2900) FROM sys_menu));

-- 插入经营调度看板页面菜单 (C)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT @menuId, '经营调度', COALESCE(@systemParentId, 0), 91, 'operationScheduler', 'system/operationScheduler/index', '', 'OperationScheduler', 1, 0, 'C', '0', '0', 'system:operation-scheduler:view', 'job', 'admin', NOW(), 'R21经营调度看板'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:operation-scheduler:view');

-- 插入查询按钮权限 (F)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '经营调度查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'system:operation-scheduler:view' LIMIT 1),
       1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:operation-scheduler:query', '#', 'admin', NOW(), '经营调度查询按钮'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:operation-scheduler:query');

-- 插入手动触发按钮权限 (F)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '经营调度触发',
       (SELECT menu_id FROM sys_menu WHERE perms = 'system:operation-scheduler:view' LIMIT 1),
       2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:operation-scheduler:trigger', '#', 'admin', NOW(), '经营调度手动触发按钮'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:operation-scheduler:trigger');

-- 仅授权 role_id = 1
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.perms IN ('system:operation-scheduler:view', 'system:operation-scheduler:query', 'system:operation-scheduler:trigger')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
