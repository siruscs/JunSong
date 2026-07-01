import request from '../request'

export function getOperatingProfitReport(data: any) {
  return request({ url: '/finance/report/profit/operating', method: 'post', data })
}

export function getExpenseAnomalyReport(data: any) {
  return request({ url: '/finance/report/expense/anomalies', method: 'post', data })
}

export function getSalesOperationReport(data: any) {
  return request({ url: '/finance/report/sale/operation', method: 'post', data })
}

export function getProfitShareSettlement(data: any) {
  return request({ url: '/finance/report/profitShare/settlement', method: 'post', data })
}
