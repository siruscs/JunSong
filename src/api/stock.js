import { request } from './index.js'
import { workContext } from '@/utils/workContext.js'

export function getStockValueReport(params = {}) {
  const currentDeptId = workContext.snapshot().currentDeptId
  return request({
    url: '/finance/report/stock/value',
    method: 'POST',
    data: { ...params, deptIds: params.deptIds || (currentDeptId ? [currentDeptId] : []) }
  })
}
