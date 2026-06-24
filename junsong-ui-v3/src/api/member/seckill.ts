import request from '../request'

export function listSeckill(query: any) {
  return request({ url: '/member/seckill/list', method: 'get', params: query })
}

export function getSeckill(seckillId: number) {
  return request({ url: '/member/seckill/' + seckillId, method: 'get' })
}

export function addSeckill(data: any) {
  return request({ url: '/member/seckill', method: 'post', data })
}

export function updateSeckill(data: any) {
  return request({ url: '/member/seckill', method: 'put', data })
}

export function delSeckill(seckillIds: string | number) {
  return request({ url: '/member/seckill/' + seckillIds, method: 'delete' })
}

export function exportSeckill() {
  return request({ url: '/member/seckill/export', method: 'get', responseType: 'blob' })
}

export function closeSeckill(seckillId: number) {
  return request({ url: '/member/seckill/' + seckillId + '/close', method: 'put' })
}

export function listActiveSeckill() {
  return request({ url: '/member/seckill/listActive', method: 'get' })
}