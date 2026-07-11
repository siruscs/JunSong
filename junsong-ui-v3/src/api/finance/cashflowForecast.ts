import request from '../request'

export function getCashflowForecastDashboard(data?: Record<string, any>) {
  return request({
    url: '/finance/cashflow-forecast/dashboard',
    method: 'post',
    data,
  })
}

export function createCashflowForecastSnapshot(data?: Record<string, any>) {
  return request({
    url: '/finance/cashflow-forecast/snapshot',
    method: 'post',
    data,
  })
}
