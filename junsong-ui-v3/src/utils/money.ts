/**
 * 金额格式化工具：统一按两位小数显示，带 ¥ 前缀
 * @example money(100) => '¥100.00'
 * @example money(null) => '¥0.00'
 * @example money(undefined) => '¥0.00'
 * @example money('abc') => '¥0.00'
 */
export function money(value: any): string {
  const num = Number(value ?? 0)
  if (!Number.isFinite(num)) return '¥0.00'
  return `¥${num.toFixed(2)}`
}
