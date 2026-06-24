import request from '../request'

// 查询借支记录列表
export function listAdvance(query: any) {
  return request({
    url: '/finance/advance/list',
    method: 'get',
    params: query
  })
}

// 查询借支记录详细
export function getAdvance(advanceId: any) {
  return request({
    url: '/finance/advance/' + advanceId,
    method: 'get'
  })
}

// 新增借支记录
export function addAdvance(data: any) {
  return request({
    url: '/finance/advance',
    method: 'post',
    data: data
  })
}

// 修改借支记录
export function updateAdvance(data: any) {
  return request({
    url: '/finance/advance',
    method: 'put',
    data: data
  })
}

// 删除借支记录
export function delAdvance(advanceId: any) {
  return request({
    url: '/finance/advance/' + advanceId,
    method: 'delete'
  })
}
