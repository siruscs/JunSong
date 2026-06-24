import request from '../request'

export interface StoreOpeningQuery {
  orderNo?: string
  storeName?: string
  regionName?: string
  workflowStatus?: string
}

export interface StoreOpeningFormData {
  id?: number
  orderNo?: string
  storeName: string
  storeShortName?: string
  storeType?: string
  expectedOpeningDate?: string
  regionName?: string
  regionLeader?: string
  generalManager?: string
  storeManagerName?: string
  province?: string
  city?: string
  district?: string
  addressDetail?: string
  siteArea?: number | null
  siteMode?: string
  plannedStaffCount?: number | null
  initialInvestmentAmount?: number | null
  estimatedMonthlyRevenue?: number | null
  openingReason?: string
  remark?: string
  processInstanceId?: string
  processDefinitionKey?: string
  workflowStatus?: string
  currentTaskName?: string
  submitter?: string
  submitTime?: string
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
}

export interface StoreOpeningSubmitPayload {
  processInstanceId: string
  processDefinitionKey: string
  currentTaskName: string
}

export function listStoreOpening(query?: StoreOpeningQuery) {
  return request({
    url: '/system/storeOpening/list',
    method: 'get',
    params: query,
  })
}

export function getStoreOpening(id: number) {
  return request({
    url: `/system/storeOpening/${id}`,
    method: 'get',
  })
}

export function getStoreOpeningByOrderNo(orderNo: string) {
  return request({
    url: `/system/storeOpening/order/${orderNo}`,
    method: 'get',
  })
}

export function addStoreOpening(data: StoreOpeningFormData) {
  return request({
    url: '/system/storeOpening',
    method: 'post',
    data,
  })
}

export function updateStoreOpening(data: StoreOpeningFormData) {
  return request({
    url: '/system/storeOpening',
    method: 'put',
    data,
  })
}

export function delStoreOpening(id: number) {
  return request({
    url: `/system/storeOpening/${id}`,
    method: 'delete',
  })
}

export function submitStoreOpening(id: number, data: StoreOpeningSubmitPayload) {
  return request({
    url: `/system/storeOpening/${id}/submit`,
    method: 'post',
    data,
  })
}

export function withdrawStoreOpening(id: number) {
  return request({
    url: `/system/storeOpening/${id}/withdraw`,
    method: 'post',
  })
}
