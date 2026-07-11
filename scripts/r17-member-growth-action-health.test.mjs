import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

test('R17 sql creates growth action tables and permissions', () => {
  const sql = read('sql/member_growth_action_r17.sql')
  assert.match(sql, /CREATE TABLE IF NOT EXISTS mem_growth_action/i)
  assert.match(sql, /CREATE TABLE IF NOT EXISTS mem_growth_action_member/i)
  assert.match(sql, /member:growthAction:view/)
  assert.match(sql, /member:growthAction:generate/)
  assert.match(sql, /member:growthAction:execute/)
  assert.match(sql, /member:growthAction:effect/)
})

test('R17 backend exposes growth action endpoints with permissions', () => {
  const controller = read('junsong-modules/junsong-member/src/main/java/com/junsong/member/controller/MemberGrowthActionController.java')
  assert.match(controller, /@RequiresPermissions\("member:growthAction:view"\)/)
  assert.match(controller, /@RequiresPermissions\("member:growthAction:generate"\)/)
  assert.match(controller, /@RequiresPermissions\("member:growthAction:execute"\)/)
  assert.match(controller, /@RequiresPermissions\("member:growthAction:effect"\)/)
  assert.match(controller, /\/growth-action\/dashboard/)
  assert.match(controller, /\/growth-action\/candidates/)
  assert.match(controller, /\/growth-action\/members/)
  assert.match(controller, /\/growth-action\/generate/)
  assert.match(controller, /\/growth-action\/execute/)
  assert.match(controller, /\/growth-action\/effect/)
})

test('R17 executeAction validates affected rows and recomputes status by actionId', () => {
  const service = read('junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberGrowthActionServiceImpl.java')
  // P0：检查影响行数，0 行抛异常
  assert.match(service, /int affected = memberGrowthActionMapper\.updateActionMemberExecute\(params\)/)
  assert.match(service, /if \(affected == 0\)/)
  // P0：按当前 actionId 重算 executed_count/status
  assert.match(service, /countExecutedByActionId\(params\.getActionId\(\)\)/)
  assert.match(service, /countTotalByActionId\(params\.getActionId\(\)\)/)
  // 不应再使用全局 effect summary 刷新当前 action
  assert.doesNotMatch(service, /selectEffectSummary\(queryParams\)\s*;\s*\/\/.*executedCount/, '不应使用全局 effect summary 刷新当前 action')
})

test('R17 mapper supports action members query and real effect stats', () => {
  const mapper = read('junsong-modules/junsong-member/src/main/java/com/junsong/member/mapper/MemberGrowthActionMapper.java')
  assert.match(mapper, /selectActionMembers/)
  assert.match(mapper, /countExecutedByActionId/)
  assert.match(mapper, /countTotalByActionId/)
  assert.match(mapper, /selectRealEffectByActionId/)
  assert.match(mapper, /updateMemberEffectFlags/)

  const xml = read('junsong-modules/junsong-member/src/main/resources/mapper/member/MemberGrowthActionMapper.xml')
  // updateActionMemberExecute 应有状态校验，避免重复执行
  assert.match(xml, /AND execute_status IN \('PENDING', 'IN_PROGRESS'\)/)
  // R17-D 真实效果回查
  assert.match(xml, /fin_sale_record/)
  assert.match(xml, /mem_member_sign_in/)
  assert.match(xml, /growth_value &gt; am\.growth_value/)
  // P1-1 修复：真实效果统计使用 COUNT(DISTINCT) 避免 JOIN 乘法放大
  assert.match(xml, /COUNT\(DISTINCT am\.id\) AS totalMemberCount/, 'totalMemberCount 应使用 COUNT(DISTINCT am.id)')
  assert.match(xml, /COUNT\(DISTINCT CASE WHEN/, '各效果人数应使用 COUNT(DISTINCT CASE WHEN ...)')
  // 不应直接 JOIN fin_sale_record/mem_member_sign_in 到主查询（会导致行乘法）
  assert.doesNotMatch(xml, /LEFT JOIN fin_sale_record r ON/, '不应直接 LEFT JOIN fin_sale_record（会乘法放大）')
  assert.doesNotMatch(xml, /LEFT JOIN mem_member_sign_in si ON/, '不应直接 LEFT JOIN mem_member_sign_in（会乘法放大）')
})

test('R17 getEffect calls updateMemberEffectFlags before real effect query', () => {
  const service = read('junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberGrowthActionServiceImpl.java')
  // P1-2 修复：带 actionId 时先回填标记位
  assert.match(service, /updateMemberEffectFlags\(params\.getActionId\(\)\)/)
  assert.match(service, /selectRealEffectByActionId\(params\.getActionId\(\)\)/)
})

test('R17 frontend effect panel passes actionId to trigger real effect query', () => {
  const vue = read('junsong-ui-v3/src/views/member/growthAction/index.vue')
  // P1-2 修复：filter 包含 actionId
  assert.match(vue, /actionId: undefined as number \| undefined/)
  // 效果复盘面板有动作选择下拉框
  assert.match(vue, /选择动作查看真实效果/)
  // loadEffect 传 filter（含 actionId）
  assert.match(vue, /getGrowthActionEffect\(params\)/)
  // dashboard 加载后默认选中第一个动作触发真实效果回查
  assert.match(vue, /filter\.actionId = recentActions\.value\[0\]\.actionId/)
})

test('R17 service links cash pressure, member activity, and effect tracking', () => {
  const service = read('junsong-modules/junsong-member/src/main/java/com/junsong/member/service/impl/MemberGrowthActionServiceImpl.java')
  assert.match(service, /SLEEPING_HIGH_VALUE/)
  assert.match(service, /NEAR_LEVEL_UP/)
  assert.match(service, /RECENT_ACTIVE_NO_REPEAT/)
  assert.match(service, /PRESSURE_STORE_RECALL/)
  assert.match(service, /pressureLevel/)
  assert.match(service, /effectRate/)
})

test('R17 frontend adds growth action page and overview card', () => {
  const api = read('junsong-ui-v3/src/api/member/growthAction.ts')
  const page = read('junsong-ui-v3/src/views/member/growthAction/index.vue')
  const overview = read('junsong-ui-v3/src/views/member/overview/index.vue')
  assert.match(api, /getGrowthActionDashboard/)
  assert.match(api, /listGrowthActionCandidates/)
  assert.match(api, /generateGrowthAction/)
  assert.match(api, /executeGrowthAction/)
  assert.match(api, /getGrowthActionEffect/)
  assert.match(page, /会员增长动作/)
  assert.match(page, /候选会员/)
  assert.match(page, /生成动作/)
  assert.match(page, /效果复盘/)
  assert.match(overview, /增长动作/)
})
