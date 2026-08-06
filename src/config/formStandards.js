// 所有小程序动态表单和自定义表单共用的数值规范。
// 新增字段优先在模块配置中声明 precision；未声明时按字段名分类兜底。
export const QUANTITY_PRECISION = 3
export const MONEY_PRECISION = 2

export const QUANTITY_KEYS = new Set([
  'quantity', 'saleQuantity', 'giftQuantity', 'totalQuantity', 'purchaseQuantity',
  'stock', 'stockNum', 'minStock', 'totalShares', 'remainShares', 'shares', 'exchanged'
])

export const MONEY_KEYS = new Set([
  'expenseAmount', 'advanceAmount', 'purchasePrice', 'salePrice', 'totalAmount',
  'paidAmount', 'saleAmount', 'unitPrice', 'amount', 'investAmount', 'extraAmount',
  'consumeAmount', 'seckillAmount', 'seckillPrice', 'goodsValue', 'packagePrice'
])

export function getFieldPrecision(field = {}) {
  if (Number.isInteger(field.precision)) return field.precision
  if (QUANTITY_KEYS.has(field.key)) return QUANTITY_PRECISION
  if (MONEY_KEYS.has(field.key)) return MONEY_PRECISION
  return null
}

export function formatNumber(value, precision) {
  if (value === '' || value === null || value === undefined) return ''
  const number = Number(value)
  return Number.isFinite(number) ? Number(number.toFixed(precision)) : ''
}
