import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
const modules = read('src/config/modules.js')
const list = read('src/pages/list/index.vue')
const detail = read('src/pages/detail/index.vue')
const page = read('src/pages/expense-verify/index.vue')
const pages = read('src/pages.json')

test('expense permissions are independent and legacy direct verify is removed', () => {
  assert.match(modules, /verify:\s*'finance:expense:verify'/)
  assert.match(modules, /unverify:\s*'finance:expense:unverify'/)
  assert.doesNotMatch(modules, /verify:\s*'finance:expense:edit'/)
  assert.doesNotMatch(modules, /\/finance\/expense\/verify\/\{id\}/)
})

test('expense list gates batch selection and only selects unverified expenses', () => {
  assert.match(list, /hasActionPermission\(this\.moduleKey,\s*'verify'\)/)
  assert.match(list, /batchSelecting/)
  assert.match(list, /selectedExpenseIds/)
  assert.match(list, /selectedExpenseTotal/)
  assert.match(list, /String\(item\.status[^\n]+!==\s*'1'/)
  assert.match(list, /\/pages\/expense-verify\/index\?expenseIds=/)
  assert.match(list, /resetBatchSelection/)
  assert.match(list, /onShow\(\)[\s\S]+resetBatchSelection\(\)/)
})

test('expense verification page validates input and uses batch API with advances', () => {
  assert.match(page, /new Set/)
  assert.match(page, /Number\.isSafeInteger/)
  assert.match(page, /\/finance\/expense\/batchVerify/)
  assert.match(page, /\/finance\/expense\/unverifiedAdvances/)
  assert.match(page, /requestId/)
  assert.match(page, /submitting/)
  assert.match(page, /differenceExplanation/)
  assert.match(page, /生成未核销节余借支单/)
  assert.match(page, /hasActionPermission\('advance',\s*'list'\)/)
  assert.match(page, /advancePermissionNotice/)
  assert.match(page, /!advancePermissionNotice\s*&&\s*!loading/)
  assert.match(page, /returnedIds/)
  assert.match(page, /requestedIds/)
  assert.match(page, /currentDeptId/)
  assert.match(page, /verificationReady/)
  assert.match(page, /!verificationReady/)
})

test('detail routes verify through shared page and reverses by capability', () => {
  assert.match(detail, /\/pages\/expense-verify\/index\?expenseIds=/)
  assert.match(detail, /`\/finance\/expense\/\$\{this\.recordId\}\/capability`/)
  assert.doesNotMatch(detail, /finance\/expense\/capability\/\$\{this\.recordId\}/)
  assert.match(detail, /finance\/expense\/unverify/)
  assert.match(detail, /canUnverify/)
  assert.match(detail, /operationDisabledReason/)
  assert.match(detail, /reverseRequestId/)
  assert.match(detail, /pendingUnverify/)
  assert.match(detail, /expenseCapability/)
  assert.match(detail, /loadExpenseCapability/)
  assert.match(detail, /unverify-disabled-reason/)
  assert.match(detail, /:disabled="!expenseCapability\.canUnverify"/)
  assert.match(detail, /operationDisabledReason/)
  assert.match(detail, /\.trim\(\)/)
  assert.match(detail, /onShow\(\)[\s\S]+loadDetail\(\)/)
})

test('verification page loads expenses only through scoped verification candidate endpoint', () => {
  const page = read('src/pages/expense-verify/index.vue')
  assert.match(page, /`\/finance\/expense\/\$\{id\}\/verificationCandidate`/)
  assert.doesNotMatch(page, /getData\('\/finance\/expense', id\)/)
  assert.match(page, /url: '\/finance\/expense\/unverifiedAdvances'[^]*method: 'GET'/)
  assert.doesNotMatch(page, /unverifiedAdvances'[^]*data: deptId/)
})

test('expense verification page is registered', () => {
  assert.match(pages, /pages\/expense-verify\/index/)
})
