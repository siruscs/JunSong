import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

test('R18 release inventory and execution report are present', () => {
  read('docs/superpowers/plans/2026-07-03-r1-r18-release-inventory.zh-CN.md')
  read('docs/superpowers/plans/2026-07-03-r18-r1-r18-closure-execution-report.zh-CN.md')
  read('docs/superpowers/plans/2026-07-03-r1-r18-risk-and-backlog.zh-CN.md')
})

test('R18 keeps R9-R17 stage health scripts available', () => {
  const scripts = [
    'scripts/r9-closure-health.test.mjs',
    'scripts/r10-readiness-health.test.mjs',
    'scripts/r10-config-quality-health.test.mjs',
    'scripts/r11-readiness-health.test.mjs',
    'scripts/r11-store-health-knowledge-health.test.mjs',
    'scripts/r12-action-effect-health.test.mjs',
    'scripts/r13-receivable-cashflow-health.test.mjs',
    'scripts/r14-receivable-closure-health.test.mjs',
    'scripts/r15-receivable-command-center-health.test.mjs',
    'scripts/r16-cashflow-forecast-health.test.mjs',
    'scripts/r17-member-growth-action-health.test.mjs',
  ]
  for (const script of scripts) {
    assert.equal(existsSync(script), true, `${script} must exist`)
  }
})

test('R18 keeps core backend gates available', () => {
  read('scripts/backend-permission-health.mjs')
  read('scripts/backend-mybatis-health.mjs')
  read('scripts/admin-health.mjs')
  read('scripts/three-module-regression.mjs')
})

test('R18 keeps R13-R17 business SQL assets available', () => {
  const sqlFiles = [
    'sql/finance_receivable_collection_r15.sql',
    'sql/finance_cashflow_forecast_r16.sql',
    'sql/member_growth_action_r17.sql',
  ]
  for (const file of sqlFiles) {
    const sql = read(file)
    assert.match(sql, /INSERT|CREATE|ALTER/i)
  }
})

test('R18 preserves R17 Codex-reviewed fixes', () => {
  const service = read('junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberGrowthActionServiceImpl.java')
  const mapperXml = read('junsong-modules/junsong-member/src/main/resources/mapper/member/MemberGrowthActionMapper.xml')
  const vue = read('junsong-ui-v3/src/views/member/growthAction/index.vue')

  assert.match(service, /if \(affected == 0\)/)
  assert.match(service, /countExecutedByActionId\(params\.getActionId\(\)\)/)
  assert.match(service, /updateMemberEffectFlags\(params\.getActionId\(\)\)/)
  assert.match(mapperXml, /COUNT\(DISTINCT am\.id\) AS totalMemberCount/)
  assert.doesNotMatch(mapperXml, /LEFT JOIN fin_sale_record r ON/)
  assert.match(vue, /filter\.actionId = recentActions\.value\[0\]\.actionId/)
})

test('R18 execution report contains required closure sections', () => {
  const report = read('docs/superpowers/plans/2026-07-03-r18-r1-r18-closure-execution-report.zh-CN.md')
  assert.match(report, /R1-R18 资产盘点/)
  assert.match(report, /总健康门禁/)
  assert.match(report, /DEV 冒烟/)
  assert.match(report, /PROD 发布状态/)
  assert.match(report, /风险与 R19\+ Backlog/)
  assert.match(report, /Codex 复核请求/)
})
