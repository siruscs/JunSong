import request from '../request'

// 查询应用列表
export function listApp(query: any) {
  return request({ url: '/open/app/list', method: 'get', params: query }) as Promise<any>
}

// 查询应用详情
export function getApp(appId: number) {
  return request({ url: '/open/app/' + appId, method: 'get' })
}

// 新增应用
export function addApp(data: any) {
  return request({ url: '/open/app', method: 'post', data })
}

// 修改应用
export function updateApp(data: any) {
  return request({ url: '/open/app', method: 'put', data })
}

// 删除应用
export function delApp(appId: number | number[]) {
  return request({ url: '/open/app/' + appId, method: 'delete' })
}

// 审批通过应用
export function approveApp(appId: number) {
  return request({ url: '/open/app/approve/' + appId, method: 'put' })
}

// 驳回应用
export function rejectApp(appId: number, rejectReason: string) {
  return request({ url: '/open/app/reject/' + appId, method: 'put', params: { rejectReason } })
}

// 查询应用的API Key列表
export function listAppKeys(appId: number) {
  return request({ url: '/open/app/keys/' + appId, method: 'get' })
}

// 查询所有API Key列表(分页)
export function listAllKeys(query: any) {
  return request({ url: '/open/app/keys/list', method: 'get', params: query })
}

// 修改API Key状态(启用/停用)
export function changeKeyStatus(data: any) {
  return request({ url: '/open/app/keys/changeStatus', method: 'put', data })
}
