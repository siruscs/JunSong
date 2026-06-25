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
  try {
    const result = safeEvaluateExpression(expression, scope)
    if (result === null || !Number.isFinite(result)) return null
    return roundTo(result, scale, mode)
  } catch {
    return null
  }
}

/* ========== 安全算术表达式求值器（替代 new Function） ========== */

type SafeToken =
  | { type: 'NUM'; value: number }
  | { type: 'VAR'; name: string }
  | { type: 'OP'; op: '+' | '-' | '*' | '/' }
  | { type: 'LPAREN' }
  | { type: 'RPAREN' }

function safeTokenize(expr: string): SafeToken[] {
  const tokens: SafeToken[] = []
  let i = 0
  while (i < expr.length) {
    const ch = expr[i]
    if (/\s/.test(ch)) { i++; continue }
    if (/\d/.test(ch) || (ch === '.' && /\d/.test(expr[i + 1] || ''))) {
      let j = i
      while (j < expr.length && (/\d/.test(expr[j]) || expr[j] === '.')) j++
      const num = parseFloat(expr.slice(i, j))
      if (Number.isNaN(num)) throw new Error('Invalid number')
      tokens.push({ type: 'NUM', value: num })
      i = j; continue
    }
    if (/[A-Za-z_]/.test(ch)) {
      let j = i
      while (j < expr.length && /[A-Za-z0-9_]/.test(expr[j])) j++
      tokens.push({ type: 'VAR', name: expr.slice(i, j) })
      i = j; continue
    }
    if (ch === '+') { tokens.push({ type: 'OP', op: '+' }); i++; continue }
    if (ch === '-') { tokens.push({ type: 'OP', op: '-' }); i++; continue }
    if (ch === '*') { tokens.push({ type: 'OP', op: '*' }); i++; continue }
    if (ch === '/') { tokens.push({ type: 'OP', op: '/' }); i++; continue }
    if (ch === '(') { tokens.push({ type: 'LPAREN' }); i++; continue }
    if (ch === ')') { tokens.push({ type: 'RPAREN' }); i++; continue }
    throw new Error(`Unexpected character: ${ch}`)
  }
  return tokens
}

function safeEvaluateExpression(expr: string, scope: Record<string, any>): number | null {
  const tokens = safeTokenize(expr)
  let pos = 0

  function peek(): SafeToken | undefined { return tokens[pos] }
  function consume(): SafeToken | undefined { return tokens[pos++] }

  function parseExpr(): number {
    let value = parseTerm()
    while (true) {
      const tok = peek()
      if (tok?.type === 'OP' && (tok.op === '+' || tok.op === '-')) {
        consume()
        const rhs = parseTerm()
        value = tok.op === '+' ? value + rhs : value - rhs
      } else { break }
    }
    return value
  }

  function parseTerm(): number {
    let value = parseFactor()
    while (true) {
      const tok = peek()
      if (tok?.type === 'OP' && (tok.op === '*' || tok.op === '/')) {
        consume()
        const rhs = parseFactor()
        if (tok.op === '*') { value = value * rhs }
        else { if (rhs === 0) return 0; value = value / rhs }
      } else { break }
    }
    return value
  }

  function parseFactor(): number {
    const tok = peek()
    if (!tok) throw new Error('Unexpected end of expression')
    if (tok.type === 'NUM') { consume(); return tok.value }
    if (tok.type === 'VAR') {
      consume()
      const raw = scope[tok.name]
      const num = raw === '' || raw === null || raw === undefined ? 0 : Number(raw)
      return Number.isFinite(num) ? num : 0
    }
    if (tok.type === 'LPAREN') {
      consume()
      const value = parseExpr()
      if (peek()?.type !== 'RPAREN') throw new Error('Missing closing parenthesis')
      consume()
      return value
    }
    throw new Error(`Unexpected token: ${tok.type}`)
  }

  if (tokens.length === 0) return null
  const result = parseExpr()
  if (pos < tokens.length) throw new Error('Unexpected token after end')
  return result
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
