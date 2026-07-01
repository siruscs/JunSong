import request from '@/utils/request'

export interface TimeoutItem {
  id?: number
  processDefinitionId?: string
  processDefinitionKey: string
  activityId: string
  activityName?: string
  timeoutMinutes: number
  escalationType: string
  escalationTarget?: string
  isWorkday: string
}

export function listTimeout(params?: any) {
  return request({ url: '/workflow/timeout/list', method: 'get', params })
}

export function addTimeout(data: Partial<TimeoutItem>) {
  return request({ url: '/workflow/timeout', method: 'post', data })
}

export function updateTimeout(data: Partial<TimeoutItem>) {
  return request({ url: '/workflow/timeout', method: 'put', data })
}

export function delTimeout(id: number) {
  return request({ url: `/workflow/timeout/${id}`, method: 'delete' })
}
