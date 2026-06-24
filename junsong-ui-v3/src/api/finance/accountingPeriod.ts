import request from '../request'

// 查询财务核算周期列表
export function listAccountingPeriod(query: any) {
  return request({
    url: '/finance/accountingPeriod/list',
    method: 'get',
    params: query
  })
}

// 查询财务核算周期详细
export function getAccountingPeriod(periodId: any) {
  return request({
    url: '/finance/accountingPeriod/' + periodId,
    method: 'get'
  })
}

// 查询财务核算周期聚合明细
export function getAccountingPeriodDetail(periodId: any) {
  return request({
    url: '/finance/accountingPeriod/detail/' + periodId,
    method: 'get'
  })
}

// 查询店面当前核算周期
export function getCurrentAccountingPeriod(deptId: any) {
  return request({
    url: '/finance/accountingPeriod/current/' + deptId,
    method: 'get'
  })
}

// 初始化店面当前核算周期
export function initCurrentAccountingPeriod(deptId: any) {
  return request({
    url: '/finance/accountingPeriod/current/' + deptId + '/init',
    method: 'post'
  })
}

// 试算回本
export function trialBreakEven(deptId: any) {
  return request({
    url: '/finance/accountingPeriod/current/' + deptId + '/trialBreakEven',
    method: 'post'
  })
}

// 结转
export function carryForward(deptId: any) {
  return request({
    url: '/finance/accountingPeriod/current/' + deptId + '/carryForward',
    method: 'post'
  })
}

// 结转回退
export function rollbackCarryForward(deptId: any) {
  return request({
    url: '/finance/accountingPeriod/current/' + deptId + '/rollbackCarryForward',
    method: 'post'
  })
}

// 修改财务核算周期
export function updateAccountingPeriod(data: any) {
  return request({
    url: '/finance/accountingPeriod',
    method: 'put',
    data: data
  })
}
