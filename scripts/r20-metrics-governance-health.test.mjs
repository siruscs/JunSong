import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { checkMetricsDictionary } from './metrics-dictionary-health.mjs'

test('R20 metrics dictionary is complete and unique', () => {
  const report = checkMetricsDictionary()

  assert.equal(report.ok, true, report.errors.join('\n'))
  assert.ok(report.totalMetrics >= 30)
  assert.ok(report.modules.finance >= 12)
  assert.ok(report.modules.member >= 8)
  assert.ok(report.modules.system >= 5)
  assert.ok(report.modules.stock >= 5)
})

test('R20 forbidden future scopes are not implemented in dictionary', () => {
  const report = checkMetricsDictionary()

  assert.deepEqual(report.forbiddenMatches, [])
})

test('R20 data quality menu SQL is idempotent and admin-only', () => {
  const sql = readFileSync('sql/system_data_quality_menu.sql', 'utf8')

  assert.match(sql, /system:data-quality:view/)
  assert.match(sql, /WHERE NOT EXISTS/i)
  assert.match(sql, /role_id\s*=\s*1/i)
  assert.doesNotMatch(sql, /SELECT\s+role_id\s+FROM\s+sys_role/i)
})

test('R20 frontend data quality page and api exist', () => {
  const api = readFileSync('junsong-ui-v3/src/api/system/dataQuality.ts', 'utf8')
  const page = readFileSync('junsong-ui-v3/src/views/system/dataQuality/index.vue', 'utf8')

  assert.match(api, /\/system\/data-quality\/dashboard/)
  assert.match(page, /getDataQualityDashboard/)
  assert.match(page, /issueType/)
  assert.match(page, /severity/)
  assert.doesNotMatch(page, /假数据|mock|Mock/)
})
