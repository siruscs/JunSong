import request from '../request'

export function listDept(query: any) {
  return request({ url: '/system/dept/list', method: 'get', params: query })
}

export function getDept(deptId: number) {
  return request({ url: '/system/dept/' + deptId, method: 'get' })
}

export function addDept(data: any) {
  return request({ url: '/system/dept', method: 'post', data })
}

export function updateDept(data: any) {
  return request({ url: '/system/dept', method: 'put', data })
}

export function delDept(deptId: number) {
  return request({ url: '/system/dept/' + deptId, method: 'delete' })
}

export function treeselect() {
  return request({ url: '/system/dept/treeselect', method: 'get' })
}

export function roleDeptTreeselect(roleId: number) {
  return request({ url: '/system/dept/roleDeptTreeselect/' + roleId, method: 'get' })
}

export function getDeptTreeSelect() {
  return request({ url: '/system/dept/treeselect', method: 'get' })
}
