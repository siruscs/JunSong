import request from '../request'

export function listLogininfor(query: any) {
  return request({ url: '/system/logininfor/list', method: 'get', params: query })
}

export function delLogininfor(infoId: number | string) {
  return request({ url: '/system/logininfor/' + infoId, method: 'delete' })
}

export function cleanLogininfor() {
  return request({ url: '/system/logininfor/clean', method: 'delete' })
}

export function unlockUser(userName: string) {
  return request({ url: '/system/logininfor/unlock/' + userName, method: 'get' })
}
