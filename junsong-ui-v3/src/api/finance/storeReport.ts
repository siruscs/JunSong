import request from '../request'

export interface StoreReportQuery {
  deptId: number
  startTime?: string
  endTime?: string
  timeType?: 'day' | 'week' | 'month'
}

export interface AuthorizedStoreReportQuery {
  deptIds?: number[]
  startTime?: string
  endTime?: string
  timeType?: 'day' | 'week' | 'month'
}

export function getStoreOperationSummary(data: StoreReportQuery) {
  return request({
    url: '/finance/report/store/summary',
    method: 'post',
    data
  })
}

export function getAuthorizedStorePortfolio(data: AuthorizedStoreReportQuery) {
  return request({
    url: '/finance/report/store/authorized/portfolio',
    method: 'post',
    data
  })
}
