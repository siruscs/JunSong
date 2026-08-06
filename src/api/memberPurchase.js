import { request } from './index.js'

export function listMemberPurchases(params = {}) {
  return request({ url: '/member/purchase/list', method: 'GET', data: params })
}

export function getMemberPurchase(purchaseId) {
  return request({ url: `/member/purchase/${purchaseId}`, method: 'GET' })
}

export function getMemberPurchaseStatistics(params = {}) {
  return request({ url: '/member/purchase/statistics', method: 'GET', data: params })
}

export function receiveMemberPurchasePayment(purchaseId, data) {
  return request({ url: `/member/purchase/${purchaseId}/payment`, method: 'POST', data })
}

export function deliverMemberPurchase(purchaseId, data) {
  return request({ url: `/member/purchase/${purchaseId}/delivery`, method: 'POST', data })
}
