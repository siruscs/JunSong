import request from '../request'

export function listPointsGoods(query: any) {
  return request({ url: '/member/pointsGoods/list', method: 'get', params: query })
}

export function getPointsGoods(goodsId: number) {
  return request({ url: '/member/pointsGoods/' + goodsId, method: 'get' })
}

export function addPointsGoods(data: any) {
  return request({ url: '/member/pointsGoods', method: 'post', data })
}

export function updatePointsGoods(data: any) {
  return request({ url: '/member/pointsGoods', method: 'put', data })
}

export function delPointsGoods(goodsIds: string | number) {
  return request({ url: '/member/pointsGoods/' + goodsIds, method: 'delete' })
}

export function exportPointsGoods() {
  return request({ url: '/member/pointsGoods/export', method: 'get', responseType: 'blob' })
}

export function updateStock(goodsId: number, quantity: number) {
  return request({ url: '/member/pointsGoods/' + goodsId + '/stock', method: 'put', params: { quantity } })
}