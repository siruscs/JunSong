import request from '../request'

// 查询投资人列表
export function listInvestor(query: any) {
  return request({
    url: '/finance/investor/list',
    method: 'get',
    params: query
  })
}

// 查询投资人详细
export function getInvestor(investorId: any) {
  return request({
    url: '/finance/investor/' + investorId,
    method: 'get'
  })
}

// 新增投资人
export function addInvestor(data: any) {
  return request({
    url: '/finance/investor',
    method: 'post',
    data: data
  })
}

// 修改投资人
export function updateInvestor(data: any) {
  return request({
    url: '/finance/investor',
    method: 'put',
    data: data
  })
}

// 删除投资人
export function delInvestor(investorIds: any) {
  return request({
    url: '/finance/investor/' + investorIds,
    method: 'delete'
  })
}
