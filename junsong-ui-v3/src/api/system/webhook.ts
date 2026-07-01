import request from '../request'

export function listSubscription(query: any) {
  return request({ url: '/system/webhook/subscription/list', method: 'get', params: query })
}

export function getSubscription(id: number) {
  return request({ url: '/system/webhook/subscription/' + id, method: 'get' })
}

export function addSubscription(data: any) {
  return request({ url: '/system/webhook/subscription', method: 'post', data })
}

export function updateSubscription(data: any) {
  return request({ url: '/system/webhook/subscription', method: 'put', data })
}

export function delSubscription(ids: string | number | (string | number)[]) {
  return request({ url: '/system/webhook/subscription/' + ids, method: 'delete' })
}

export function changeStatus(id: number, status: string) {
  return request({ url: '/system/webhook/subscription/changeStatus', method: 'put', data: { id, status } })
}

export function generateToken() {
  return request({ url: '/system/webhook/subscription/generateToken', method: 'post' })
}

export function listDelivery(query: any) {
  return request({ url: '/system/webhook/subscription/delivery/list', method: 'get', params: query })
}

export function testEvent(eventType: string) {
  return request({ url: '/system/webhook/subscription/test', method: 'post', data: { eventType } })
}
