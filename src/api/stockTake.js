/**
 * 库存盘点 API。
 *
 * 安全边界：
 * 1. 盘点数据由后端重新验证：租户、部门、商品归属、库存版本、权限
 * 2. 盘盈盘亏原因必填，金额和数量校验由后端决定
 * 3. 幂等基于 takeNo（后端检查 reference_no 唯一）
 * 4. 重复提交不重复写入（后端拒绝相同 takeNo）
 *
 * 后端接口：POST /stockTake（StockTakeController）
 *
 * @module api/stockTake
 */
import { request } from './index.js'

/**
 * 生成盘点单号（客户端唯一，用于幂等）。
 * 格式：TK-{timestamp}-{random}
 * @returns {string}
 */
export function generateTakeNo() {
  const ts = Date.now()
  const rand = Math.floor(Math.random() * 10000).toString().padStart(4, '0')
  return 'TK-' + ts + '-' + rand
}

/**
 * 提交库存盘点。
 *
 * @param {Object} params 盘点参数
 * @param {string} params.takeNo 盘点单号（幂等键）
 * @param {number} params.deptId 门店ID
 * @param {number} params.productId 商品ID
 * @param {number} params.actualQuantity 盘点后实际数量
 * @param {number} [params.expectedQuantity] 盘点前系统库存
 * @param {number} [params.unitCost] 单位成本（盘盈时按此成本入账）
 * @param {string} params.reason 盘盈盘亏原因（必填）
 * @returns {Promise<number>} 盘点流水ID
 */
export function submitStockTake(params = {}) {
  return request({
    url: '/stockTake',
    method: 'POST',
    data: {
      takeNo: params.takeNo || generateTakeNo(),
      deptId: params.deptId,
      productId: params.productId,
      actualQuantity: params.actualQuantity,
      expectedQuantity: params.expectedQuantity,
      unitCost: params.unitCost,
      reason: params.reason
    }
  })
}

/**
 * 查询库存流水（用于盘点后核对）。
 *
 * @param {Object} params 查询参数
 * @param {number} params.deptId 门店ID
 * @param {number} params.productId 商品ID
 * @param {string} [params.startDate] 开始日期
 * @param {string} [params.endDate] 结束日期
 * @param {number} [params.pageNum] 页码
 * @param {number} [params.pageSize] 每页数量
 * @returns {Promise<Object>} 流水分页结果
 */
export function queryStockLedger(params = {}) {
  return request({
    url: '/report/stock/ledger/page',
    method: 'POST',
    data: params
  })
}
