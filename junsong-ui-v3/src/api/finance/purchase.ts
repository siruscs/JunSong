import request from '../request'

// 查询进货单列表
export function listPurchase(query: any) {
  return request({
    url: '/finance/purchase/list',
    method: 'get',
    params: query
  })
}

// 查询进货单详细
export function getPurchase(purchaseId: any) {
  return request({
    url: '/finance/purchase/' + purchaseId,
    method: 'get'
  })
}

// 新增进货单
export function addPurchase(data: any) {
  return request({
    url: '/finance/purchase',
    method: 'post',
    data: data
  })
}

// 修改进货单
export function updatePurchase(data: any) {
  return request({
    url: '/finance/purchase',
    method: 'put',
    data: data
  })
}

// 删除进货单
export function delPurchase(purchaseId: any) {
  return request({
    url: '/finance/purchase/' + purchaseId,
    method: 'delete'
  })
}
