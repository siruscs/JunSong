import request from '../request'
import { listPointsGoods as fetchPointsGoods } from './pointsGoods'

export function listPointsExchange(query: any) {
  return request({ url: '/member/pointsExchange/list', method: 'get', params: query })
}

export function getPointsExchange(exchangeId: number) {
  return request({ url: '/member/pointsExchange/' + exchangeId, method: 'get' })
}

export function addPointsExchange(data: any) {
  return request({ url: '/member/pointsExchange', method: 'post', data })
}

export function updatePointsExchange(data: any) {
  return request({ url: '/member/pointsExchange', method: 'put', data })
}

export function delPointsExchange(exchangeIds: string | number) {
  return request({ url: '/member/pointsExchange/' + exchangeIds, method: 'delete' })
}

export function claimPointsExchange(exchangeIds: any) {
  return request({ url: '/member/pointsExchange/claim', method: 'put', data: exchangeIds })
}

export function receiveExchange(exchangeIds: any) {
  return claimPointsExchange(exchangeIds)
}

export function listPointsGoods(query: any) {
  return fetchPointsGoods(query)
}

export function exportPointsExchange() {
  return request({ url: '/member/pointsExchange/export', method: 'get', responseType: 'blob' })
}