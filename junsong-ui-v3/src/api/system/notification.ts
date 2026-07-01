import request from '../request'

export interface NotificationItem {
  id: number
  userId: number
  title: string
  content: string
  type: string
  linkUrl: string
  bizId: string
  isRead: string
  createTime: string
  readTime: string | null
}

export function listNotification(params?: any) {
  return request({
    url: '/system/notification/list',
    method: 'get',
    params,
  })
}

export function getUnreadCount() {
  return request({
    url: '/system/notification/unread-count',
    method: 'get',
    silentError: true,
  })
}

export function markRead(id: number) {
  return request({
    url: `/system/notification/read/${id}`,
    method: 'put',
  })
}

export function markAllRead() {
  return request({
    url: '/system/notification/read-all',
    method: 'put',
  })
}

export function delNotification(ids: number[]) {
  return request({
    url: `/system/notification/${ids.join(',')}`,
    method: 'delete',
  })
}
