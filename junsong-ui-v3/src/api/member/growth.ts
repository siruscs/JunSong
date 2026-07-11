import request from '../request'

// 查询成长值记录列表
export function listGrowthRecord(query: any) {
  return request({ url: '/member/growth/list', method: 'get', params: query })
}

// 查询成长值汇总
export function getGrowthSummary(memberId: number) {
  return request({ url: '/member/growth/summary', method: 'get', params: { memberId } })
}

// 手工调整积分和成长值
export function adjustGrowth(data: any) {
  return request({ url: '/member/growth/adjust', method: 'post', data })
}

// 查询成长规则
export function getGrowthRule() {
  return request({ url: '/member/growth/rule', method: 'get' })
}

// 修改成长规则
export function updateGrowthRule(data: any) {
  return request({ url: '/member/growth/rule', method: 'put', data })
}
