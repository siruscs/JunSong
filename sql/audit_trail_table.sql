-- 审计快照表（幂等）
CREATE TABLE IF NOT EXISTS sys_audit_trail (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  module VARCHAR(50) NOT NULL COMMENT '模块: system/finance/member',
  action VARCHAR(100) NOT NULL COMMENT '操作类型',
  target_type VARCHAR(50) NOT NULL COMMENT '目标实体类型',
  target_id VARCHAR(64) NOT NULL COMMENT '目标实体ID',
  before_snapshot TEXT COMMENT '变更前JSON',
  after_snapshot TEXT COMMENT '变更后JSON',
  operator VARCHAR(64) COMMENT '操作人',
  dept_id BIGINT COMMENT '部门ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_module_action (module, action),
  INDEX idx_target (target_type, target_id),
  INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='高危操作审计快照';
