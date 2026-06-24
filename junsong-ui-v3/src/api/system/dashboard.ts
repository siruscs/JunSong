import request from '../request'

export function getDashboardHealth() {
  return request({ url: '/system/dashboard/health', method: 'get' })
}
