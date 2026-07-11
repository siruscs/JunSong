import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

test('R12 lifecycle migration exists', () => {
  const sql = read('sql/finance_review_task_lifecycle_r12.sql').toLowerCase()
  assert.match(sql, /archived/)
  assert.match(sql, /archive_time/)
  assert.match(sql, /reopen_count/)
})

test('R12 review task effect endpoint exists', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinanceReviewTaskController.java')
  assert.match(src, /\/\{taskId\}\/effect/)
  assert.match(src, /finance:reviewTask:list/)
})

test('R12 reopen endpoint exists and requires edit permission', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinanceReviewTaskController.java')
  assert.match(src, /\/\{taskId\}\/reopen/)
  assert.match(src, /finance:reviewTask:edit/)
})

test('R12 knowledge recommendation endpoint exists', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinanceReviewKnowledgeController.java')
  assert.match(src, /recommendations\/task\/\{taskId\}/)
})

test('R12 effect summary endpoint exists', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinanceReviewTaskController.java')
  assert.match(src, /effect-summary/)
})

test('R12 permission gap script has no hardcoded mysql password default', () => {
  const src = read('scripts/permission-menu-gap-health.mjs')
  assert.doesNotMatch(src, /MYSQL_ROOT_PASSWORD\s*=\s*process\.env\.MYSQL_ROOT_PASSWORD\s*\|\|\s*['"]root_123['"]/)
})

test('R12 governance SQL uses dynamic parent_id, not hardcoded 1', () => {
  const sql = read('sql/sys_governance_task_log.sql')
  assert.match(sql, /@sysRootId/, 'must use @sysRootId variable for parent lookup')
  assert.match(sql, /path\s*=\s*'system'/, 'must query system root menu by path')
  assert.doesNotMatch(sql, /SELECT\s+'[^']+',\s*1,\s*\d+/i, 'INSERT SELECT must not hardcode parent_id=1')
})

test('R12 admin-health.test.mjs is synced with pipeline', () => {
  const testSrc = read('scripts/admin-health.test.mjs')
  assert.match(testSrc, /r12 action effect health unit tests/, 'admin-health.test must include R12 check name')
  assert.match(testSrc, /r12-action-effect-health\.test\.mjs/, 'admin-health.test must reference R12 test script')
})
