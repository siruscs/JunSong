import request from '../request'

// 查询投资款记录列表
export function listInvestRecord(query: any) {
  return request({
    url: '/finance/investRecord/list',
    method: 'get',
    params: query
  })
}

// 查询投资款记录详细
export function getInvestRecord(investId: any) {
  return request({
    url: '/finance/investRecord/' + investId,
    method: 'get'
  })
}

// 新增投资款记录
export function addInvestRecord(data: any) {
  return request({
    url: '/finance/investRecord',
    method: 'post',
    data: data
  })
}

// 修改投资款记录
export function updateInvestRecord(data: any) {
  return request({
    url: '/finance/investRecord',
    method: 'put',
    data: data
  })
}

// 删除投资款记录
export function delInvestRecord(investIds: any) {
  return request({
    url: '/finance/investRecord/' + investIds,
    method: 'delete'
  })
}
