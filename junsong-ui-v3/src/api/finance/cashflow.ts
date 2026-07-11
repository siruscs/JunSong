import request from '@/api/request'

export function getCashflowDashboard(data: Record<string, any>) {
  return request({ url: '/finance/cashflow/dashboard', method: 'post', data })
}
