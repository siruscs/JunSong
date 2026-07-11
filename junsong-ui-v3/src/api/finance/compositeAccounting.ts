import request from '../request'

// 查询复合核算池列表
export function listCompositeAccounting(query: any) {
  return request({
    url: '/finance/compositeAccounting/list',
    method: 'get',
    params: query
  })
}

// 查询复合核算池详情
export function getCompositeAccounting(poolId: any) {
  return request({
    url: '/finance/compositeAccounting/' + poolId,
    method: 'get'
  })
}

// 查询复合核算池概览(含参与店面、共享投资人、周期明细、回本进度)
export function getCompositeOverview(poolId: any) {
  return request({
    url: '/finance/compositeAccounting/' + poolId + '/overview',
    method: 'get'
  })
}

// 查询已纳入周期明细
export function listCompositePeriods(poolId: any) {
  return request({
    url: '/finance/compositeAccounting/' + poolId + '/periods',
    method: 'get'
  })
}

// 查询可手动纳入的候选周期(回本后使用)
export function listCandidatePeriods(poolId: any, deptId: any) {
  return request({
    url: '/finance/compositeAccounting/' + poolId + '/candidatePeriods',
    method: 'get',
    params: { deptId }
  })
}

// 新增复合核算池
export function addCompositeAccounting(data: any) {
  return request({
    url: '/finance/compositeAccounting',
    method: 'post',
    data: data
  })
}

// 修改复合核算池基础信息
export function updateCompositeAccounting(data: any) {
  return request({
    url: '/finance/compositeAccounting',
    method: 'put',
    data: data
  })
}

// 删除复合核算池
export function delCompositeAccounting(poolIds: any) {
  return request({
    url: '/finance/compositeAccounting/' + poolIds,
    method: 'delete'
  })
}

// 维护参与店面(全量覆盖)
export function bindDepts(poolId: any, deptIds: number[]) {
  return request({
    url: '/finance/compositeAccounting/' + poolId + '/bindDepts',
    method: 'post',
    data: deptIds
  })
}

// 维护共享投资人和出资款(全量覆盖)
export function bindInvestors(poolId: any, investors: any[]) {
  return request({
    url: '/finance/compositeAccounting/' + poolId + '/bindInvestors',
    method: 'post',
    data: investors
  })
}

// 试算手动纳入结果(不落库)
export function trialIncludePeriods(poolId: any, periodIds: number[]) {
  return request({
    url: '/finance/compositeAccounting/' + poolId + '/trialInclude',
    method: 'post',
    data: periodIds
  })
}

// 确认纳入周期(落库并刷新回本金额)
export function confirmIncludePeriods(poolId: any, periodIds: number[]) {
  return request({
    url: '/finance/compositeAccounting/' + poolId + '/confirmInclude',
    method: 'post',
    data: periodIds
  })
}

// 重新计算累计回本、缺口、超额收益
export function recalculatePool(poolId: any) {
  return request({
    url: '/finance/compositeAccounting/' + poolId + '/recalculate',
    method: 'post'
  })
}

// 财务确认整体回本
export function confirmBreakEven(poolId: any) {
  return request({
    url: '/finance/compositeAccounting/' + poolId + '/confirmBreakEven',
    method: 'post'
  })
}

// 关闭复合核算池
export function closeCompositePool(poolId: any) {
  return request({
    url: '/finance/compositeAccounting/' + poolId + '/close',
    method: 'post'
  })
}
