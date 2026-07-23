import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const dashboardApi = fs.readFileSync(new URL('../src/api/dashboard.js', import.meta.url), 'utf8')

// 1. 统一指标接口定义
test('dashboard.js exports getOperatingMetrics for unified metrics', () => {
  assert.match(dashboardApi, /export function getOperatingMetrics/)
  assert.match(dashboardApi, /url: '\/finance\/operatingMetrics'/)
  assert.match(dashboardApi, /method: 'POST'/)
})

// 2. 统一指标接口传递 deptId 和上下文元数据
test('getOperatingMetrics passes params and context meta options', () => {
  assert.match(dashboardApi, /data: params/)
  assert.match(dashboardApi, /silent: options\.silent !== false/)
  assert.match(dashboardApi, /withContextMeta: options\.withContextMeta !== false/)
})

// 3. 旧接口保留兼容
test('legacy dashboard endpoints remain for backward compatibility', () => {
  assert.match(dashboardApi, /export function getMpDashboardOverview/)
  assert.match(dashboardApi, /export function getMpDashboardStats/)
  assert.match(dashboardApi, /export function getMpDashboardTrend/)
  assert.match(dashboardApi, /旧版首页统计接口（保留兼容）/)
  assert.match(dashboardApi, /旧版首页趋势接口（保留兼容）/)
})

// 4. PC API 文件存在统一指标接口
test('PC operatingMetrics.ts exports getOperatingMetrics', () => {
  const pcApi = fs.readFileSync(new URL('../../junsong-ui-v3/src/api/finance/operatingMetrics.ts', import.meta.url), 'utf8')
  assert.match(pcApi, /export function getOperatingMetrics/)
  assert.match(pcApi, /url: '\/finance\/operatingMetrics'/)
  assert.match(pcApi, /method: 'post'/)
  // 类型定义
  assert.match(pcApi, /interface OperatingMetric/)
  assert.match(pcApi, /code:/)
  assert.match(pcApi, /value:/)
  assert.match(pcApi, /unit:/)
  assert.match(pcApi, /period:/)
  assert.match(pcApi, /scope:/)
  assert.match(pcApi, /source:/)
  assert.match(pcApi, /drillDownRoute/)
})

// 5. 后端统一指标控制器存在
test('backend FinanceOperatingMetricController exists with correct endpoint', () => {
  const controller = fs.readFileSync(new URL('../../junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller/FinanceOperatingMetricController.java', import.meta.url), 'utf8')
  assert.match(controller, /@RequestMapping\("\/operatingMetrics"\)/)
  assert.match(controller, /@PostMapping/)
  assert.match(controller, /@RequiresPermissions\("finance:dashboard:operation"\)/)
  assert.match(controller, /getOperatingMetrics/)
})

// 6. 后端统一指标域名类包含完整契约字段
test('OperatingMetric domain class has all contract fields', () => {
  const domain = fs.readFileSync(new URL('../../junsong-modules/junsong-finance/src/main/java/com/junsong/finance/domain/vo/OperatingMetric.java', import.meta.url), 'utf8')
  assert.match(domain, /private String code/)
  assert.match(domain, /private BigDecimal value/)
  assert.match(domain, /private String unit/)
  assert.match(domain, /private Period period/)
  assert.match(domain, /private Scope scope/)
  assert.match(domain, /private Source source/)
  assert.match(domain, /private String drillDownRoute/)
  // Period 内嵌类型
  assert.match(domain, /class Period/)
  assert.match(domain, /String type/)
  assert.match(domain, /String start/)
  assert.match(domain, /String end/)
  // Scope 内嵌类型
  assert.match(domain, /class Scope/)
  assert.match(domain, /List<Long> deptIds/)
  assert.match(domain, /String tenantId/)
  // Source 内嵌类型
  assert.match(domain, /class Source/)
  assert.match(domain, /String module/)
  assert.match(domain, /String endpoint/)
})

// 7. 后端服务实现使用授权门店交集模型
test('FinanceOperatingMetricServiceImpl uses authorized dept intersection', () => {
  const service = fs.readFileSync(new URL('../../junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinanceOperatingMetricServiceImpl.java', import.meta.url), 'utf8')
  assert.match(service, /resolveAuthorizedDeptIds/)
  assert.match(service, /SecurityUtils\.isAdmin/)
  assert.match(service, /RemoteUserService/)
  assert.match(service, /getUserDeptList/)
  assert.match(service, /SENTINEL_DEPT_IDS/)
  assert.match(service, /Collections\.singletonList\(-1L\)/)
  // 交集逻辑
  assert.match(service, /filter\(finalAllowed::contains\)/)
  // 金额 scale 2 HALF_UP
  assert.match(service, /setScale\(2, RoundingMode\.HALF_UP\)/)
  // 复用现有 finance service
  assert.match(service, /IFinanceReportService/)
  assert.match(service, /getOperationDashboard/)
})

// 8. 后端服务返回 10 个指标
test('service returns exactly 10 unified metrics', () => {
  const service = fs.readFileSync(new URL('../../junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinanceOperatingMetricServiceImpl.java', import.meta.url), 'utf8')
  assert.match(service, /buildMetric\("todaySales"/)
  assert.match(service, /buildMetric\("todayExpense"/)
  assert.match(service, /buildMetric\("netCashflow"/)
  assert.match(service, /buildMetric\("receivableBalance"/)
  assert.match(service, /buildMetric\("overdueReceivable"/)
  assert.match(service, /buildMetric\("inventoryRisk"/)
  assert.match(service, /buildMetric\("todayNewMembers"/)
  assert.match(service, /buildMetric\("activeMembers"/)
  assert.match(service, /buildMetric\("unverifiedAmount"/)
  assert.match(service, /buildMetric\("pendingTaskCount"/)
})

// 9. 后端服务使用跨模块 JdbcTemplate 查询会员和任务计数
test('service uses JdbcTemplate for cross-module member and task counts', () => {
  const service = fs.readFileSync(new URL('../../junsong-modules/junsong-finance/src/main/java/com/junsong/finance/service/impl/FinanceOperatingMetricServiceImpl.java', import.meta.url), 'utf8')
  // 负库存查询
  assert.match(service, /fin_stock_ledger/)
  assert.match(service, /after_quantity < 0/)
  // 会员新增查询
  assert.match(service, /mem_member/)
  assert.match(service, /DATE\(create_time\)=CURDATE/)
  // 活跃会员查询（近30天）
  assert.match(service, /DATE_SUB\(CURDATE\(\), INTERVAL 30 DAY\)/)
  // 待办任务查询
  assert.match(service, /sys_operating_task/)
  assert.match(service, /status IN \('PENDING','IN_PROGRESS','REOPENED'\)/)
})

// 10. 后端测试覆盖关键场景
test('backend test covers all required scenarios', () => {
  const test = fs.readFileSync(new URL('../../junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/FinanceOperatingMetricServiceImplTest.java', import.meta.url), 'utf8')
  // 10 个指标
  assert.match(test, /returnsAllTenMetricsWithCompleteStructure/)
  // 金额精度
  assert.match(test, /amountsUseScale2HalfUp/)
  // admin 不限制
  assert.match(test, /adminUserNotRestrictedByDept/)
  // 非 admin 交集
  assert.match(test, /nonAdminUserIntersectsAuthorizedDepts/)
  // fail-closed
  assert.match(test, /noAuthorizedDeptsFailsClosed/)
  // 净现金流计算
  assert.match(test, /netCashflowCalculatedAsPaymentsMinusExpense/)
  // 待核销金额
  assert.match(test, /unverifiedAmountIsExpensePlusAdvance/)
  // drillDownRoute
  assert.match(test, /drillDownRoutesPointToValidPages/)
  // 一致性
  assert.match(test, /periodAndScopeAreConsistentAcrossMetrics/)
})
