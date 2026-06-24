import request from '../request'

// 查询店面分润配置列表
export function listDeptProfitConfig(query: any) {
  return request({
    url: '/finance/deptProfitConfig/list',
    method: 'get',
    params: query
  })
}

// 查询店面分润配置详细
export function getDeptProfitConfig(configId: any) {
  return request({
    url: '/finance/deptProfitConfig/' + configId,
    method: 'get'
  })
}

// 查询机构分润配置
export function getDeptProfitConfigByDept(deptId: any) {
  return request({
    url: '/finance/deptProfitConfig/dept/' + deptId,
    method: 'get'
  })
}

// 新增店面分润配置
export function addDeptProfitConfig(data: any) {
  return request({
    url: '/finance/deptProfitConfig',
    method: 'post',
    data: data
  })
}

// 修改店面分润配置
export function updateDeptProfitConfig(data: any) {
  return request({
    url: '/finance/deptProfitConfig',
    method: 'put',
    data: data
  })
}

// 删除店面分润配置
export function delDeptProfitConfig(configIds: any) {
  return request({
    url: '/finance/deptProfitConfig/' + configIds,
    method: 'delete'
  })
}
