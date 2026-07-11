-- ============================================================
-- R12 动作成效与质量调优 — 权限 SQL
-- ============================================================
-- 结论：R12 所有新增端点均复用已有权限码，无需新增 sys_menu 行。
--
-- 已有权限码复用清单：
--   finance:reviewTask:list   → GET  /review-task/{taskId}/effect
--                               GET  /review-task/effect-summary
--   finance:reviewTask:edit   → POST /review-task/{taskId}/reopen
--   finance:reviewTask:add    → POST /review-task/from-member-action
--   finance:reviewKnowledge:list → GET /review-knowledge/recommendations/task/{taskId}
--
-- 无需执行 INSERT，此文件仅作归档记录。
-- ============================================================

-- 验证：确认现有权限码已存在
SELECT menu_id, menu_name, perms
FROM sys_menu
WHERE perms IN (
  'finance:reviewTask:list',
  'finance:reviewTask:edit',
  'finance:reviewTask:add',
  'finance:reviewKnowledge:list'
)
ORDER BY menu_id;
