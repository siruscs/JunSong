import { parseTime } from '@/utils/junsong'

export function formatWorkflowDateTime(value?: string | null) {
  if (!value) return '-'
  return parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') || '-'
}

export function formatWorkflowDuration(durationMs?: number | null) {
  if (durationMs == null || Number.isNaN(Number(durationMs))) return '-'
  const totalSeconds = Math.max(0, Math.floor(durationMs / 1000))
  const days = Math.floor(totalSeconds / 86400)
  const hours = Math.floor((totalSeconds % 86400) / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  if (days > 0) return `${days}天 ${hours}小时`
  if (hours > 0) return `${hours}小时 ${minutes}分钟`
  if (minutes > 0) return `${minutes}分钟 ${seconds}秒`
  return `${seconds}秒`
}

export function mapWorkflowInstanceStatus(row: {
  running?: boolean
  endTime?: string | null
  suspended?: boolean
}) {
  if (row.suspended) return { label: '已挂起', type: 'warning' as const }
  if (row.running === true || !row.endTime) return { label: '运行中', type: 'success' as const }
  return { label: '已结束', type: 'info' as const }
}

export function mapWorkflowTaskTabStatus(tab: 'todo' | 'done' | 'applied', row?: { endTime?: string | null; running?: boolean }) {
  if (tab === 'todo') return { label: '待处理', type: 'warning' as const }
  if (tab === 'done') return { label: '已处理', type: 'success' as const }
  if (row?.running === true || !row?.endTime) return { label: '运行中', type: 'primary' as const }
  return { label: '已结束', type: 'info' as const }
}

export function mapWorkflowCommentType(type?: string | null) {
  if (type === 'approve') return '审批通过'
  if (type === 'reject') return '驳回'
  if (type === 'comment') return '评论'
  return type || '记录'
}

export function safeWorkflowText(value?: string | number | null) {
  if (value === null || value === undefined || value === '') return '-'
  return String(value)
}

const workflowBusinessRouteRegistry: Record<string, { path: string; queryKey: string }> = {
  store_opening_apply: { path: '/system/storeOpening', queryKey: 'orderNo' },
  refund_apply: { path: '/member/refund', queryKey: 'refundNo' },
}

export function resolveWorkflowBusinessTarget(processDefinitionKey?: string | null, businessKey?: string | null) {
  const route = processDefinitionKey ? workflowBusinessRouteRegistry[processDefinitionKey] : undefined
  if (!route || !businessKey) return undefined
  return {
    path: route.path,
    query: { [route.queryKey]: businessKey },
  }
}

export function parseWorkflowVariablesText(value?: string | null) {
  const text = value?.trim()
  if (!text) return undefined
  const parsed = JSON.parse(text)
  if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error('流程变量必须是 JSON 对象')
  }
  return parsed as Record<string, any>
}
