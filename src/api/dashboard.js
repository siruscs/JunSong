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
