import request from '../request'

export function getPointsOperationSummary(deptIds?: number[]) {
  return request({
    url: '/member/dashboard/points-summary',
    method: 'get',
    params: deptIds && deptIds.length > 0 ? { deptIds: deptIds.join(',') } : undefined,
  })
}
