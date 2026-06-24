import request from '../../request'

export function listType(query: any) {
  return request({ url: '/system/dict/type/list', method: 'get', params: query })
}

export function getType(dictId: number) {
  return request({ url: '/system/dict/type/' + dictId, method: 'get' })
}

export function addType(data: any) {
  return request({ url: '/system/dict/type', method: 'post', data })
}

export function updateType(data: any) {
  return request({ url: '/system/dict/type', method: 'put', data })
}

export function delType(dictId: number) {
  return request({ url: '/system/dict/type/' + dictId, method: 'delete' })
}

export function refreshCache() {
  return request({ url: '/system/dict/type/refreshCache', method: 'delete' })
}

export function listAllDicts() {
  return request({ url: '/system/dict/type/optionselect', method: 'get' })
}
