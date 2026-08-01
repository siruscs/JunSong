import request from '../request'

export interface StockReportQuery {
  deptIds?: number[]
  startDate?: string // yyyy-MM-dd
  endDate?: string // yyyy-MM-dd
  keyword?: string
  status?: string
  pageNum?: number
  pageSize?: number
}

export interface StockReportSummary {
  openingQuantity: number
  purchaseNetInQuantity: number
  saleNetOutQuantity: number
  otherAdjustmentNetQuantity: number
  closingQuantity: number
  negativeStockCount: number
  lowStockCount: number
  zeroStockCount: number
  staleStockCount: number
  anomalyCount: number
}

export interface StockReportItem {
  tenantId: number
  deptId: number
  deptName: string
  productId: number
  productCode: string
  productName: string
  unit: string
  minStock: number
  openingQuantity: number
  purchaseNetInQuantity: number
  saleNetOutQuantity: number
  closingQuantity: number
  lastInboundTime: string
  lastOutboundTime: string
  daysWithoutSale: number
  stockStatus: string
  reconciliationStatus: string
}

export interface StockReportVO {
  summary: StockReportSummary
  items: StockReportItem[]
  total: number
  pageNum: number
  pageSize: number
}

export interface StockLedgerRow {
  ledgerId: number
  tenantId: number
  deptId: number
  productId: number
  productName: string
  changeType: string
  changeQuantity: number
  beforeQuantity: number
  afterQuantity: number
  referenceType: string
  referenceId: number
  referenceNo: string
  createBy: string
  createTime: string
  remark: string
}

// 库存报表（汇总 + 分页组合）
export function getStockReport(query: StockReportQuery) {
  return request({ url: '/finance/report/stock', method: 'post', data: query })
}

// 库存报表汇总
export function getStockReportSummary(query: StockReportQuery) {
  return request({ url: '/finance/report/stock/summary', method: 'post', data: query })
}

// 库存报表分页
export function getStockReportPage(query: StockReportQuery) {
  return request({ url: '/finance/report/stock/page', method: 'post', data: query })
}

// 库存流水下钻
export function getStockLedgerPage(params: {
  deptId: number
  productId: number
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}) {
  return request({ url: '/finance/report/stock/ledger/page', method: 'post', data: params })
}

// 导出库存报表
export function exportStockReport(query: StockReportQuery) {
  return request({
    url: '/finance/report/stock/export',
    method: 'post',
    data: query,
    responseType: 'blob',
  }) as Promise<any>
}

// 库存对账
export function getStockReconciliation(query: StockReportQuery) {
  return request({ url: '/finance/report/stock/reconciliation', method: 'post', data: query })
}

// ==================== 库存价值报表（第二期财务计价） ====================

export interface StockValueReportVO {
  costReady: boolean
  periodStatus: string // ACTIVE / LOCKED / CARRIED_FORWARD
  openingAmount: number
  inboundAmount: number
  saleCost: number
  adjustmentAmount: number
  closingAmount: number
  saleRevenue: number
  grossProfit: number
  grossProfitRate: number
  items: StockValueReportItemVO[]
}

export interface StockValueReportItemVO {
  tenantId: number
  deptId: number
  deptName: string
  productId: number
  productCode: string
  productName: string
  closingQuantity: number
  avgUnitCost: number
  closingAmount: number
  inboundAmount: number
  saleCost: number
  adjustmentAmount: number
  saleRevenue: number
  grossProfit: number
}

// 库存价值报表
export function getStockValueReport(query: StockReportQuery) {
  return request({ url: '/finance/report/stock/value', method: 'post', data: query })
}

// 成本调整
export function createCostAdjustment(data: {
  deptIds: number[]
  startDate?: string
  endDate?: string
  productId: number
  amount: number
  reason: string
}) {
  return request({ url: '/finance/report/stock/cost-adjustment', method: 'post', data })
}
