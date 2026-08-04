import { request } from './index.js'

export function getSeckillStatisticsBatch(seckillIds = [], deptId) {
  const data = { seckillIds: seckillIds.join(',') }
  if (deptId !== null && deptId !== undefined) data.deptId = deptId
  return request({
    url: '/member/seckillRecord/statistics/batch',
    method: 'GET',
    data,
    silent: true,
    timeout: 12000,
    withContextMeta: true
  })
}
