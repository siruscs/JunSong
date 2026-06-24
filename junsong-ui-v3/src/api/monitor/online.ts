import request from '../request'

export function listOnline(query: any) {
  return request({ url: '/system/online/list', method: 'get', params: query })
}

export function forceLogout(tokenId: string) {
  return request({ url: '/system/online/' + tokenId, method: 'delete' })
}
