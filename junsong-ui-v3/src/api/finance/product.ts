import request from '../request'

// 查询商品列表
export function listProduct(query: any) {
  return request({
    url: '/finance/product/list',
    method: 'get',
    params: query
  })
}

// 查询商品详细
export function getProduct(productId: any) {
  return request({
    url: '/finance/product/' + productId,
    method: 'get'
  })
}

// 新增商品
export function addProduct(data: any) {
  return request({
    url: '/finance/product',
    method: 'post',
    data: data
  })
}

// 修改商品
export function updateProduct(data: any) {
  return request({
    url: '/finance/product',
    method: 'put',
    data: data
  })
}

// 删除商品
export function delProduct(productId: any) {
  return request({
    url: '/finance/product/' + productId,
    method: 'delete'
  })
}
