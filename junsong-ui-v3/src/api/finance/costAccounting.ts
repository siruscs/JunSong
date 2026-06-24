import request from '../request'

export function listCostAccounting(query: any) {
  return request({ url: '/finance/costAccounting/list', method: 'get', params: query })
}

export function getCostAccounting(id: number) {
  return request({ url: '/finance/costAccounting/detail/' + id, method: 'get' })
}

export function addCostAccounting(data: any) {
  return request({ url: '/finance/costAccounting', method: 'post', data })
}

export function updateCostAccounting(data: any) {
  return request({ url: '/finance/costAccounting', method: 'put', data })
}

export function delCostAccounting(id: number) {
  return request({ url: '/finance/costAccounting/' + id, method: 'delete' })
}

export function getRealTimeStats(query: any) {
  return request({ url: '/finance/costAccounting/realTime', method: 'get', params: query })
}

export function previewCostAccounting(startDate: string, endDate: string, deptId?: number) {
  return request({
    url: '/finance/costAccounting/preview',
    method: 'get',
    params: { startDate, endDate, deptId },
  })
}

export function checkUnverifiedExpense(query: any) {
  return request({ url: '/finance/costAccounting/checkUnverifiedExpense', method: 'get', params: query })
}
