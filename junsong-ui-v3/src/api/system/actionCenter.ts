import request from '@/utils/request'

export interface ActionCenterItem {
  actionId: string
  sourceType: string
  sourceId: string
  title: string
  description: string
  priority: string
  status: string
  ownerName: string
  ownerId: number
  deptId: number
  deptName: string
  dueDate: string
  effectStatus: string
  drilldownPath: string
  latestTouchStatus: string
  latestTouchTime: string
  touchCount24h: number
  touchable: boolean
  touchDisabledReason: string
}

export interface ActionCenterCalendar {
  date: string
  pendingCount: number
  overdueCount: number
  doneCount: number
  effectPendingCount: number
}

export interface ActionTouchResult {
  logId: number
  actionId: string
  channel: string
  touchStatus: string
  message: string
  providerResponse: string
}

export function listActionCenter(params?: Record<string, any>) {
  return request({
    url: '/system/action-center/list',
    method: 'get',
    params,
  })
}

export function getActionCenterCalendar(params?: Record<string, any>) {
  return request({
    url: '/system/action-center/calendar',
    method: 'get',
    params,
  })
}

export function touchAction(actionId: string, data: Record<string, any>) {
  return request({
    url: `/system/action-center/${encodeURIComponent(actionId)}/touch`,
    method: 'post',
    data,
  })
}
