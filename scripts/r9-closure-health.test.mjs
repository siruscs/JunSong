import { readFileSync, existsSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

test('finance review task log SQL exists', () => {
  assert.equal(existsSync('sql/finance_review_task_log.sql'), true)
  const sql = readFileSync('sql/finance_review_task_log.sql', 'utf8').toLowerCase()
  assert.match(sql, /create table if not exists finance_review_task_log/)
  assert.match(sql, /before_status/)
  assert.match(sql, /after_status/)
  assert.match(sql, /handler_note/)
})

test('system governance task log SQL exists', () => {
  assert.equal(existsSync('sql/sys_governance_task_log.sql'), true)
  const sql = readFileSync('sql/sys_governance_task_log.sql', 'utf8').toLowerCase()
  assert.match(sql, /create table if not exists sys_governance_task_log/)
  assert.match(sql, /action_type/)
  assert.match(sql, /handler_note/)
})

test('sale record member link SQL is mysql compatible', () => {
  assert.equal(existsSync('sql/finance_sale_member_link.sql'), true)
  const sql = readFileSync('sql/finance_sale_member_link.sql', 'utf8').toLowerCase()
  assert.match(sql, /information_schema\.columns/)
  assert.match(sql, /member_id/)
  assert.match(sql, /idx_fin_sale_member/)
  assert.doesNotMatch(sql, /add column if not exists/)
})

test('getTaskLogs calls verifyTaskAccess for data scope', () => {
  const source = readFileSync(
    'junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinanceReviewTaskServiceImpl.java',
    'utf8',
  )
  // The getTaskLogs method must call verifyTaskAccess before querying
  const getTaskLogsMatch = source.match(/getTaskLogs\s*\([^)]*\)\s*\{([\s\S]*?)^\s*\}/m)
  assert.ok(getTaskLogsMatch, 'getTaskLogs method must exist')
  assert.match(getTaskLogsMatch[1], /verifyTaskAccess/, 'getTaskLogs must call verifyTaskAccess')
})

test('governance action endpoint validates actionType and requires notes', () => {
  const source = readFileSync(
    'junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysDashboardController.java',
    'utf8',
  )
  // Must validate actionType enum
  assert.match(source, /"ACK"/, 'Must check ACK action type')
  assert.match(source, /"DONE"/, 'Must check DONE action type')
  assert.match(source, /"IGNORED"/, 'Must check IGNORED action type')
  // Must require handlerNote for DONE/IGNORED
  assert.match(source, /handlerNote.*trim|handlerNote.*isEmpty/i, 'Must validate handlerNote is not empty for DONE/IGNORED')
})

test('governance SQL grants only to role_id=1 not all active roles', () => {
  const sql = readFileSync('sql/sys_governance_task_log.sql', 'utf8')
  // Must NOT have SELECT DISTINCT r.role_id FROM sys_role pattern
  assert.doesNotMatch(sql, /SELECT\s+DISTINCT\s+r\.role_id/is, 'Must not grant to all active roles')
  // Must have role_id = 1
  assert.match(sql, /role_id\s*=\s*1|SELECT\s+1\s*,/i, 'Must grant to role_id=1')
})

test('governance SQL creates system:dashboard:governance permission', () => {
  const sql = readFileSync('sql/sys_governance_task_log.sql', 'utf8')
  assert.match(sql, /system:dashboard:governance/, 'Must create system:dashboard:governance permission')
})

test('frontend reviewTask API has getTaskLogs function', () => {
  const api = readFileSync('junsong-ui-v3/src/api/finance/reviewTask.ts', 'utf8')
  assert.match(api, /getTaskLogs/, 'reviewTask API must export getTaskLogs')
  assert.match(api, /review-task\/\$\{.*\}\/logs/, 'Must call /review-task/{taskId}/logs endpoint')
})

test('frontend reviewTask page references task logs', () => {
  const page = readFileSync('junsong-ui-v3/src/views/finance/reviewTask/index.vue', 'utf8')
  assert.match(page, /getTaskLogs|taskLogs|task-logs|轨迹/, 'Review task page must reference task logs UI')
})
