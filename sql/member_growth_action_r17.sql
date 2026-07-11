-- ============================================================
-- R17 会员增长动作闭环
-- 创建增长动作表、动作会员明细表，注册菜单与按钮权限
-- 幂等：所有 INSERT 均带 NOT EXISTS 保护，可重复执行
-- ============================================================

CREATE TABLE IF NOT EXISTS mem_growth_action (
  action_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '增长动作ID',
  action_no VARCHAR(64) NOT NULL COMMENT '动作编号',
  dept_id BIGINT DEFAULT NULL COMMENT '门店ID',
  dept_name VARCHAR(128) DEFAULT NULL COMMENT '门店名称',
  action_type VARCHAR(32) NOT NULL COMMENT '动作类型',
  action_title VARCHAR(128) NOT NULL COMMENT '动作标题',
  source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型',
  pressure_level VARCHAR(16) DEFAULT 'LOW' COMMENT '现金压力等级',
  candidate_count INT NOT NULL DEFAULT 0 COMMENT '候选会员数',
  executed_count INT NOT NULL DEFAULT 0 COMMENT '已执行会员数',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  action_reason VARCHAR(500) DEFAULT NULL COMMENT '动作原因',
  suggested_script VARCHAR(500) DEFAULT NULL COMMENT '建议话术',
  effect_window_days INT NOT NULL DEFAULT 7 COMMENT '效果观察窗口',
  tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
  del_flag CHAR(1) DEFAULT '0' COMMENT '删除标志',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (action_id),
  UNIQUE KEY uk_growth_action_no (action_no),
  KEY idx_growth_action_dept_time (dept_id, create_time),
  KEY idx_growth_action_status (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员增长动作表';

CREATE TABLE IF NOT EXISTS mem_growth_action_member (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  action_id BIGINT NOT NULL COMMENT '增长动作ID',
  member_id BIGINT NOT NULL COMMENT '会员ID',
  member_no VARCHAR(64) DEFAULT NULL COMMENT '会员编号',
  member_name VARCHAR(64) DEFAULT NULL COMMENT '会员姓名',
  dept_id BIGINT DEFAULT NULL COMMENT '门店ID',
  segment_type VARCHAR(32) NOT NULL COMMENT '候选分层',
  growth_value BIGINT DEFAULT 0 COMMENT '生成时成长值',
  card_type VARCHAR(64) DEFAULT NULL COMMENT '生成时等级',
  last_active_time DATETIME DEFAULT NULL COMMENT '生成时最后活跃时间',
  candidate_reason VARCHAR(500) DEFAULT NULL COMMENT '入选原因',
  execute_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '执行状态',
  execute_note VARCHAR(500) DEFAULT NULL COMMENT '执行备注',
  execute_time DATETIME DEFAULT NULL COMMENT '执行时间',
  repurchased CHAR(1) DEFAULT '0' COMMENT '观察期是否复购',
  signed_in CHAR(1) DEFAULT '0' COMMENT '观察期是否签到',
  growth_increased CHAR(1) DEFAULT '0' COMMENT '观察期成长值是否增长',
  tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_growth_action_member (action_id, member_id),
  KEY idx_growth_action_member_member (member_id, create_time),
  KEY idx_growth_action_member_status (execute_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员增长动作会员明细表';

-- 注册菜单与按钮权限
SET @memberRootId := (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'member' AND menu_type = 'M' LIMIT 1);
SET @growthActionMenuId := 2790;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT @growthActionMenuId, '会员增长动作', @memberRootId, 91, 'growthAction', 'member/growthAction/index', '', '', 1, 0, 'C', '0', '0', 'member:growthAction:view', 'peoples', 'admin', NOW(), '会员增长动作'
WHERE @memberRootId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:growthAction:view');

SET @growthActionMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'member:growthAction:view' AND menu_type = 'C' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '生成增长动作', @growthActionMenuId, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'member:growthAction:generate', '#', 'admin', NOW(), '生成增长动作'
WHERE @growthActionMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:growthAction:generate');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '执行增长动作', @growthActionMenuId, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'member:growthAction:execute', '#', 'admin', NOW(), '执行增长动作'
WHERE @growthActionMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:growthAction:execute');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '增长效果复盘', @growthActionMenuId, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'member:growthAction:effect', '#', 'admin', NOW(), '增长效果复盘'
WHERE @growthActionMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'member:growthAction:effect');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('member:growthAction:view', 'member:growthAction:generate', 'member:growthAction:execute', 'member:growthAction:effect')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );
