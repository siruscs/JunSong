import request from '../request'
import { listSeckill as fetchSeckill, listActiveSeckill as fetchActiveSeckill } from './seckill'

export function listSeckillRecord(query: any) {
  return request({ url: '/member/seckillRecord/list', method: 'get', params: query })
}

export function getSeckillRecord(recordId: number) {
  return request({ url: '/member/seckillRecord/' + recordId, method: 'get' })
}

export function addSeckillRecord(data: any) {
  return request({ url: '/member/seckillRecord', method: 'post', data })
}

export function updateSeckillRecord(data: any) {
  return request({ url: '/member/seckillRecord', method: 'put', data })
}

export function delSeckillRecord(recordIds: string | number) {
  return request({ url: '/member/seckillRecord/' + recordIds, method: 'delete' })
}

export function claimSeckillRecord(recordId: number, data?: any) {
  return request({ url: '/member/seckillRecord/claim/' + recordId, method: 'put', data: data || {} })
}

export function receiveRecord(recordIds: number, data?: any) {
  return claimSeckillRecord(recordIds, data)
}

export function listSeckillClaimRecord(query: any) {
  return request({ url: '/member/seckillRecord/claim/list', method: 'get', params: query })
}

export function listSeckill(query: any) {
  return fetchSeckill(query)
}

export function listActiveSeckill() {
  return fetchActiveSeckill()
}

export function getSeckillRecordStatistics(query: any) {
  return request({ url: '/member/seckillRecord/statistics', method: 'get', params: query })
}

export function getSeckillRecordDetail(recordId: number) {
  return request({ url: '/member/seckillRecord/' + recordId + '/detail', method: 'get' })
}

export function batchSeckillForAll(data: any) {
  return request({ url: '/member/seckillRecord/batch', method: 'post', data })
}
