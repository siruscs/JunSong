const TODO_TYPES = new Set(['wf_todo', 'wf_timeout_urge', 'wf_timeout_transfer'])
const DONE_TYPES = new Set(['wf_finished', 'wf_rejected'])

function safeBizId(value) {
  const id = String(value ?? '').trim()
  if (!id || id.length > 200 || /[\u0000-\u001f]/.test(id)) return ''
  return id
}

export function resolveNotificationTarget(item = {}, capabilities = {}) {
  const type = String(item.type || item.notificationType || '').trim()
  const linkUrl = String(item.linkUrl || '').trim().split('?')[0]
  const bizId = safeBizId(item.bizId)

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
