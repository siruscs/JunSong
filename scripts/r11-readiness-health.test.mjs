import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

test('R10 health rule config foundation exists', () => {
  const sql = read('sql/sys_health_rule_config.sql').toLowerCase()
  assert.match(sql, /create table if not exists sys_health_rule_config/)
  assert.match(sql, /rule_code/)
  assert.match(sql, /threshold_value/)
})

test('R10 review quality endpoint exists', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/ReviewQualityController.java')
  assert.match(src, /finance:reviewQuality:view/)
  assert.match(src, /review-quality/)
  assert.match(src, /\/dashboard/)
})

test('R9 review task log foundation exists', () => {
  const sql = read('sql/finance_review_task_log.sql').toLowerCase()
  assert.match(sql, /finance_review_task_log/)
  assert.match(sql, /handler_note/)
})

test('authorized store portfolio foundation exists', () => {
  const service = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/StoreFinanceReportServiceImpl.java')
  assert.match(service, /getAuthorizedPortfolio/)
  assert.match(service, /loadAllowedDeptIds|RemoteUserService/)
  assert.match(service, /fillStoreDerivedMetrics/)
})

test('finance review task menu and frontend route are complete', () => {
  const sql = read('sql/finance_review_task.sql')
  // All three perms must exist
  assert.match(sql, /finance:reviewTask:list/)
  assert.match(sql, /finance:reviewTask:add/)
  assert.match(sql, /finance:reviewTask:edit/)
  // Component path must be correct
  assert.match(sql, /finance\/reviewTask\/index/)
  // Must NOT gate on @operationMenuId (R11 P0-FIX: removed unnecessary dependency)
  assert.doesNotMatch(sql, /@operationMenuId IS NOT NULL/)
  // Must have COALESCE fallback for financeRootId
  assert.match(sql, /COALESCE\(@financeRootId/)
  // Must NOT do broad role grants
  assert.doesNotMatch(sql, /SELECT\s+DISTINCT\s+r\.role_id/is)

  // Frontend component must exist and have correct title
  const page = read('junsong-ui-v3/src/views/finance/reviewTask/index.vue')
  assert.match(page, /复盘任务管理/)

  // API must point to correct backend endpoints
  const api = read('junsong-ui-v3/src/api/finance/reviewTask.ts')
  assert.match(api, /finance\/review-task\/list/)
  assert.match(api, /finance\/review-task\/generate/)
})
