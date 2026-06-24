import request from '../request'

export function listNotice(query: any) {
  return request({ url: '/system/notice/list', method: 'get', params: query })
}

export function getNotice(noticeId: number) {
  return request({ url: '/system/notice/' + noticeId, method: 'get' })
}

export function addNotice(data: any) {
  return request({ url: '/system/notice', method: 'post', data })
}

export function updateNotice(data: any) {
  return request({ url: '/system/notice', method: 'put', data })
}

export function delNotice(noticeId: number) {
  return request({ url: '/system/notice/' + noticeId, method: 'delete' })
}

export function listNoticeTop() {
  return request({ url: '/system/notice/listTop', method: 'get' })
}

export function markNoticeRead(noticeId: number) {
  return request({ url: '/system/notice/markRead', method: 'post', params: { noticeId } })
}

export function markNoticeReadAll(ids: string) {
  return request({ url: '/system/notice/markReadAll', method: 'post', params: { ids } })
}

export function listNoticeReadUsers(query: any) {
  return request({ url: '/system/notice/readUsers/list', method: 'get', params: query })
}
