import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.existsSync(path) ? fs.readFileSync(path, 'utf8') : ''

test('R24 deliverables exist', () => {
  for (const path of [
    'sql/r24_predictive_ops_v2.sql',
    'junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/PredictiveOpsController.java',
    'junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/PredictiveOpsServiceImpl.java',
    'junsong-modules/junsong-finance/src/main/resources/mapper/finance/PredictiveOpsMapper.xml',
    'junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemberPredictiveOpsInnerController.java',
    'junsong-ui-v3/src/views/finance/predictiveOps/index.vue',
    'docs/superpowers/plans/2026-07-04-r24-predictive-ops-v2-execution-report.zh-CN.md'
  ]) {
    assert.ok(fs.existsSync(path), `missing ${path}`)
  }
})

test('R24 predictions expose explainable factors and risk levels', () => {
  const corpus = [
    read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/PredictionRiskVO.java'),
    read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/PredictionFactorVO.java'),
    read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/PredictiveOpsServiceImpl.java')
  ].join('\n')

  for (const token of ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL', 'score', 'factors', 'basis', 'sampleDate']) {
    assert.match(corpus, new RegExp(token), `missing explainability token ${token}`)
  }
})

test('R24 covers cashflow receivable member stock and what-if', () => {
  const corpus = [
    read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/PredictiveOpsServiceImpl.java'),
    read('junsong-modules/junsong-finance/src/main/resources/mapper/finance/PredictiveOpsMapper.xml'),
    read('junsong-ui-v3/src/views/finance/predictiveOps/index.vue')
  ].join('\n')

  for (const token of ['CASHFLOW', 'RECEIVABLE', 'MEMBER_ACTION', 'STOCK', 'WHAT_IF']) {
    assert.match(corpus, new RegExp(token), `missing prediction domain ${token}`)
  }
})

test('R24 what-if is simulation-only and does not mutate business state', () => {
  const controller = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/PredictiveOpsController.java')
  const service = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/PredictiveOpsServiceImpl.java')
  const corpus = `${controller}\n${service}`

  assert.match(controller, /@PostMapping\(\"\/predictive-ops\/what-if\"\)/, 'what-if endpoint missing')
  assert.match(corpus, /simulateWhatIf/, 'simulation method missing')
  assert.doesNotMatch(corpus, /updateReceivableStatus|completeAction|touch\(|sendMessage|adjustStock|updateMemberLevel/i, 'what-if must not mutate business state')
})

test('R24 endpoints require permissions and preserve tenant or store scope', () => {
  const controller = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/PredictiveOpsController.java')
  const mapper = read('junsong-modules/junsong-finance/src/main/resources/mapper/finance/PredictiveOpsMapper.xml')
  const service = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/PredictiveOpsServiceImpl.java')

  assert.match(controller, /finance:predictiveOps:view/, 'view permission missing')
  assert.match(controller, /finance:predictiveOps:simulate/, 'simulate permission missing')
  assert.match([mapper, service].join('\n'), /deptId|dept_id|tenantId|tenant_id/, 'scope boundary missing')
})

test('R24 avoids black-box model and future R25 scope', () => {
  const corpus = [
    read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/PredictiveOpsServiceImpl.java'),
    read('junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberActionPredictionServiceImpl.java'),
    read('junsong-ui-v3/src/views/finance/predictiveOps/index.vue')
  ].join('\n')

  for (const forbidden of [
    /machine\s*learning/i,
    /tensorflow/i,
    /pytorch/i,
    /sklearn/i,
    /llm/i,
    /openai/i,
    /black\s*box/i,
    /load\s*test\s*platform/i,
    /archive\s*platform/i,
    /alert\s*platform/i
  ]) {
    assert.doesNotMatch(corpus, forbidden, `forbidden scope leaked: ${forbidden}`)
  }
})
