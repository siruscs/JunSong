import request from '../request'

// 查询费用记录列表
export function listExpense(query: any) {
  return request({
    url: '/finance/expense/list',
    method: 'get',
    params: query
  })
}

// 查询费用记录详细
export function getExpense(expenseId: any) {
  return request({
    url: '/finance/expense/' + expenseId,
    method: 'get'
  })
}

// 新增费用记录
export function addExpense(data: any) {
  return request({
    url: '/finance/expense',
    method: 'post',
    data: data
  })
}

// 修改费用记录
export function updateExpense(data: any) {
  return request({
    url: '/finance/expense',
    method: 'put',
    data: data
  })
}

// 删除费用记录
export function delExpense(expenseId: any) {
  return request({
    url: '/finance/expense/' + expenseId,
    method: 'delete'
  })
}

// 核销费用
export function verifyExpense(expenseId: any) {
  return request({
    url: '/finance/expense/verify/' + expenseId,
    method: 'put'
  })
}

// 获取统计数据
export function getExpenseSummary(deptId: any) {
  return request({
    url: '/finance/expense/summary',
    method: 'get',
    params: { deptId }
  })
}

// 批量核销费用
export function batchVerifyExpense(data: any) {
  return request({
    url: '/finance/expense/batchVerify',
    method: 'put',
    data: data
  })
}

// 查询未核销借支记录
export function listUnverifiedAdvances(deptId: any) {
  return request({
    url: '/finance/expense/unverifiedAdvances',
    method: 'get',
    params: { deptId }
  })
}
