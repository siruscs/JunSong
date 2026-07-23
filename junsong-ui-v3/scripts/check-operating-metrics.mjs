import { readFileSync, existsSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(new URL('..', import.meta.url).pathname)

function read(path) {
  return readFileSync(resolve(root, path), 'utf8')
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

// ── 1. 统一指标 API 客户端契约 ──

const metricsApi = read('src/api/finance/operatingMetrics.ts')
assert(
  metricsApi.includes("url: '/finance/operatingMetrics'") && metricsApi.includes("method: 'post'"),
  'operatingMetrics API must call POST /finance/operatingMetrics',
)
assert(
  metricsApi.includes('interface OperatingMetric'),
  'OperatingMetric interface must be defined',
)
assert(
  metricsApi.includes('interface OperatingMetricQueryParams'),
  'OperatingMetricQueryParams interface must be defined',
)

// 验证响应契约字段（与后端 OperatingMetric.java 一致）
for (const field of ['code', 'value', 'unit', 'period', 'scope', 'source', 'drillDownRoute']) {
  assert(metricsApi.includes(`${field}:`), `OperatingMetric must define field: ${field}`)
}
// 验证 period 子字段
for (const field of ['type', 'start', 'end']) {
  assert(metricsApi.includes(`${field}:`), `OperatingMetric.period must define field: ${field}`)
}
// 验证 scope 子字段
assert(metricsApi.includes('deptIds:'), 'OperatingMetric.scope must define deptIds')
assert(metricsApi.includes('tenantId:'), 'OperatingMetric.scope must define tenantId')
// 验证 source 子字段
assert(metricsApi.includes('module:'), 'OperatingMetric.source must define module')
assert(metricsApi.includes('endpoint:'), 'OperatingMetric.source must define endpoint')

// 查询参数契约
assert(metricsApi.includes('deptIds?'), 'OperatingMetricQueryParams must allow deptIds')
assert(metricsApi.includes('startTime?'), 'OperatingMetricQueryParams must allow startTime')
assert(metricsApi.includes('endTime?'), 'OperatingMetricQueryParams must allow endTime')
assert(metricsApi.includes('timeType?'), 'OperatingMetricQueryParams must allow timeType')

// 导出函数
assert(
  metricsApi.includes('export function getOperatingMetrics'),
  'getOperatingMetrics function must be exported',
)

// ── 2. 旧接口兼容性（不能删除）──

const legacyDashboard = read('src/api/finance/dashboard.ts')
assert(
  legacyDashboard.includes("url: '/finance/dashboard/stats'") &&
    legacyDashboard.includes("method: 'get'"),
  'legacy getFinDashboardStats must be preserved (GET /finance/dashboard/stats)',
)
assert(
  legacyDashboard.includes("url: '/finance/dashboard/operation'") &&
    legacyDashboard.includes("method: 'post'"),
  'legacy getOperationDashboard must be preserved (POST /finance/dashboard/operation)',
)

// ── 3. 现有页面未被破坏（Phase 5 不强制迁移，保留兼容）──

assert(existsSync(resolve(root, 'src/views/dashboard/StoreDashboard.vue')), 'StoreDashboard must exist')
assert(existsSync(resolve(root, 'src/views/finance/overview/index.vue')), 'finance overview must exist')

// StoreDashboard 仍使用既有 API（未被 Phase 5 破坏）
const storeDashboard = read('src/views/dashboard/StoreDashboard.vue')
assert(
  storeDashboard.includes("from '@/api/finance/dailyReview'"),
  'StoreDashboard must still import dailyReview API (not broken by Phase 5)',
)

// finance overview 仍使用既有 operation dashboard 接口
const financeOverview = read('src/views/finance/overview/index.vue')
assert(
  financeOverview.includes('getOperationDashboard') || financeOverview.includes('/dashboard/operation'),
  'finance overview must still call the legacy operation dashboard (gradual migration)',
)

console.log('operating metrics contract checks passed')
