import request from '../request'

export function listOperlog(query: any) {
  return request({ url: '/system/operlog/list', method: 'get', params: query })
}

export function delOperlog(operId: number | string) {
  return request({ url: '/system/operlog/' + operId, method: 'delete' })
}

export function cleanOperlog() {
  return request({ url: '/system/operlog/clean', method: 'delete' })
}

export function getOperlog(operId: number) {
  return request({ url: '/system/operlog/' + operId, method: 'get' })
}
