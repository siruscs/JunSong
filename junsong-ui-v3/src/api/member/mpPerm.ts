import request from '../request'

export function listMpPerm(query: any) {
  return request({ url: '/member/mpPerm/list', method: 'get', params: query })
}

export function getMpPermRoles() {
  return request({ url: '/member/mpPerm/roles', method: 'get' })
}

export function getMpPermModules() {
  return request({ url: '/member/mpPerm/modules', method: 'get' })
}

export function saveMpPerm(data: any) {
  return request({ url: '/member/mpPerm', method: 'post', data })
}

export function deleteMpPerm(id: number) {
  return request({ url: '/member/mpPerm/' + id, method: 'delete' })
}

export function deleteMpPermByRole(roleId: number, deptId: number) {
  return request({ url: '/member/mpPerm/role/' + roleId + '/' + deptId, method: 'delete' })
}