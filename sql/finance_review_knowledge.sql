-- R11-F: 复盘知识库表
-- 幂等：CREATE TABLE IF NOT EXISTS

CREATE TABLE IF NOT EXISTS finance_review_knowledge (
  knowledge_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  task_id BIGINT DEFAULT NULL COMMENT '来源复盘任务ID',
  dept_id BIGINT DEFAULT NULL COMMENT '来源门店ID',
  problem_type VARCHAR(64) NOT NULL COMMENT '问题类型',
  title VARCHAR(128) NOT NULL COMMENT '知识标题',
  problem_summary VARCHAR(500) NOT NULL COMMENT '问题摘要',
  root_cause VARCHAR(500) DEFAULT NULL COMMENT '原因分析',
  action_taken VARCHAR(500) NOT NULL COMMENT '采取动作',
  result_summary VARCHAR(500) DEFAULT NULL COMMENT '效果摘要',
  reusable CHAR(1) NOT NULL DEFAULT '1' COMMENT '是否可复用',
  source_handler_name VARCHAR(64) DEFAULT NULL COMMENT '来源处理人',
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  KEY idx_review_knowledge_type (problem_type),
  KEY idx_review_knowledge_dept_time (dept_id, create_time),
  KEY idx_review_knowledge_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复盘知识库';
