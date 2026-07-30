SET NAMES utf8mb4;
-- 工作流孤儿/空处理人安全修复脚本。
-- 默认只读（@apply_repair=0）。必须由 DBA 明确设置为 1，并先保存查询结果。
SET @apply_repair := 0;

DROP TEMPORARY TABLE IF EXISTS tmp_workflow_orphan_candidates;
CREATE TEMPORARY TABLE tmp_workflow_orphan_candidates AS
SELECT t.ID_ task_id, t.PROC_INST_ID_ process_instance_id, t.TASK_DEF_KEY_ task_definition_key,
       h.recovered_username
FROM ACT_RU_TASK t
JOIN ACT_RU_EXECUTION e ON e.ID_ = t.PROC_INST_ID_ AND e.PARENT_ID_ IS NULL
JOIN (
  SELECT PROC_INST_ID_, TASK_DEF_KEY_, MAX(END_TIME_) latest_end_time, MAX(ASSIGNEE_) recovered_username
  FROM ACT_HI_TASKINST
  WHERE END_TIME_ IS NOT NULL AND ASSIGNEE_ IS NOT NULL AND TRIM(ASSIGNEE_) <> ''
  GROUP BY PROC_INST_ID_, TASK_DEF_KEY_
) h ON h.PROC_INST_ID_ = t.PROC_INST_ID_ AND h.TASK_DEF_KEY_ = t.TASK_DEF_KEY_
WHERE (t.ASSIGNEE_ IS NULL OR TRIM(t.ASSIGNEE_) = '')
  AND h.recovered_username NOT REGEXP '^[0-9]+$';

SELECT 'WORKFLOW_ORPHAN_CANDIDATE' issue_type, task_id, process_instance_id,
       task_definition_key, recovered_username
FROM tmp_workflow_orphan_candidates;

CREATE TEMPORARY TABLE tmp_workflow_orphan_reconcile AS
SELECT * FROM tmp_workflow_orphan_candidates;

-- 仅修复“同实例、同节点、历史已有非数字 username”的确定性候选；其余孤儿任务保持不变。
UPDATE ACT_RU_TASK t
JOIN tmp_workflow_orphan_candidates c ON c.task_id = t.ID_
SET t.ASSIGNEE_ = c.recovered_username
WHERE @apply_repair = 1
  AND (t.ASSIGNEE_ IS NULL OR TRIM(t.ASSIGNEE_) = '');

SELECT 'WORKFLOW_ORPHAN_REPAIR_RECONCILIATION' result_type,
       @apply_repair applied,
       (SELECT COUNT(*) FROM tmp_workflow_orphan_candidates) candidates,
       (SELECT COUNT(*) FROM ACT_RU_TASK t JOIN tmp_workflow_orphan_reconcile c ON c.task_id = t.ID_
         WHERE t.ASSIGNEE_ IS NULL OR TRIM(t.ASSIGNEE_) = '') remaining_empty;

DROP TEMPORARY TABLE IF EXISTS tmp_workflow_orphan_candidates;
DROP TEMPORARY TABLE IF EXISTS tmp_workflow_orphan_reconcile;
