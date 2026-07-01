import request from '../request'

export function getDashboardStats() {
  return request({ url: '/system/dashboard/stats', method: 'get' })
}

export function getDashboardHealth() {
  return request({ url: '/system/dashboard/health', method: 'get' })
}

export function getDashboardGovernance() {
  return request({ url: '/system/dashboard/governance', method: 'get' })
}
