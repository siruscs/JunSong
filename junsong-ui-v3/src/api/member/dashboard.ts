import request from '../request'

export function getDashboardStats() {
  return request({ url: '/member/dashboard/stats', method: 'get' })
}

export function getDashboardTrend() {
  return request({ url: '/member/dashboard/trend', method: 'get' })
}

export function getDashboardRanking() {
  return request({ url: '/member/dashboard/ranking', method: 'get' })
}