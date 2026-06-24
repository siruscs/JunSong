import request from '../request'

export function listMenu(query: any) {
  return request({ url: '/system/menu/list', method: 'get', params: query })
}

export function getMenu(menuId: number) {
  return request({ url: '/system/menu/' + menuId, method: 'get' })
}

export function addMenu(data: any) {
  return request({ url: '/system/menu', method: 'post', data })
}

export function updateMenu(data: any) {
  return request({ url: '/system/menu', method: 'put', data })
}

export function delMenu(menuId: number) {
  return request({ url: '/system/menu/' + menuId, method: 'delete' })
}

export function treeselect() {
  return request({ url: '/system/menu/treeselect', method: 'get' })
}

export function roleMenuTreeselect(roleId: number) {
  return request({ url: '/system/menu/roleMenuTreeselect/' + roleId, method: 'get' })
}
