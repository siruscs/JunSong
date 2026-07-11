import { readFileSync, existsSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

import { checkOverviewRealData } from './overview-real-data-health.mjs'

const FINANCE = 'junsong-ui-v3/src/views/finance/overview/index.vue'
const MEMBER = 'junsong-ui-v3/src/views/member/overview/index.vue'
const SYSTEM = 'junsong-ui-v3/src/views/system/overview/index.vue'

// 硬编码假指标数组模式，例如 { value: 1288, trend: '+12%' }
const FAKE_METRIC_PATTERN = /value\s*:\s*\d+,\s*trend\s*:\s*['"]\s*[+-]?\d+\s*%?['"]/

function readOverview(p) {
  return readFileSync(p, 'utf8')
}

test('member overview uses operation API', () => {
  const src = readOverview(MEMBER)
  assert.match(src, /dashboard\/operation|getDashboardOperation/i)
})

test('member overview calls points-summary API', () => {
  const src = readOverview(MEMBER)
  assert.match(src, /points-summary|pointsOperation|pointsSummary/i)
})

test('finance overview calls real finance dashboard API', () => {
  const src = readOverview(FINANCE)
  assert.match(src, /\/finance\/dashboard\//)
})

test('finance overview calls all 4 required finance APIs (R8-P0)', () => {
  const src = readOverview(FINANCE)
  assert.match(src, /\/finance\/dashboard\/operation/, 'must call /finance/dashboard/operation')
  assert.match(src, /\/finance\/dashboard\/alerts/, 'must call /finance/dashboard/alerts')
  assert.match(src, /\/finance\/dashboard\/review-tasks/, 'must call /finance/dashboard/review-tasks')
  assert.match(src, /\/finance\/cashflow\/dashboard/, 'must call /finance/cashflow/dashboard')
})

test('member overview calls at least 3 of 5 real member APIs (R8-P0)', () => {
  const src = readOverview(MEMBER)
  const checks = [
    /\/member\/dashboard\/stats|getDashboardStats/i,
    /\/member\/dashboard\/trend|getDashboardTrend/i,
    /\/member\/dashboard\/points-summary|pointsOperation|pointsSummary/i,
    /\/member\/dashboard\/operation|getDashboardOperation/i,
    /\/member\/report\/contribution/,
  ]
  let count = 0
  for (const re of checks) {
    if (re.test(src)) count++
  }
  assert.ok(count >= 3, `member overview should call at least 3 of 5 real APIs, got ${count}`)
})

test('system overview calls stats health and governance APIs (R8-P0)', () => {
  const src = readOverview(SYSTEM)
  assert.match(src, /getDashboardStats|\/system\/dashboard\/stats/i)
  assert.match(src, /system\/dashboard\/health|getDashboardHealth/i)
  assert.match(src, /system\/dashboard\/governance|getDashboardGovernance/i)
})

test('finance overview has no hardcoded fake metrics', () => {
  const src = readOverview(FINANCE)
  assert.doesNotMatch(src, FAKE_METRIC_PATTERN)
})

test('member overview has no hardcoded fake metrics', () => {
  const src = readOverview(MEMBER)
  assert.doesNotMatch(src, FAKE_METRIC_PATTERN)
})

test('system overview has no hardcoded fake metrics', () => {
  const src = readOverview(SYSTEM)
  assert.doesNotMatch(src, FAKE_METRIC_PATTERN)
})

test('finance overview has error state handling', () => {
  const src = readOverview(FINANCE)
  assert.match(src, /loadError|\.catch\s*\(|v-if=.*error|el-alert.*error/i)
})

test('member overview has error state handling', () => {
  const src = readOverview(MEMBER)
  assert.match(src, /loadError|\.catch\s*\(|v-if=.*error|el-alert.*error/i)
})

test('system overview has error state handling', () => {
  const src = readOverview(SYSTEM)
  assert.match(src, /loadError|\.catch\s*\(|v-if=.*error|el-alert.*error/i)
})

test('checkOverviewRealData passes with at most warnings', () => {
  const result = checkOverviewRealData()
  assert.equal(result.passed, true, `unexpected errors: ${result.errors.join('; ')}`)
  assert.ok(Array.isArray(result.errors), 'errors should be an array')
  assert.ok(Array.isArray(result.warnings), 'warnings should be an array')
})

test('overview-real-data-health script file exists and is importable', () => {
  assert.ok(existsSync('scripts/overview-real-data-health.mjs'), 'script file should exist')
  assert.equal(typeof checkOverviewRealData, 'function', 'checkOverviewRealData should be a function')
})
