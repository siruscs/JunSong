import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

test('R11 store health rules exist', () => {
  const sql = read('sql/sys_health_rule_config_r11_store_health.sql')
  assert.match(sql, /STORE_PROFIT_RATE_LOW/)
  assert.match(sql, /STORE_SALES_DROP_RATE/)
  assert.match(sql, /STORE_EXPENSE_RATE_HIGH/)
  assert.match(sql, /STORE_PENDING_AMOUNT_HIGH/)
  assert.match(sql, /STORE_REVIEW_SCORE_LOW/)
})

test('store health factor VO exists', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/StoreHealthFactorVO.java')
  assert.match(src, /factorCode/)
  assert.match(src, /deductedScore/)
  assert.match(src, /suggestion/)
})

test('authorized store row exposes health V2 fields', () => {
  const src = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/AuthorizedStoreRowVO.java')
  assert.match(src, /healthLevel/)
  assert.match(src, /healthFactors/)
  assert.match(src, /healthScoreVersion/)
})

test('review knowledge SQL and controller exist', () => {
  const sql = read('sql/finance_review_knowledge.sql').toLowerCase()
  assert.match(sql, /create table if not exists finance_review_knowledge/)
  const controller = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinanceReviewKnowledgeController.java')
  assert.match(controller, /finance:reviewKnowledge:list/)
  assert.match(controller, /from-task/)
})

test('governance action no longer trusts frontend severity count directly', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysDashboardController.java')
  assert.doesNotMatch(src, /log\.getSeverity\(\).*insertGovernanceTaskLog/s)
  assert.match(src, /buildGovernanceTasks|find.*GovernanceTask|current.*task/i)
})

test('knowledge list/add/update enforce dept access boundary', () => {
  const svc = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinanceReviewKnowledgeServiceImpl.java')
  // listKnowledge must inject allowedDeptIds
  assert.match(svc, /listKnowledge.*allowedDeptIds|allowedDeptIds.*listKnowledge/s)
  // addKnowledge must verify deptId
  assert.match(svc, /addKnowledge[\s\S]*?verifyDeptAccess/)
  // updateKnowledge must load existing and verify
  assert.match(svc, /updateKnowledge[\s\S]*?selectByKnowledgeId[\s\S]*?verifyDeptAccess/)
  // mapper XML must filter by allowedDeptIds
  const xml = read('junsong-modules/junsong-finance/src/main/resources/mapper/finance/FinanceReviewKnowledgeMapper.xml')
  assert.match(xml, /allowedDeptIds/)
})

test('health trend uses real expense and risk data', () => {
  const svc = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/StoreFinanceReportServiceImpl.java')
  // R11-FIX-B: 主查询已通过 UNION ALL 包含费用数据，直接使用 totalExpense
  assert.match(svc, /totalExpense/)
  assert.match(svc, /riskLookup/)
  // R11-FIX-C: 按周期统计高风险任务数
  assert.match(svc, /selectHighRiskTaskCountByDeptsAndPeriod/)
  // netCashflowAmount in trend must NOT be hardcoded to ZERO with the old comment
  assert.doesNotMatch(svc, /趋势接口不单独查现金流/)
})

// ==================== R11-HOTFIX 覆盖扩展 ====================

test('R11-FIX-A: tenant_id migration SQL exists and is idempotent', () => {
  const sql = read('sql/sys_health_rule_config_tenant_id.sql')
  assert.match(sql, /ALTER TABLE sys_health_rule_config ADD COLUMN tenant_id/i)
  assert.match(sql, /information_schema.COLUMNS/i, '必须用 information_schema 做幂等判断')
  assert.match(sql, /uk_health_rule_tenant_code/i, '必须补租户+规则编码唯一索引')
})

test('R11-FIX-A: sys_health_rule_config base table includes tenant_id', () => {
  const sql = read('sql/sys_health_rule_config.sql')
  assert.match(sql, /tenant_id BIGINT NOT NULL/i)
  assert.match(sql, /uk_health_rule_tenant_code/i)
})

test('R11-FIX-A: SysHealthRuleConfigMapper resultMap includes tenant_id', () => {
  const xml = read('junsong-modules/junsong-system/src/main/resources/mapper/system/SysHealthRuleConfigMapper.xml')
  assert.match(xml, /property="tenantId"\s+column="tenant_id"/)
  assert.match(xml, /select rule_id, tenant_id, rule_code/)
})

test('R11-FIX-B: health trend SQL uses UNION ALL to cover expense-only periods', () => {
  const xml = read('junsong-modules/junsong-finance/src/main/resources/mapper/finance/StoreFinanceReportMapper.xml')
  const blockMatch = xml.match(/id="selectHealthTrendByDepts"[^>]*>([\s\S]*?)<\/select>/)
  assert.ok(blockMatch, 'selectHealthTrendByDepts must exist')
  const block = blockMatch[1]
  assert.match(block, /union all/i, '必须使用 UNION ALL 全外连接销售和费用周期')
  assert.match(block, /fin_expense/i, '费用子查询必须存在')
  assert.match(block, /fin_sale_record/i, '销售子查询必须存在')
  // R11-FIX2-A: 费用子查询不能有 status='1' 过滤，需与健康矩阵全量费用口径一致
  assert.doesNotMatch(
    block,
    /e\.status\s*=\s*['"]1['"]/i,
    '费用子查询不能加 status=1 过滤，需与健康矩阵 total_expense 全量费用口径一致'
  )
})

test('R11-FIX2-A: health trend expense aligns with matrix total_expense (no status filter)', () => {
  const xml = read('junsong-modules/junsong-finance/src/main/resources/mapper/finance/StoreFinanceReportMapper.xml')
  // 健康矩阵的费用子查询（selectAuthorizedStoreRows）不应有 status 过滤
  const matrixMatch = xml.match(/id="selectAuthorizedStoreRows"[^>]*>([\s\S]*?)<\/select>/)
  assert.ok(matrixMatch, 'selectAuthorizedStoreRows must exist')
  const matrixBlock = matrixMatch[1]
  // 矩阵的费用子查询：sum(expense_amount) as total_expense，无 status='1' 过滤
  const matrixExpenseMatch = matrixBlock.match(/sum\(expense_amount\)\s+as\s+total_expense/i)
  assert.ok(matrixExpenseMatch, '矩阵必须汇总 total_expense')
  // 矩阵费用子查询不应有 status='1' 过滤（允许 status='0' 用于 unverified_expense）
  const matrixExpenseSub = matrixBlock.match(/from fin_expense[\s\S]*?group by dept_id/)
  assert.ok(matrixExpenseSub, '矩阵费用子查询必须存在')
  assert.doesNotMatch(
    matrixExpenseSub[0],
    /\bstatus\s*=\s*['"]1['"]/i,
    '矩阵 total_expense 不应过滤 status=1（全量费用）'
  )
})

test('R11-FIX2-B: tenant_id migration SQL drops old uk_health_rule_code index', () => {
  const sql = read('sql/sys_health_rule_config_tenant_id.sql')
  assert.match(sql, /DROP INDEX uk_health_rule_code/i, '必须包含 DROP INDEX uk_health_rule_code 清理旧索引')
  assert.match(sql, /information_schema.STATISTICS/i, 'drop 必须用 information_schema 幂等判断')
  // R11-FIX2-B: 校验 SQL 关键字拼写正确，防止 DEALLOCARE 之类笔误导致执行失败
  assert.doesNotMatch(sql, /DEALLOCARE/i, '不得出现 DEALLOCARE 拼写错误')
  assert.match(sql, /DEALLOCATE PREPARE stmt3/i, '必须包含 DEALLOCATE PREPARE stmt3')
})

test('R11-FIX2-C: selectExpenseTrendByDepts removed from mapper and stubs', () => {
  // Mapper 接口不应再声明此方法
  const mapper = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/StoreFinanceReportMapper.java')
  assert.doesNotMatch(mapper, /selectExpenseTrendByDepts/, 'Mapper 接口不应再声明 selectExpenseTrendByDepts')
  // XML 不应再包含此 select
  const xml = read('junsong-modules/junsong-finance/src/main/resources/mapper/finance/StoreFinanceReportMapper.xml')
  assert.doesNotMatch(xml, /id="selectExpenseTrendByDepts"/, 'XML 不应再包含 selectExpenseTrendByDepts')
})

test('R11-FIX-C: high risk task count has per-period query method', () => {
  const mapper = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/mapper/StoreFinanceReportMapper.java')
  assert.match(mapper, /selectHighRiskTaskCountByDeptsAndPeriod/)
  const xml = read('junsong-modules/junsong-finance/src/main/resources/mapper/finance/StoreFinanceReportMapper.xml')
  assert.match(xml, /id="selectHighRiskTaskCountByDeptsAndPeriod"/)
  const blockMatch = xml.match(/id="selectHighRiskTaskCountByDeptsAndPeriod"[^>]*>([\s\S]*?)<\/select>/)
  assert.ok(blockMatch)
  assert.match(blockMatch[1], /periodLabel/i, '按周期查询必须包含 periodLabel')
})

test('R11-FIX-D: knowledge frontend has scope field (GLOBAL/STORE)', () => {
  const vue = read('junsong-ui-v3/src/views/finance/reviewKnowledge/index.vue')
  assert.match(vue, /scope:\s*['"]GLOBAL['"]/)
  assert.match(vue, /全局知识/)
  assert.match(vue, /指定门店/)
  assert.match(vue, /editForm\.scope === 'GLOBAL'/)
})

test('health score consumes STORE_REVIEW_SCORE_LOW rule', () => {
  const svc = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/StoreFinanceReportServiceImpl.java')
  assert.match(svc, /STORE_REVIEW_SCORE_LOW/)
  assert.match(svc, /computeReviewScore/)
  // AuthorizedStoreRowVO must expose reviewScore
  const vo = read('junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/AuthorizedStoreRowVO.java')
  assert.match(vo, /reviewScore/)
})

test('knowledge frontend has add button', () => {
  const vue = read('junsong-ui-v3/src/views/finance/reviewKnowledge/index.vue')
  assert.match(vue, /handleAdd/)
  assert.match(vue, /addReviewKnowledge/)
})
