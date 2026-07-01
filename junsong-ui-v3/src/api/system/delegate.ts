import request from '../request'

export interface DelegateItem {
  id?: number
  userId?: number
  delegateUserId: number
  delegateType: string
  processKeys: string
  startTime: string
  endTime: string
  status: string
  remark?: string
}

export function listDelegate(params?: any) {
  return request({
    url: '/system/delegate/list',
    method: 'get',
    params,
  })
}

export function getMyDelegates() {
  return request({
    url: '/system/delegate/my',
    method: 'get',
  })
}

export function getMyAgentTasks() {
  return request({
    url: '/system/delegate/agent',
    method: 'get',
  })
}

export function addDelegate(data: Partial<DelegateItem>) {
  return request({
    url: '/system/delegate',
    method: 'post',
    data,
  })
}

export function updateDelegate(data: Partial<DelegateItem>) {
  return request({
    url: '/system/delegate',
    method: 'put',
    data,
  })
}

export function delDelegate(ids: number[]) {
  return request({
    url: `/system/delegate/${ids.join(',')}`,
    method: 'delete',
  })
}
