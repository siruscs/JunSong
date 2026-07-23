import { request } from './index.js'

/**
 * 小程序首页聚合看板接口（R1-R25 同步）。
 *
 * 后端按当前登录用户的租户 + 授权门店范围聚合：
 * - member / growth / points / level / segment / activity / finance 分组
 * - 未授权模块不返回对应分组
 *
 * @param {Object} [params] 预留查询参数（如指定门店 ID）
 * @returns {Promise<Object>} 聚合看板数据
 */
export function getMpDashboardOverview(params = {}) {
  return request({
    url: '/member/mp/dashboard/overview',
    method: 'GET',
    data: params
  })
}

/**
 * 旧版首页统计接口（保留兼容）。
 */
export function getMpDashboardStats() {
  return request({
    url: '/member/mp/dashboard/stats',
    method: 'GET'
  })
}

/**
 * 旧版首页趋势接口（保留兼容）。
 */
export function getMpDashboardTrend() {
  return request({
    url: '/member/mp/dashboard/trend',
    method: 'GET'
  })
}

/**
 * 统一经营指标接口（Phase 5）。
 *
 * PC 和小程序共用同一后端端点，后端负责租户/部门范围和口径统一。
 * 返回 10 个指标：销售/费用/净现金流/应收/逾期/库存风险/会员新增/活跃会员/待核销/待办任务。
 * 旧接口保留兼容，客户端逐步迁移。
 *
 * @param {Object} [params] 查询参数（deptIds / timeType / startTime / endTime）
 * @param {boolean} [options.silent] 是否静默请求
 * @param {boolean} [options.withContextMeta] 是否附加上下文元数据
 * @returns {Promise<Array>} 统一指标列表
 */
export function getOperatingMetrics(params = {}, options = {}) {
  return request({
    url: '/finance/operatingMetrics',
    method: 'POST',
    data: params,
    silent: options.silent !== false,
    withContextMeta: options.withContextMeta !== false
  })
}
