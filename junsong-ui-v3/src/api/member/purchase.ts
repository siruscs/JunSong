import request from '../request'

export interface MemberPurchaseQuery {
  purchaseNo?: string
  customerName?: string
  customerType?: string
  paymentStatus?: string
  deliveryStatus?: string
  beginTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}

export function listMemberPurchases(query: MemberPurchaseQuery) {
  return request({ url: '/member/purchase/list', method: 'get', params: query })
}

export function exportMemberPurchases(query: MemberPurchaseQuery) {
  return request({ url: '/member/purchase/export', method: 'post', params: query, responseType: 'blob' })
}

export function createMemberPurchase(data: Record<string, unknown>) {
  return request({ url: '/member/purchase', method: 'post', data })
}

export function updateMemberPurchase(purchaseId: number, data: Record<string, unknown>) {
  return request({ url: '/member/purchase/' + purchaseId, method: 'put', data })
}

export function getMemberPurchase(purchaseId: number) {
  return request({ url: '/member/purchase/' + purchaseId, method: 'get' })
}

export function getMemberPurchaseStatistics(query: MemberPurchaseQuery = {}) {
  return request({ url: '/member/purchase/statistics', method: 'get', params: query })
}

export function receiveMemberPurchasePayment(purchaseId: number, data: Record<string, unknown>) {
  return request({ url: '/member/purchase/' + purchaseId + '/payment', method: 'post', data })
}

export function updateMemberPurchasePayment(purchaseId: number, paymentId: number, data: Record<string, unknown>) {
  return request({ url: `/member/purchase/${purchaseId}/payment/${paymentId}`, method: 'put', data })
}

export function deliverMemberPurchase(purchaseId: number, data: Record<string, unknown>) {
  return request({ url: '/member/purchase/' + purchaseId + '/delivery', method: 'post', data })
}

export function updateMemberPurchaseDelivery(purchaseId: number, deliveryId: number, data: Record<string, unknown>) {
  return request({ url: `/member/purchase/${purchaseId}/delivery/${deliveryId}`, method: 'put', data })
}

export function cancelMemberPurchase(purchaseId: number) {
  return request({ url: '/member/purchase/' + purchaseId + '/cancel', method: 'put' })
}

export function bindMemberPurchase(purchaseId: number, memberId: number) {
  return request({ url: `/member/purchase/${purchaseId}/bind-member/${memberId}`, method: 'put' })
}

export function createMemberPurchaseReturn(data: Record<string, unknown>) {
  return request({ url: '/member/purchase-return', method: 'post', data })
}

export function listMemberPurchaseReturns(query: Record<string, unknown> = {}) {
  return request({ url: '/member/purchase-return/list', method: 'get', params: query })
}

export function getMemberPurchaseReturn(returnId: number) {
  return request({ url: '/member/purchase-return/' + returnId, method: 'get' })
}

export function completeMemberPurchaseReturn(returnId: number) {
  return request({ url: `/member/purchase-return/${returnId}/complete`, method: 'put' })
}
