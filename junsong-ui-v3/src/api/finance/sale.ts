import request from '../request'

// 查询销售记录列表
export function listSale(query: any) {
  return request({
    url: '/finance/sale/list',
    method: 'get',
    params: query
  })
}

// 查询销售记录详细
export function getSale(saleId: any) {
  return request({
    url: '/finance/sale/' + saleId,
    method: 'get'
  })
}

// 新增销售记录
export function addSale(data: any) {
  return request({
    url: '/finance/sale',
    method: 'post',
    data: data
  })
}

// 修改销售记录
export function updateSale(data: any) {
  return request({
    url: '/finance/sale',
    method: 'put',
    data: data
  })
}

// 删除销售记录
export function delSale(saleId: any) {
  return request({
    url: '/finance/sale/' + saleId,
    method: 'delete'
  })
}

// 添加缴款
export function addPayment(saleId: any, data: any) {
  return request({
    url: '/finance/sale/payment/' + saleId,
    method: 'post',
    data: data
  })
}

// 修改缴款
export function updatePayment(paymentId: any, data: any) {
  return request({
    url: '/finance/sale/payment/' + paymentId,
    method: 'put',
    data: data
  })
}

// 删除缴款
export function delPayment(paymentId: any) {
  return request({
    url: '/finance/sale/payment/' + paymentId,
    method: 'delete'
  })
}
