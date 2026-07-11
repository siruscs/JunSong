import request from '../request'

// 查询等级配置列表
export function listLevel(query: any) {
  return request({ url: '/member/level/list', method: 'get', params: query })
}

// 查询等级配置详情
export function getLevel(typeCode: string) {
  return request({ url: '/member/level/' + typeCode, method: 'get' })
}

// 新增等级配置
export function addLevel(data: any) {
  return request({ url: '/member/level', method: 'post', data })
}

// 修改等级配置
export function updateLevel(data: any) {
  return request({ url: '/member/level', method: 'put', data })
}
