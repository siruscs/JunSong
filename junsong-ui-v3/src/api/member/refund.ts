import request from '../request'

export interface RefundQuery {
  refundNo?: string
  memberName?: string
  storeName?: string
  workflowStatus?: string
}

export interface RefundFormData {
  id?: number
  refundNo?: string
  memberId?: number | null
  memberName: string
  memberPhone?: string
  storeId?: number | null
  storeName?: string
  refundAmount: number | null
  refundReason?: string
  refundRemark?: string
  storeApprover?: string
  financeApprover?: string
  processInstanceId?: string
  processDefinitionKey?: string
  workflowStatus?: string
  currentTaskName?: string
  submitter?: string
  submitTime?: string
  createTime?: string
  updateTime?: string
}

export interface RefundSubmitPayload {
  processInstanceId: string
  processDefinitionKey: string
  currentTaskName: string
}

export function listRefund(query?: RefundQuery) {
  return request({ url: '/member/refund/list', method: 'get', params: query })
}

export function getRefund(id: number) {
  return request({ url: `/member/refund/${id}`, method: 'get' })
}

export function getRefundByRefundNo(refundNo: string) {
  return request({ url: `/member/refund/no/${refundNo}`, method: 'get' })
}

export function addRefund(data: RefundFormData) {
  return request({ url: '/member/refund', method: 'post', data })
}

export function updateRefund(data: RefundFormData) {
  return request({ url: '/member/refund', method: 'put', data })
}

export function delRefund(ids: number | number[]) {
  const idPath = Array.isArray(ids) ? ids.join(',') : ids
  return request({ url: `/member/refund/${idPath}`, method: 'delete' })
}

export function submitRefund(id: number, data: RefundSubmitPayload) {
  return request({ url: `/member/refund/${id}/submit`, method: 'post', data })
}

export function withdrawRefund(id: number) {
  return request({ url: `/member/refund/${id}/withdraw`, method: 'post' })
}
