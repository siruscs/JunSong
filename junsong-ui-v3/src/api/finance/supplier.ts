import request from '../request'

// 查询供应商列表
export function listSupplier(query: any) {
  return request({
    url: '/finance/supplier/list',
    method: 'get',
    params: query
  })
}

// 查询供应商详细
export function getSupplier(supplierId: any) {
  return request({
    url: '/finance/supplier/' + supplierId,
    method: 'get'
  })
}

// 新增供应商
export function addSupplier(data: any) {
  return request({
    url: '/finance/supplier',
    method: 'post',
    data: data
  })
}

// 修改供应商
export function updateSupplier(data: any) {
  return request({
    url: '/finance/supplier',
    method: 'put',
    data: data
  })
}

// 删除供应商
export function delSupplier(supplierId: any) {
  return request({
    url: '/finance/supplier/' + supplierId,
    method: 'delete'
  })
}
