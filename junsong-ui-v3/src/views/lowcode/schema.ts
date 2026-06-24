import { parseTime } from '@/utils/junsong'
import type { LcBizField } from '@/api/lowcode'
import type { FormItemRule } from 'element-plus'

export const LC_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '审批中', value: 'IN_APPROVAL', type: 'warning' },
  { label: '待履约', value: 'PENDING_FULFILLMENT', type: 'primary' },
  { label: '已完成', value: 'FULFILLED', type: 'success' },
  { label: '审批通过', value: 'APPROVED', type: 'success' },
  { label: '已驳回', value: 'REJECTED', type: 'danger' },
  { label: '已撤回', value: 'WITHDRAWN', type: 'info' },
] as const

export type LcStatusMeta = { label: string; value: string; type: string }

export function lcStatusMeta(status?: string): LcStatusMeta {
  return (LC_STATUS_OPTIONS.find((item) => item.value === status) as LcStatusMeta) || LC_STATUS_OPTIONS[0]
}

export function lcCanEdit(status?: string) {
  return ['DRAFT', 'REJECTED'].includes(status || 'DRAFT')
}

export function lcCanSubmit(status?: string) {
  return ['DRAFT', 'REJECTED'].includes(status || 'DRAFT')
}

export function lcCanWithdraw(status?: string) {
  return status === 'IN_APPROVAL'
}

export function lcCanFulfill(status?: string) {
  return status === 'PENDING_FULFILLMENT'
}

export function isTrue(flag?: string) {
  return flag === '1' || flag === 'Y' || flag === 'true'
}

export function lcFormatDateTime(value?: string | null) {
  if (!value) return '-'
  return parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') || '-'
}

const SELECT_LIKE = new Set(['dict', 'select', 'multi-select', 'sys-ref'])
const DATE_LIKE = new Set(['date', 'datetime', 'date-range', 'time', 'time-range'])

export function isSelectLike(fieldType?: string) {
  return SELECT_LIKE.has(fieldType || '')
}

export function isDateLike(fieldType?: string) {
  return DATE_LIKE.has(fieldType || '')
}

export function parseFieldExt(field: LcBizField): Record<string, any> {
  if (!field.fieldExt) return {}
  try {
    const parsed = JSON.parse(field.fieldExt)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

export function buildFieldRules(field: LcBizField): FormItemRule[] {
  const rules: FormItemRule[] = []
  if (isTrue(field.required)) {
    const trigger = isSelectLike(field.fieldType) || isDateLike(field.fieldType) ? 'change' : 'blur'
    rules.push({ required: true, message: `请填写${field.fieldLabel}`, trigger })
  }
  const rule = (field.validateRule || '').trim()
  if (rule) {
    if (rule.startsWith('regex:')) {
      const pattern = rule.slice('regex:'.length)
      try {
        rules.push({ pattern: new RegExp(pattern), message: `${field.fieldLabel}格式不正确`, trigger: 'blur' })
      } catch {
        // ignore invalid pattern
      }
    } else if (rule === 'phone') {
      rules.push({ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' })
    } else if (rule === 'email') {
      rules.push({ pattern: /^[\w.-]+@[\w-]+(\.[\w-]+)+$/, message: '请输入正确的邮箱', trigger: 'blur' })
    } else if (rule === 'idcard') {
      rules.push({ pattern: /(^\d{15}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号', trigger: 'blur' })
    }
  }
  return rules
}

function roundTo(num: number, scale = 2, mode = 'HALF_UP'): number {
  if (!Number.isFinite(num)) return num
  const factor = Math.pow(10, scale)
  if (mode === 'FLOOR') return Math.floor(num * factor) / factor
  if (mode === 'CEIL') return Math.ceil(num * factor) / factor
  return Math.round(num * factor) / factor
}

export function evalComputed(expression: string, scope: Record<string, any>, scale = 2, mode = 'HALF_UP'): number | null {
  if (!expression) return null
  if (!/^[\w\s.+\-*/()]*$/.test(expression)) return null
  const tokens = expression.match(/[A-Za-z_]\w*/g) || []
  const args: string[] = []
  const values: number[] = []
  let divByZero = false
  for (const token of Array.from(new Set(tokens))) {
    const raw = scope[token]
    const num = raw === '' || raw === null || raw === undefined ? 0 : Number(raw)
    args.push(token)
    values.push(Number.isFinite(num) ? num : 0)
  }
  if (/\/\s*0(\b|$)/.test(expression)) divByZero = true
  for (const arg of args) {
    if (new RegExp(`/\\s*${arg}\\b`).test(expression) && Number(scope[arg]) === 0) divByZero = true
  }
  if (divByZero) return null
  try {
    // eslint-disable-next-line no-new-func
    const fn = new Function(...args, `return (${expression});`)
    const result = fn(...values)
    if (!Number.isFinite(result)) return null
    return roundTo(result, scale, mode)
  } catch {
    return null
  }
}

export function defaultValueFor(field: LcBizField): any {
  const dv = field.defaultValue
  if (field.fieldType === 'multi-select' || field.fieldType === 'date-range' || field.fieldType === 'time-range') {
    if (!dv) return []
    try {
      const parsed = JSON.parse(dv)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return dv.split(',').map((item) => item.trim()).filter(Boolean)
    }
  }
  if (field.fieldType === 'boolean') {
    return dv === '1' || dv === 'true' || dv === 'Y'
  }
  if (field.fieldType === 'number' || field.fieldType === 'decimal' || field.fieldType === 'percent') {
    return dv === undefined || dv === null || dv === '' ? null : Number(dv)
  }
  return dv ?? ''
}
