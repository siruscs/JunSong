import request from '../request'

/**
 * 库存盘点 API（Task 9：PC 盘点工作台）。
 *
 * 端点：/stocktakes
 * 权限分离：finance:stocktake:list/query/add/assign/count/submit/recount/approve/post/reverse/export
 *
 * 盲盘保护：counter 视角且任务未提交时，expectedQuantity/varianceQuantity/varianceAmount/unitCost 置 null。
 */

export interface StocktakeQuery {
  deptId?: number
  status?: string
  counterUserId?: number
  takeNo?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

export interface StocktakeItemAttachment {
  name: string
  url: string
}

export interface StocktakeItemVO {
  itemId: number
  stocktakeId: number
  tenantId: number
  deptId: number
  productId: number
  productName: string
  expectedQuantity: number | null
  movementQuantityAfterFreeze: number
  adjustedExpectedQuantity: number | null
  actualQuantity: number | null
  recountQuantity: number | null
  finalQuantity: number | null
  varianceQuantity: number | null
  unitCost: number | null
  varianceAmount: number | null
  reasonCode: string | null
  reason: string | null
  attachments: StocktakeItemAttachment[] | null
  countedBy: string | null
  countedTime: string | null
  recountedBy: string | null
  recountedTime: string | null
  stockLedgerId: number | null
  costLedgerId: number | null
  reverseStockLedgerId: number | null
  reverseCostLedgerId: number | null
  countIdempotencyKey: string | null
  version: number
}

export interface StocktakeHistoryVO {
  historyId: number
  stocktakeId: number
  action: string
  fromStatus: string | null
  toStatus: string
  operator: string
  comment: string | null
  createTime: string
}

export interface StocktakeDetailVO {
  stocktakeId: number
  tenantId: number
  takeNo: string
  deptId: number
  scopeType: string
  status: string
  freezeTime: string
  counterUserId: number
  counterUserName: string | null
  recountUserId: number | null
  recountUserName: string | null
  submittedBy: string | null
  submittedTime: string | null
  approvedBy: string | null
  approvedTime: string | null
  postedBy: string | null
  postedTime: string | null
  reversedBy: string | null
  reversedTime: string | null
  reversalReason: string | null
  version: number
  remark: string | null
  hideExpected: boolean
  items: StocktakeItemVO[]
  histories: StocktakeHistoryVO[]
}

export interface StocktakeCreateRequest {
  takeNo: string
  deptId: number
  scopeType: string
  productIds: number[]
  counterUserId: number
  recountUserId?: number
  remark?: string
}

export interface StocktakeAssignRequest {
  counterUserId: number
  recountUserId?: number
  version: number
}

export interface StocktakeCountRequest {
  actualQuantity: number
  reasonCode?: string
  reason?: string
  attachments?: StocktakeItemAttachment[]
  idempotencyKey: string
  version: number
}

export interface StocktakeRecountRequest {
  recountQuantity: number
  reason?: string
  idempotencyKey: string
  version: number
}

export interface StocktakeApprovalRequest {
  decision: string
  comment?: string
  version: number
}

export interface StocktakeReverseRequest {
  reason: string
  idempotencyKey: string
  version: number
}

// 创建盘点任务
export function createStocktake(data: StocktakeCreateRequest) {
  return request({ url: '/finance/stocktakes', method: 'post', data })
}

// 分页查询盘点任务
export function listStocktakes(params: StocktakeQuery) {
  return request({ url: '/finance/stocktakes', method: 'get', params })
}

// 盘点任务详情（counter 视角盲盘保护）
export function getStocktakeDetail(stocktakeId: number) {
  return request({ url: `/finance/stocktakes/${stocktakeId}`, method: 'get' })
}

// 分配盘点人和复盘人
export function assignCounter(stocktakeId: number, data: StocktakeAssignRequest) {
  return request({ url: `/finance/stocktakes/${stocktakeId}/assign`, method: 'put', data })
}

// 启动盘点（DRAFT -> COUNTING）
export function startStocktake(stocktakeId: number, version: number) {
  return request({ url: `/finance/stocktakes/${stocktakeId}/start`, method: 'put', params: { version } })
}

// 行录入（盲盘，counter 视角不显示期望值）
export function countItem(stocktakeId: number, itemId: number, data: StocktakeCountRequest) {
  return request({ url: `/finance/stocktakes/${stocktakeId}/items/${itemId}/count`, method: 'put', data })
}

// 提交盘点（COUNTING -> SUBMITTED，生成临时方差，触发复盘阈值）
export function submitStocktake(stocktakeId: number, version: number) {
  return request({ url: `/finance/stocktakes/${stocktakeId}/submit`, method: 'put', params: { version } })
}

// 复盘行录入（RECOUNTING，复盘人须与盘点人不同）
export function recountItem(stocktakeId: number, itemId: number, data: StocktakeRecountRequest) {
  return request({ url: `/finance/stocktakes/${stocktakeId}/items/${itemId}/recount`, method: 'put', data })
}

// 审批盘点（SUBMITTED/RECOUNTING -> APPROVED）
export function approveStocktake(stocktakeId: number, data: StocktakeApprovalRequest) {
  return request({ url: `/finance/stocktakes/${stocktakeId}/approve`, method: 'put', data })
}

// 过账盘点（APPROVED -> POSTED，数量与移动平均成本原子过账）
export function postStocktake(stocktakeId: number, version: number) {
  return request({ url: `/finance/stocktakes/${stocktakeId}/post`, method: 'put', params: { version } })
}

// 取消盘点任务（过账前 -> CANCELLED）
export function cancelStocktake(stocktakeId: number, version: number) {
  return request({ url: `/finance/stocktakes/${stocktakeId}/cancel`, method: 'put', params: { version } })
}

// 整单冲销（POSTED -> REVERSED）
export function reverseStocktake(stocktakeId: number, data: StocktakeReverseRequest) {
  return request({ url: `/finance/stocktakes/${stocktakeId}/reverse`, method: 'put', data })
}

// 导出盘点任务
export function exportStocktakes(data: StocktakeQuery) {
  return request({
    url: '/finance/stocktakes/export',
    method: 'post',
    data,
    responseType: 'blob',
  }) as Promise<any>
}
