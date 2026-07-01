import request from '@/utils/request'

export interface FieldPermissionItem {
  id?: number
  processDefinitionKey: string
  activityId: string
  fieldKey: string
  fieldLabel?: string
  permission: string
}

export function listFieldPermission(params?: any) {
  return request({ url: '/workflow/field-permission/list', method: 'get', params })
}

export function getFieldPermission(processDefinitionKey: string, activityId: string) {
  return request({ url: '/workflow/field-permission', method: 'get', params: { processDefinitionKey, activityId } })
}

export function addFieldPermission(data: Partial<FieldPermissionItem>) {
  return request({ url: '/workflow/field-permission', method: 'post', data })
}

export function updateFieldPermission(data: Partial<FieldPermissionItem>) {
  return request({ url: '/workflow/field-permission', method: 'put', data })
}

export function delFieldPermission(id: number) {
  return request({ url: `/workflow/field-permission/${id}`, method: 'delete' })
}
