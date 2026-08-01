import { request } from './index.js'

export function getStockValueReport(params = {}) {
  return request({
    url: '/finance/report/stock/value',
    method: 'POST',
    data: params
  })
}
