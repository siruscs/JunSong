import request from '../request'

export function listRole(query: any) {
  return request({ url: '/system/role/list', method: 'get', params: query })
}

export function getRole(roleId: number) {
  return request({ url: '/system/role/' + roleId, method: 'get' })
}

export function addRole(data: any) {
  return request({ url: '/system/role', method: 'post', data })
}

export function updateRole(data: any) {
  return request({ url: '/system/role', method: 'put', data })
}

export function delRole(roleId: number | string) {
  return request({ url: '/system/role/' + roleId, method: 'delete' })
}

export function roleTreeselect(roleId?: number) {
  const url = roleId ? '/system/menu/roleMenuTreeselect/' + roleId : '/system/menu/treeselect'
  return request({ url, method: 'get' })
}

export function roleDeptTreeselect(roleId: number) {
  return request({ url: '/system/role/deptTree/' + roleId, method: 'get' })
}

export function dataScope(data: any) {
  return request({ url: '/system/role/dataScope', method: 'put', data })
}

export function changeRoleStatus(roleId: number, status: string) {
  const data = { roleId, status }
  return request({ url: '/system/role/changeStatus', method: 'put', data })
}

export function allocatedUserList(query: any) {
  return request({ url: '/system/role/authUser/allocatedList', method: 'get', params: query })
}

export function unallocatedUserList(query: any) {
  return request({ url: '/system/role/authUser/unallocatedList', method: 'get', params: query })
}

export function authUserCancel(data: any) {
  return request({ url: '/system/role/authUser/cancel', method: 'put', data })
}

export function authUserCancelAll(data: any) {
  return request({ url: '/system/role/authUser/cancelAll', method: 'put', params: data })
}

export function authUserSelectAll(data: any) {
  return request({ url: '/system/role/authUser/selectAll', method: 'put', params: data })
}

export function authUser(userId: number, roleIds: string) {
  return request({ url: '/system/user/authRole', method: 'put', params: { userId, roleIds } })
}
