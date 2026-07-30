import request from '../request'

/**
 * 期初库存 API（Task：PC 期初库存工作台）。
 *
 * 端点：/finance/stockInit
 * 权限分离：finance:stockInit:list/query/add/approve/post/export
 *
 * 状态机：DRAFT → VALIDATED → SUBMITTED → APPROVED → POSTED
 * batchNo 由服务端生成（SI + 时间戳），不接受客户端传入。
 */

export interface StockInitItemInput {
  productId: number
  quantity: number
  unitCost: number
}

export interface StockInitCreateRequest {
  deptId: number
  initDate: string
  items: StockInitItemInput[]
  remark?: string
}

export interface StockInitPostRequest {
  postIdempotencyKey: string
  version: number
}

export interface StockInitApproveRequest {
  decision: 'APPROVE' | 'REJECT'
  comment?: string
  version: number
}

export interface StockInitQuery {
  deptId?: number
  status?: string
  batchNo?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

export interface StockInitItemVO {
  itemId: number
  batchId: number
  tenantId: number
  deptId: number
  productId: number
  productName: string
  quantity: number
  unitCost: number
  amount: number
  stockLedgerId: number | null
  costLedgerId: number | null
  version: number
}

export interface StockInitBatchVO {
  batchId: number
  tenantId: number
  batchNo: string
  deptId: number
  initDate: string
  status: string
  submittedBy: string | null
  submittedTime: string | null
  approvedBy: string | null
  approvedTime: string | null
  postedBy: string | null
  postedTime: string | null
  postIdempotencyKey: string | null
  remark: string | null
  version: number
  createBy: string | null
  createTime: string | null
}

export interface StockInitDetailVO {
  batch: StockInitBatchVO
  items: StockInitItemVO[]
}

// 分页查询期初库存批次
export function listStockInit(params: StockInitQuery) {
  return request({ url: '/finance/stockInit', method: 'get', params })
}

// 期初库存批次详情
export function getStockInitDetail(batchId: number) {
  return request({ url: `/finance/stockInit/${batchId}`, method: 'get' })
}

// 创建期初库存批次
export function createStockInit(data: StockInitCreateRequest) {
  return request({ url: '/finance/stockInit', method: 'post', data })
}

// 校验期初库存批次（DRAFT -> VALIDATED）
export function validateStockInit(batchId: number, version: number) {
  return request({ url: `/finance/stockInit/${batchId}/validate`, method: 'put', params: { version } })
}

// 提交期初库存批次（VALIDATED -> SUBMITTED）
export function submitStockInit(batchId: number, version: number) {
  return request({ url: `/finance/stockInit/${batchId}/submit`, method: 'put', params: { version } })
}

// 审批期初库存批次（SUBMITTED -> APPROVED / DRAFT）
export function approveStockInit(batchId: number, data: StockInitApproveRequest) {
  return request({ url: `/finance/stockInit/${batchId}/approve`, method: 'put', data })
}

// 过账期初库存批次（APPROVED -> POSTED，数量与移动平均成本原子过账）
export function postStockInit(batchId: number, data: StockInitPostRequest) {
  return request({ url: `/finance/stockInit/${batchId}/post`, method: 'put', data })
}

// 导出期初库存批次
export function exportStockInit(query: StockInitQuery) {
  return request({
    url: '/finance/stockInit/export',
    method: 'post',
    params: query,
    responseType: 'blob',
  }) as Promise<any>
}
