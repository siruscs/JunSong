import request from '../request'

const BASE = '/workflow/lowcode'

export interface LcBizObject {
  id?: number
  bizCode: string
  bizName: string
  storageMode?: string
  orderNoPrefix?: string
  workflowEnabled?: string
  processKey?: string
  fulfillmentEnabled?: string
  menuParentPath?: string
  status?: string
}

export interface LcBizField {
  id?: number
  bizCode: string
  fieldKey: string
  fieldLabel: string
  fieldType: string
  componentType?: string
  required?: string
  defaultValue?: string
  dictType?: string
  uploadConfig?: string
  fieldExt?: string
  validateRule?: string
  stage?: string
  isQuery?: string
  isList?: string
  isDetail?: string
  isProcessVar?: string
  processVarName?: string
  orderNum?: number
}

export interface LcBizInstance {
  id?: number
  bizCode?: string
  orderNo?: string
  formData?: string
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

export type LcFormData = Record<string, any>

export function getBizObject(bizCode: string) {
  return request({ url: `${BASE}/meta/object/code/${bizCode}`, method: 'get' })
}

export function listBizFields(bizCode: string) {
  return request({ url: `${BASE}/meta/field/code/${bizCode}`, method: 'get' })
}

export function listBizInstances(bizCode: string, query?: Record<string, any>) {
  return request({ url: `${BASE}/biz/${bizCode}/list`, method: 'get', params: query })
}

export function getBizInstance(bizCode: string, id: number) {
  return request({ url: `${BASE}/biz/${bizCode}/${id}`, method: 'get' })
}

export function saveBizInstance(bizCode: string, formData: LcFormData) {
  return request({ url: `${BASE}/biz/${bizCode}`, method: 'post', data: formData })
}

export function updateBizInstance(bizCode: string, id: number, formData: LcFormData) {
  return request({ url: `${BASE}/biz/${bizCode}/${id}`, method: 'put', data: formData })
}

export function deleteBizInstance(bizCode: string, ids: number | number[]) {
  const idPath = Array.isArray(ids) ? ids.join(',') : ids
  return request({ url: `${BASE}/biz/${bizCode}/${idPath}`, method: 'delete' })
}

export function submitBizInstance(bizCode: string, id: number) {
  return request({ url: `${BASE}/biz/${bizCode}/${id}/submit`, method: 'post' })
}

export function withdrawBizInstance(bizCode: string, id: number) {
  return request({ url: `${BASE}/biz/${bizCode}/${id}/withdraw`, method: 'post' })
}

export function fulfillBizInstance(bizCode: string, id: number, formData?: LcFormData) {
  return request({ url: `${BASE}/biz/${bizCode}/${id}/fulfill`, method: 'post', data: formData || {} })
}

// 查询业务动作配置（前端动态渲染按钮用）
export function listBizActions(bizCode: string) {
  return request({ url: `${BASE}/meta/action/${bizCode}`, method: 'get' })
}
