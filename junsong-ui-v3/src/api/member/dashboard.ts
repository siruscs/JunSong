import request from '../request'

export function getDashboardStats(deptIds?: number[]) {
  return request({
    url: '/member/dashboard/stats',
    method: 'get',
    params: deptIds && deptIds.length > 0 ? { deptIds: deptIds.join(',') } : undefined,
  })
}

export function getDashboardTrend(deptIds?: number[]) {
  return request({
    url: '/member/dashboard/trend',
    method: 'get',
    params: deptIds && deptIds.length > 0 ? { deptIds: deptIds.join(',') } : undefined,
  })
}

export function getDashboardRanking(deptIds?: number[]) {
  return request({
    url: '/member/dashboard/ranking',
    method: 'get',
    params: deptIds && deptIds.length > 0 ? { deptIds: deptIds.join(',') } : undefined,
  })
}

export function getDashboardOperation(deptIds?: number[]) {
  return request({
    url: '/member/dashboard/operation',
    method: 'get',
    params: deptIds && deptIds.length > 0 ? { deptIds: deptIds.join(',') } : undefined,
  })
}
