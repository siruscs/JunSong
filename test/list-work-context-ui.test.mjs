import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const page = fs.readFileSync(new URL('../src/pages/list/index.vue', import.meta.url), 'utf8')

test('generic list visibly identifies its authoritative work scope', () => {
  assert.match(page, /class="work-scope"/)
  assert.match(page, /currentDeptName/)
  assert.match(page, /scopeLabel/)
  assert.match(page, /resolveListWorkScope/)
})

test('returning after a department switch clears stale data but preserves search', () => {
  assert.match(page, /workContext\.snapshot\(\)/)
  const sync = page.match(/syncWorkScope\(\) \{([\s\S]*?)(?=\n    [a-zA-Z])/i)?.[1] || ''
  assert.match(sync, /departmentChanged/)
  assert.match(sync, /this\.rows = \[\]/)
  assert.match(sync, /this\.expenseSummary = null/)
  assert.doesNotMatch(sync, /this\.queryValue\s*=/)
})

test('list and expense summary reject responses from an old work context', () => {
  const fetchList = page.match(/async fetchList\(reset\) \{([\s\S]*?)(?=\n    async loadClaimRows)/)?.[1] || ''
  assert.match(fetchList, /const contextVersion = workContext\.captureVersion\(\)/)
  assert.match(fetchList, /workContext\.isCurrent\(contextVersion\)/)
  assert.match(fetchList, /String\(this\.currentDeptId\) !== String\(requestDeptId\)/)

  const summary = page.match(/async loadExpenseSummary\(\) \{([\s\S]*?)(?=\n    statusText)/)?.[1] || ''
  assert.match(summary, /const contextVersion = workContext\.captureVersion\(\)/)
  assert.match(summary, /workContext\.isCurrent\(contextVersion\)/)
  assert.match(summary, /String\(this\.currentDeptId\) !== String\(requestDeptId\)/)
})

test('missing work scope blocks list and summary requests before transport', () => {
  const fetchList = page.match(/async fetchList\(reset\) \{([\s\S]*?)(?=\n    async loadClaimRows)/)?.[1] || ''
  assert.match(fetchList, /if \(!canRequestListScope\(this\.currentDeptId\)\)/)
  assert.ok(fetchList.indexOf('!canRequestListScope(this.currentDeptId)') < fetchList.indexOf('listData('))

  const summary = page.match(/async loadExpenseSummary\(\) \{([\s\S]*?)(?=\n    statusText)/)?.[1] || ''
  assert.match(summary, /if \(!canRequestListScope\(this\.currentDeptId\)\)/)
  assert.ok(summary.indexOf('!canRequestListScope(this.currentDeptId)') < summary.indexOf("request({ url: '/finance/expense/summary'"))
})

test('pagination rollback cannot restore a page from an old department', () => {
  const loadMore = page.match(/async loadMore\(\) \{([\s\S]*?)(?=\n    async fetchList)/)?.[1] || ''
  assert.match(loadMore, /const contextVersion = workContext\.captureVersion\(\)/)
  assert.match(loadMore, /shouldRestoreListPage\(loaded, workContext\.isCurrent\(contextVersion\)\)/)
})
