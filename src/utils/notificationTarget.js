const TODO_TYPES = new Set(['wf_todo', 'wf_timeout_urge', 'wf_timeout_transfer'])
const DONE_TYPES = new Set(['wf_finished', 'wf_rejected'])

// 经营任务通知类型：与工作流任务明确区分，避免误判
const OPERATING_TASK_TYPES = new Set([
  'operating_task',
  'operating_task_claimed',
  'operating_task_completed',
  'operating_task_rejected',
  'operating_task_reopened',
  'operating_task_overdue'
])

// 经营任务来源路由前缀（后端权威返回的 source_route）
const OPERATING_TASK_LINK_PREFIXES = [
  '/operatingTask',
  '/finance/reviewTask',
  '/finance/receivable',
  '/finance/stock/health'
]

function safeBizId(value) {
  const id = String(value ?? '').trim()
  if (!id || id.length > 200 || /[\u0000-\u001f]/.test(id)) return ''
  return id
}

function isOperatingTaskLink(linkUrl) {
  if (!linkUrl) return false
  return OPERATING_TASK_LINK_PREFIXES.some((prefix) => linkUrl === prefix || linkUrl.startsWith(prefix + '/'))
}

export function resolveNotificationTarget(item = {}, capabilities = {}) {
  const type = String(item.type || item.notificationType || '').trim()
  const linkUrl = String(item.linkUrl || '').trim().split('?')[0]
  const bizId = safeBizId(item.bizId)

  // 经营任务跳转：优先通过 type 判断，其次通过 linkUrl 判断
  // 必须与工作流任务区分，不能把 operating_task 当成 wf_todo 处理
  if (OPERATING_TASK_TYPES.has(type) || (isOperatingTaskLink(linkUrl) && !TODO_TYPES.has(type) && !DONE_TYPES.has(type))) {
    if (!capabilities.operatingTask) return ''
    // 经营任务详情跳转：如果有 bizId（taskId），跳转详情；否则跳转列表
    if (bizId) {
      return `/pages/operating-task/index?taskId=${encodeURIComponent(bizId)}`
    }
    return '/pages/operating-task/index'
  }

  if ((TODO_TYPES.has(type) || linkUrl === '/workflow/task') && capabilities.workflowTodo && bizId) {
    return `/pages/workflow/detail?taskId=${encodeURIComponent(bizId)}`
  }
  if ((DONE_TYPES.has(type) || linkUrl === '/workflow/instance') && capabilities.workflowDone) {
    return '/pages/workflow/todo?tab=done'
  }
  if (linkUrl === '/finance/expense' && capabilities.expenseList) {
    return '/pages/list/index?module=expense'
  }
  return ''
}
