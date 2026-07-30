/**
 * 库存盘点 API（Task 10：同时支持 legacy 和新工作流）。
 *
 * 历史背景：
 * - 原 stockTake.js（camelCase）已重命名为 stocktake.js（小写）以匹配 PC 命名约定。
 * - 本文件同时导出 legacy API（generateTakeNo/submitStockTake/queryStockLedger）
 *   和新工作流 API（listStocktakes/createStocktake 等）。
 * - legacy POST /stockTake 已在后端 Task 8 收口（fail-closed），但保留导出以维持
 *   field-work 页面向后兼容；新页面应使用新工作流 API。
 *
 * 安全契约（新工作流）：
 * 1. 端点 /stocktakes（新工作流），与 PC 共享后端权限码 finance:stocktake:*
 * 2. 盲盘保护：counter 视角且任务未提交时，expectedQuantity/varianceQuantity/varianceAmount/unitCost 由后端置 null
 * 3. 幂等键：前端生成 ${stocktakeId}-${productId}-${action}-${version}，重复提交复用同一值
 * 4. 旧 POST /stockTake 已收口（Task 8），新工作流取代之
 *
 * 后端接口：/finance/stocktakes（FinStocktakeController）
 *
 * @module api/stocktake
 */
import { request } from './index.js'

// ==================== Legacy API（Task 8 收口，保留导出以兼容 field-work 页面） ====================

/**
 * 生成盘点单号（客户端唯一，用于 legacy 幂等）。
 * 格式：TK-{timestamp}-{random}
 * @returns {string}
 * @deprecated 新工作流请使用 buildIdempotencyKey
 */
export function generateTakeNo() {
  const ts = Date.now()
  const rand = Math.floor(Math.random() * 10000).toString().padStart(4, '0')
  return 'TK-' + ts + '-' + rand
}

/**
 * 提交库存盘点（legacy，后端已 Task 8 收口，调用将返回迁移提示）。
 * @param {Object} params 盘点参数
 * @returns {Promise<number>} 盘点流水ID（实际后端抛出 ServiceException 迁移提示）
 * @deprecated 请使用新工作流 createStocktake → countItem → submitStocktake → approveStocktake → postStocktake
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
 * @param {Object} params 查询参数
 * @returns {Promise<Object>} 流水分页结果
 */
export function queryStockLedger(params = {}) {
  return request({
    url: '/report/stock/ledger/page',
    method: 'POST',
    data: params
  })
}

// ==================== 新工作流 API（Task 10：与 PC 共享 /stocktakes 端点） ====================

/**
 * 生成本地幂等键（与后端 count_idempotency_key 唯一约束配合）。
 * @param {number} stocktakeId
 * @param {number} productId
 * @param {'count'|'recount'} action
 * @param {number} version
 * @returns {string}
 */
export function buildIdempotencyKey(stocktakeId, productId, action, version) {
  return `${stocktakeId}-${productId}-${action}-${version}`
}

/**
 * 分页查询盘点任务。
 * @param {Object} params { deptId?, status?, takeNo?, startDate?, endDate?, pageNum?, pageSize? }
 * @returns {Promise<Object>} { rows, total }
 */
export function listStocktakes(params = {}) {
  return request({
    url: '/finance/stocktakes',
    method: 'GET',
    data: params
  })
}

/**
 * 盘点任务详情（counter 视角盲盘保护）。
 * @param {number} stocktakeId
 * @returns {Promise<Object>} StocktakeDetailVO
 */
export function getStocktakeDetail(stocktakeId) {
  return request({
    url: `/finance/stocktakes/${stocktakeId}`,
    method: 'GET'
  })
}

/**
 * 创建盘点任务。
 * @param {Object} data { takeNo, deptId, scopeType, productIds, counterUserId, recountUserId?, remark? }
 * @returns {Promise<Object>}
 */
export function createStocktake(data = {}) {
  return request({
    url: '/finance/stocktakes',
    method: 'POST',
    data
  })
}

/**
 * 分配盘点人和复盘人。
 * @param {number} stocktakeId
 * @param {Object} data { counterUserId, recountUserId?, version }
 * @returns {Promise<Object>}
 */
export function assignCounter(stocktakeId, data = {}) {
  return request({
    url: `/finance/stocktakes/${stocktakeId}/assign`,
    method: 'PUT',
    data
  })
}

/**
 * 启动盘点（DRAFT -> COUNTING，冻结库存快照）。
 * @param {number} stocktakeId
 * @param {number} version
 * @returns {Promise<Object>}
 */
export function startStocktake(stocktakeId, version) {
  return request({
    url: `/finance/stocktakes/${stocktakeId}/start?version=${version}`,
    method: 'PUT'
  })
}

/**
 * 行录入（盲盘，counter 视角不显示期望值）。
 * @param {number} stocktakeId
 * @param {number} itemId
 * @param {Object} data { actualQuantity, reasonCode?, reason?, idempotencyKey, version }
 * @returns {Promise<Object>}
 */
export function countItem(stocktakeId, itemId, data = {}) {
  return request({
    url: `/finance/stocktakes/${stocktakeId}/items/${itemId}/count`,
    method: 'PUT',
    data
  })
}

/**
 * 提交盘点（COUNTING -> SUBMITTED，生成临时方差，触发复盘阈值）。
 * @param {number} stocktakeId
 * @param {number} version
 * @returns {Promise<Object>}
 */
export function submitStocktake(stocktakeId, version) {
  return request({
    url: `/finance/stocktakes/${stocktakeId}/submit?version=${version}`,
    method: 'PUT'
  })
}

/**
 * 复盘行录入（RECOUNTING，复盘人须与盘点人不同）。
 * @param {number} stocktakeId
 * @param {number} itemId
 * @param {Object} data { recountQuantity, reasonCode?, reason?, idempotencyKey, version }
 * @returns {Promise<Object>}
 */
export function recountItem(stocktakeId, itemId, data = {}) {
  return request({
    url: `/finance/stocktakes/${stocktakeId}/items/${itemId}/recount`,
    method: 'PUT',
    data
  })
}

/**
 * 审批盘点（SUBMITTED/RECOUNTING -> APPROVED/REJECTED）。
 * @param {number} stocktakeId
 * @param {Object} data { decision, comment?, version }
 * @returns {Promise<Object>}
 */
export function approveStocktake(stocktakeId, data = {}) {
  return request({
    url: `/finance/stocktakes/${stocktakeId}/approve`,
    method: 'PUT',
    data
  })
}

/**
 * 过账盘点（APPROVED -> POSTED，数量与移动平均成本原子过账）。
 * @param {number} stocktakeId
 * @param {number} version
 * @returns {Promise<Object>}
 */
export function postStocktake(stocktakeId, version) {
  return request({
    url: `/finance/stocktakes/${stocktakeId}/post?version=${version}`,
    method: 'PUT'
  })
}

/**
 * 取消盘点任务（过账前 -> CANCELLED）。
 * @param {number} stocktakeId
 * @param {number} version
 * @returns {Promise<Object>}
 */
export function cancelStocktake(stocktakeId, version) {
  return request({
    url: `/finance/stocktakes/${stocktakeId}/cancel?version=${version}`,
    method: 'PUT'
  })
}

/**
 * 整单冲销（POSTED -> REVERSED，生成红字库存与成本台账）。
 * @param {number} stocktakeId
 * @param {Object} data { reason, idempotencyKey, version }
 * @returns {Promise<Object>}
 */
export function reverseStocktake(stocktakeId, data = {}) {
  return request({
    url: `/finance/stocktakes/${stocktakeId}/reverse`,
    method: 'PUT',
    data
  })
}
