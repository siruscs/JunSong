import request from '../request'

export function getFinDashboardStats() {
  return request({ url: '/finance/dashboard/stats', method: 'get' })
}

export function getOperationDashboard(data: any) {
  return request({ url: '/finance/dashboard/operation', method: 'post', data })
}
