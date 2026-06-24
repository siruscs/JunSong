import request from '../request'

export function listPointsRule(query: any) {
  return request({ url: '/member/pointsRule/list', method: 'get', params: query })
}

export function getPointsRule(ruleId: number) {
  return request({ url: '/member/pointsRule/' + ruleId, method: 'get' })
}

export function addPointsRule(data: any) {
  return request({ url: '/member/pointsRule', method: 'post', data })
}

export function updatePointsRule(data: any) {
  return request({ url: '/member/pointsRule', method: 'put', data })
}

export function delPointsRule(ruleIds: string | number) {
  return request({ url: '/member/pointsRule/' + ruleIds, method: 'delete' })
}

export function getEffectiveRule() {
  return request({ url: '/member/pointsRule/effective', method: 'get' })
}