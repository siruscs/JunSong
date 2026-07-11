import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

test('health rule config SQL exists', () => {
  const sql = read('sql/sys_health_rule_config.sql').toLowerCase()
  assert.match(sql, /create table if not exists sys_health_rule_config/)
  assert.match(sql, /rule_code/)
  assert.match(sql, /threshold_value/)
  assert.match(sql, /fin_review_first_response_hours/)
})

test('health rule controller exists', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysHealthRuleConfigController.java')
  assert.match(src, /system:healthRule:list/)
  assert.match(src, /system:healthRule:edit/)
})

test('review quality endpoint exists', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/ReviewQualityController.java')
  assert.match(src, /review-quality/)
  assert.match(src, /\/dashboard/)
  assert.match(src, /finance:reviewQuality:view/)
})

test('weekly memo endpoint exists', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/DailyReviewBoardController.java')
  assert.match(src, /weekly-memo/)
})

test('health rule menu SQL grants only role_id=1', () => {
  const sql = read('sql/sys_health_rule_config_menu.sql')
  assert.match(sql, /role_id = 1/)
  assert.doesNotMatch(sql, /SELECT DISTINCT role_id/)
})

test('review quality permission SQL grants only role_id=1', () => {
  const sql = read('sql/finance_review_quality_permission.sql')
  assert.match(sql, /role_id = 1/)
  assert.doesNotMatch(sql, /SELECT DISTINCT role_id/)
})

test('health rule config reader exists in finance', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/HealthRuleConfigReader.java')
  assert.match(src, /getThreshold/)
  assert.match(src, /isEnabled/)
  assert.match(src, /sys_health_rule_config/)
})

test('frontend health rule API exists', () => {
  const src = read('junsong-ui-v3/src/api/system/healthRule.ts')
  assert.match(src, /listHealthRules/)
  assert.match(src, /toggleHealthRule/)
})

test('frontend health rule page exists', () => {
  const src = read('junsong-ui-v3/src/views/system/healthRule/index.vue')
  assert.match(src, /规则领域/)
})

// ==================== R10-HOTFIX 覆盖扩展 ====================

test('R10-FIX-B: review quality frontend page exists', () => {
  const src = read('junsong-ui-v3/src/views/finance/reviewQuality/index.vue')
  assert.match(src, /复盘质量分/)
  assert.match(src, /getReviewQualityDashboard/)
  assert.match(src, /qualityScore/)
})

test('R10-FIX-B: review quality frontend API exists', () => {
  const src = read('junsong-ui-v3/src/api/finance/reviewQuality.ts')
  assert.match(src, /\/finance\/review-quality\/dashboard/)
})

test('R10-FIX-C: review quality menu parent must be M type, not C type', () => {
  const sql = read('sql/finance_review_quality_permission.sql')
  // 父菜单查找必须使用 menu_type = 'M'（目录），不能是 'C'（页面）
  assert.match(sql, /menu_type\s*=\s*'M'/i)
  assert.doesNotMatch(
    sql,
    /SET\s+@finance_root\s*:=\s*\(SELECT menu_id FROM sys_menu WHERE perms\s*=\s*'finance:dashboard:view'\s+AND\s+menu_type\s*=\s*'C'/i,
    '父菜单不能查找 finance:dashboard:view 的 C 类型菜单（页面挂页面会导致动态路由异常）'
  )
})

test('R10-FIX-C2: permission SQL must include idempotent UPDATE to fix existing menu parent', () => {
  const sql = read('sql/finance_review_quality_permission.sql')
  // INSERT WHERE NOT EXISTS 不会修正已存在菜单的父级，
  // 必须有一段 UPDATE sys_menu ... WHERE perms = 'finance:reviewQuality:view' 来兜底
  assert.match(
    sql,
    /UPDATE\s+sys_menu\s+SET\s+parent_id/i,
    '必须包含 UPDATE sys_menu SET parent_id 语句，用于修正旧 SQL 创建的错误父级'
  )
  assert.match(
    sql,
    /WHERE\s+perms\s*=\s*'finance:reviewQuality:view'/i,
    'UPDATE 必须按 perms = finance:reviewQuality:view 过滤'
  )
})

test('R10-FIX-A: avgFirstResponseHours SQL must not nest aggregate functions', () => {
  const xml = read('junsong-modules/junsong-finance/src/main/resources/mapper/finance/ReviewQualityMapper.xml')
  // 提取 avgFirstResponseHours 的 select 块
  const blockMatch = xml.match(/id="avgFirstResponseHours"[^>]*>([\s\S]*?)<\/select>/)
  assert.ok(blockMatch, 'avgFirstResponseHours select must exist')
  const block = blockMatch[1]
  // MySQL 不允许 AVG(... MIN(...)) 直接嵌套（Invalid use of group function）
  assert.doesNotMatch(
    block,
    /AVG\s*\([^)]*MIN\s*\(/i,
    'AVG 内不能直接嵌套 MIN，需用子查询先分组求 MIN 再外层 AVG'
  )
  assert.doesNotMatch(
    block,
    /AVG\s*\([^)]*MAX\s*\(/i,
    'AVG 内不能直接嵌套 MAX'
  )
  assert.doesNotMatch(
    block,
    /AVG\s*\([^)]*SUM\s*\(/i,
    'AVG 内不能直接嵌套 SUM'
  )
  // 应使用子查询分组
  assert.match(block, /GROUP BY/i, '应使用 GROUP BY 子查询先按 task 分组')
})

test('R10-FIX-D: thresholdValue must be required (not nullable) in validate', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/impl/SysHealthRuleConfigServiceImpl.java')
  // validate 方法必须强制 thresholdValue 非空
  assert.match(src, /getThresholdValue\(\)\s*==\s*null/, 'validate 必须检查 thresholdValue == null')
  assert.match(src, /阈值不能为空/, '必须在 thresholdValue 为空时抛出明确异常')
  // 不应包含允许 null 的旧逻辑
  assert.doesNotMatch(
    src,
    /thresholdValue may be null/i,
    '不应保留允许 thresholdValue 为空的旧注释'
  )
})

test('R10-FIX-E: governance task generation must use config threshold, not hardcoded 20', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysDashboardController.java')
  // buildGovernanceTasks 中不应有硬编码 > 20
  const buildMethodMatch = src.match(/List<SystemGovernanceTaskVO>\s+buildGovernanceTasks\s*\([^)]*\)\s*\{([\s\S]*?)^\s{4}\}/m)
  assert.ok(buildMethodMatch, 'buildGovernanceTasks method must exist')
  const buildMethod = buildMethodMatch[1]
  assert.doesNotMatch(
    buildMethod,
    /recentLoginFailCount\s*>\s*20\b/,
    'buildGovernanceTasks 不应硬编码 > 20，必须使用配置阈值'
  )
  assert.match(
    buildMethod,
    /getThreshold\s*\(\s*["']SYS_LOGIN_FAIL_24H["']/,
    'buildGovernanceTasks 必须读取 SYS_LOGIN_FAIL_24H 配置阈值'
  )
})

test('R10-FIX-F: weekly memo must set reviewQualityScore in backend', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/DailyReviewBoardServiceImpl.java')
  assert.match(src, /fillReviewQualityScore/, '必须存在 fillReviewQualityScore 方法')
  assert.match(src, /setReviewQualityScore/, '必须调用 setReviewQualityScore 赋值')
  assert.match(src, /reviewQualityService/, '必须注入 IReviewQualityService')
})

test('R10-FIX-F: weekly memo frontend must display reviewQualityScore', () => {
  const src = read('junsong-ui-v3/src/views/index.vue')
  assert.match(src, /reviewQualityScore/, 'WeeklyMemo 接口必须包含 reviewQualityScore 字段')
  assert.match(src, /复盘质量分/, '首页必须展示复盘质量分标签')
  assert.match(src, /scoreClass/, '必须存在 scoreClass 函数用于分数样式')
})
