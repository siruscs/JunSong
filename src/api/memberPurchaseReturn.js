import { request } from './index.js'

export function listMemberPurchaseReturns(params = {}) {
  return request({ url: '/member/purchase-return/list', method: 'GET', data: params })
}

export function getMemberPurchaseReturn(returnId) {
  return request({ url: `/member/purchase-return/${returnId}`, method: 'GET' })
}

export function createMemberPurchaseReturn(data) {
  return request({ url: '/member/purchase-return', method: 'POST', data })
}
