import request from '../request'

// 查询分润记录列表
export function listProfitShare(query: any) {
  return request({
    url: '/finance/profitShare/list',
    method: 'get',
    params: query
  })
}

// 查询分润记录详细
export function getProfitShare(shareId: any) {
  return request({
    url: '/finance/profitShare/' + shareId,
    method: 'get'
  })
}

// 按核算周期结转分润
export function carryForwardProfitShare(periodId: any) {
  return request({
    url: '/finance/profitShare/carryForward/' + periodId,
    method: 'post'
  })
}

// 作废分润记录
export function cancelProfitShare(shareIds: any) {
  return request({
    url: '/finance/profitShare/' + shareIds,
    method: 'delete'
  })
}
