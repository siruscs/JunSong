SET NAMES utf8mb4;
-- Workflow closed-loop migration: approval rounds and controlled orphan audit.
-- This script is additive and does not mutate existing task rows.
SET @column_sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lc_biz_instance' AND column_name = 'approval_round') = 0,
  'ALTER TABLE lc_biz_instance ADD COLUMN approval_round INT NOT NULL DEFAULT 1 COMMENT ''审批轮次''', 'SELECT 1');
PREPARE column_stmt FROM @column_sql; EXECUTE column_stmt; DEALLOCATE PREPARE column_stmt;
SET @column_sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lc_biz_instance' AND column_name = 'last_reject_reason') = 0,
  'ALTER TABLE lc_biz_instance ADD COLUMN last_reject_reason VARCHAR(500) DEFAULT NULL COMMENT ''最近驳回原因''', 'SELECT 1');
PREPARE column_stmt FROM @column_sql; EXECUTE column_stmt; DEALLOCATE PREPARE column_stmt;
SET @column_sql := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lc_biz_instance' AND column_name = 'last_reject_mode') = 0,
  'ALTER TABLE lc_biz_instance ADD COLUMN last_reject_mode VARCHAR(32) DEFAULT NULL COMMENT ''最近驳回策略''', 'SELECT 1');
PREPARE column_stmt FROM @column_sql; EXECUTE column_stmt; DEALLOCATE PREPARE column_stmt;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'lc_biz_instance'
    AND index_name = 'idx_lc_biz_instance_round');
SET @idx_sql := IF(@idx_exists = 0,
  'ALTER TABLE lc_biz_instance ADD KEY idx_lc_biz_instance_round (process_instance_id, approval_round)',
  'SELECT 1');
PREPARE stmt FROM @idx_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Read-only reconciliation output. Rows require explicit operator review before repair.
SELECT 'EMPTY_ASSIGNEE_TASK' issue_type, ID_, PROC_INST_ID_, TASK_DEF_KEY_, CREATE_TIME_
FROM ACT_RU_TASK WHERE ASSIGNEE_ IS NULL OR TRIM(ASSIGNEE_) = '';
SELECT 'EMPTY_ASSIGNEE_NOTIFICATION' issue_type, id, biz_id, create_time
FROM sys_notification WHERE (biz_id IS NULL OR TRIM(biz_id) = '') AND type LIKE 'wf_%';
