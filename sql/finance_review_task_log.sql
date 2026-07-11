-- finance_review_task_log.sql
-- 幂等创建复盘任务处理轨迹表
-- 回滚：DROP TABLE IF EXISTS finance_review_task_log;

CREATE TABLE IF NOT EXISTS finance_review_task_log (
  log_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  task_id BIGINT NOT NULL COMMENT '复盘任务ID',
  dept_id BIGINT NOT NULL COMMENT '门店ID',
  action_type VARCHAR(32) NOT NULL COMMENT 'IN_PROGRESS/DONE/IGNORED',
  before_status VARCHAR(32) DEFAULT NULL COMMENT '操作前状态',
  after_status VARCHAR(32) NOT NULL COMMENT '操作后状态',
  handler_id BIGINT DEFAULT NULL COMMENT '处理人ID',
  handler_name VARCHAR(64) DEFAULT NULL COMMENT '处理人姓名',
  handler_note VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
  action_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  KEY idx_review_task_log_task (task_id),
  KEY idx_review_task_log_dept_time (dept_id, action_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复盘任务处理轨迹';
