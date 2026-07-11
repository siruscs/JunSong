import { request } from '@/api/index.js'

export function getTodoTasks() {
  return request({ url: '/task/todo', method: 'GET' })
}

export function getDoneTasks() {
  return request({ url: '/task/done', method: 'GET' })
}

export function getAppliedTasks() {
  return request({ url: '/task/applied', method: 'GET' })
}

export function getTaskDetail(taskId) {
  return request({ url: '/task/' + taskId, method: 'GET' })
}

export function approveTask(taskId, data) {
  return request({ url: '/task/' + taskId + '/approve', method: 'POST', data })
}

export function rejectTask(taskId, data) {
  return request({ url: '/task/' + taskId + '/reject', method: 'POST', data })
}

export function getNotifications(params) {
  return request({ url: '/system/notification/list', method: 'GET', data: params })
}

export function getUnreadCount() {
  return request({ url: '/system/notification/unread-count', method: 'GET' })
}

export function markRead(id) {
  return request({ url: '/system/notification/read/' + id, method: 'PUT' })
}

export function markAllRead() {
  return request({ url: '/system/notification/read-all', method: 'PUT' })
}
