import request from '../request'

export function listTenant(query: any) {
  return request({ url: '/system/tenant/list', method: 'get', params: query })
}

export function getTenant(tenantId: number) {
  return request({ url: '/system/tenant/' + tenantId, method: 'get' })
}

export function addTenant(data: any) {
  return request({ url: '/system/tenant', method: 'post', data })
}

export function updateTenant(data: any) {
  return request({ url: '/system/tenant', method: 'put', data })
}

export function delTenant(tenantId: number) {
  return request({ url: '/system/tenant/' + tenantId, method: 'delete' })
}

export function changeTenantStatus(tenantId: number, status: string) {
  return request({ url: '/system/tenant/changeStatus', method: 'put', data: { tenantId, status } })
}
