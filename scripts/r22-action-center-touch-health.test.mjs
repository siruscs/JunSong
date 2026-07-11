import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.existsSync(path) ? fs.readFileSync(path, 'utf8') : ''

test('R22 deliverables exist', () => {
  for (const path of [
    'sql/r22_action_center_touch.sql',
    'junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysActionCenterController.java',
    'junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysActionCenterServiceImpl.java',
    'junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysActionTouchServiceImpl.java',
    'junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/WeWorkBotTouchChannelAdapter.java',
    'junsong-ui-v3/src/views/system/actionCenter/index.vue'
  ]) {
    assert.ok(fs.existsSync(path), `missing ${path}`)
  }
})

test('R22 action center covers required sources and statuses', () => {
  const corpus = [
    read('junsong-modules/junsong-system/src/main/java/com/junsong/system/domain/vo/ActionCenterItemVO.java'),
    read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysActionCenterServiceImpl.java'),
    read('junsong-modules/junsong-system/src/main/resources/mapper/system/SysActionCenterMapper.xml')
  ].join('\n')

  for (const source of ['FINANCE_RECEIVABLE', 'MEMBER_GROWTH', 'STOCK_HEALTH', 'SYSTEM_GOVERNANCE']) {
    assert.match(corpus, new RegExp(source), `missing source ${source}`)
  }

  for (const status of ['PENDING', 'IN_PROGRESS', 'DONE', 'IGNORED', 'EFFECT_PENDING']) {
    assert.match(corpus, new RegExp(status), `missing action status ${status}`)
  }
})

test('R22 exposes action list calendar and touch APIs with permissions', () => {
  const controller = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysActionCenterController.java')

  assert.match(controller, /@GetMapping\("\/list"\)/, 'list API missing')
  assert.match(controller, /@GetMapping\("\/calendar"\)/, 'calendar API missing')
  assert.match(controller, /@PostMapping\("\/\{actionId\}\/touch"\)/, 'touch API missing')
  assert.match(controller, /system:action-center:view/, 'view permission missing')
  assert.match(controller, /system:action-center:touch/, 'touch permission missing')
})

test('R22 touch channel is gated and observable', () => {
  const touchService = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysActionTouchServiceImpl.java')
  const adapter = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/WeWorkBotTouchChannelAdapter.java')
  const mapper = read('junsong-modules/junsong-system/src/main/resources/mapper/system/SysActionCenterTouchLogMapper.xml')

  for (const status of ['DRY_RUN', 'SUCCESS', 'FAILED', 'SKIPPED_RATE_LIMIT', 'SKIPPED_DUPLICATE', 'DISABLED']) {
    assert.match([touchService, adapter, mapper].join('\n'), new RegExp(status), `missing touch status ${status}`)
  }

  assert.match(touchService, /rateLimit/i, 'rate limit guard missing')
  assert.match(touchService, /duplicate/i, 'duplicate guard missing')
  assert.match(adapter, /dryRun/i, 'dry-run guard missing')
  assert.doesNotMatch(adapter, /hardcoded-webhook|qyapi\.weixin\.qq\.com\/cgi-bin\/webhook\/send\?key=[a-z0-9-]+/i, 'must not hardcode real webhook')
})

test('R22 does not implement R23 R24 R25 scopes', () => {
  const newFiles = [
    read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysActionCenterController.java'),
    read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysActionTouchServiceImpl.java'),
    read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/WeWorkBotTouchChannelAdapter.java'),
    read('sql/r22_action_center_touch.sql')
  ].join('\n')

  const forbidden = [
    /webhook\s*subscription/i,
    /dead[-_ ]?letter/i,
    /sdk/i,
    /developer\s*billing/i,
    /what-if/i,
    /load\s*test/i,
    /stress\s*test/i
  ]
  for (const pattern of forbidden) {
    assert.doesNotMatch(newFiles, pattern, `forbidden future scope found: ${pattern}`)
  }
})

test('R22 is registered in admin-health', () => {
  const adminHealth = read('scripts/admin-health.mjs')
  assert.match(adminHealth, /R22 action center touch health/, 'R22 not registered in admin-health')
})

test('R22 STOCK_HEALTH is truly aggregated, not just a comment', () => {
  const financeInner = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinanceActionCenterInnerController.java')
  assert.match(financeInner, /STOCK_HEALTH/, 'STOCK_HEALTH source type not emitted by finance inner controller')
  assert.match(financeInner, /IStockHealthService|stockHealthService/, 'finance inner controller must call IStockHealthService')
  assert.match(financeInner, /checkHealth/, 'finance inner controller must call checkHealth()')
  assert.match(financeInner, /StockHealthIssueVO|getIssues/, 'finance inner controller must iterate stock health issues')
})

test('R22 source exceptions propagate to system for risk action generation', () => {
  const financeInner = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinanceActionCenterInnerController.java')
  const memberInner = read('junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemberActionCenterInnerController.java')

  assert.doesNotMatch(financeInner, /catch\s*\(\s*Exception\s+\w+\s*\)\s*\{/, 'finance inner controller must not catch Exception (let it propagate)')
  assert.doesNotMatch(memberInner, /catch\s*\(\s*Exception\s+\w+\s*\)\s*\{/, 'member inner controller must not catch Exception (let it propagate)')

  const systemService = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysActionCenterServiceImpl.java')
  assert.match(systemService, /SOURCE_FINANCE_UNREACHABLE|SOURCE_MEMBER_UNREACHABLE/, 'system service must generate risk action on source unreachable')
})

test('R22 touch log failure is not silently swallowed', () => {
  const touchService = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysActionTouchServiceImpl.java')
  assert.match(touchService, /touchLogMapper\.insertLog\(log\)/, 'touch log insert must be called directly')
  assert.doesNotMatch(touchService, /try\s*\{\s*touchLogMapper\.insertLog\(log\)\s*;\s*\}\s*catch\s*\(\s*Exception\s+ignore\s*\)\s*\{\s*\}/, 'touch log insert must not be wrapped in try-catch-ignore')
})

test('R22 touch blocks non-touchable action states in backend', () => {
  const touchService = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysActionTouchServiceImpl.java')
  assert.match(touchService, /getTouchable/, 'touch service must check action.getTouchable()')
  assert.match(touchService, /不可触达|not.*touchable|touchable/, 'touch service must reject non-touchable actions')
})

test('R22 EFFECT_PENDING is set after successful touch', () => {
  const systemService = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysActionCenterServiceImpl.java')
  assert.match(systemService, /EFFECT_PENDING/, 'system service must reference EFFECT_PENDING status')
  assert.match(systemService, /SUCCESS.*EFFECT_PENDING|DRY_RUN.*EFFECT_PENDING/s, 'system service must set EFFECT_PENDING after SUCCESS or DRY_RUN touch')
})
