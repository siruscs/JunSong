import request from '../request'
import { generateIdempotencyKey } from '@/utils/idempotency'

export interface ExpenseVerifyRequest {
  expenseIds: number[]
  advanceIds: number[]
  requestId: string
}

export interface ExpenseUnverifyRequest {
  reason: string
  requestId: string
}

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
    idempotencyKey: generateIdempotencyKey('expense:create'),
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

// 获取统计数据
export function getExpenseSummary(deptId: any) {
  return request({
    url: '/finance/expense/summary',
    method: 'get',
    params: { deptId }
  })
}

// 批量核销费用
export function batchVerifyExpense(data: ExpenseVerifyRequest) {
  return request({
    url: '/finance/expense/batchVerify',
    method: 'put',
    data: data
  })
}

export function getExpenseCapability(expenseId: number) {
  return request({
    url: `/finance/expense/${expenseId}/capability`,
    method: 'get'
  })
}

export function unverifyExpense(batchId: number, data: ExpenseUnverifyRequest) {
  return request({
    url: `/finance/expense/unverify/${batchId}`,
    method: 'put',
    data
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

// 查询核销记录列表
export function listVerificationBatches(query: any) {
  return request({
    url: '/finance/verification-batch/list',
    method: 'get',
    params: query
  })
}

// 查询核销批次详情
export function getVerificationBatchDetail(batchId: number) {
  return request({
    url: `/finance/verification-batch/${batchId}`,
    method: 'get'
  })
}
