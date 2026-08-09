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

/** PC 端「功能模块调整」：获取当前排序好的模块完整定义（key/name/group）。 */
export function getMpPermModuleSort() {
  return request({ url: '/member/mpPerm/moduleSort', method: 'get' })
}

/** PC 端「功能模块调整」：按拖拽后的顺序整体保存，入参为 [{moduleKey, groupName, remark?}]。 */
export function saveMpPermModuleSort(data: any[]) {
  return request({ url: '/member/mpPerm/moduleSort', method: 'post', data, idempotencyNewKey: true })
}
