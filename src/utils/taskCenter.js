const URGENCY_RANK = { overdue: 0, soon: 1, attention: 2, normal: 3 }

function parseTime(value) {
  if (!value) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  const text = String(value)
  const dateOnly = text.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  const date = dateOnly
    ? new Date(Number(dateOnly[1]), Number(dateOnly[2]) - 1, Number(dateOnly[3]))
    : new Date(text.replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function approvalUrgency(item, now) {
  const due = parseTime(item.dueDate)
  if (!due) return 'normal'
  const remaining = due.getTime() - now.getTime()
  if (remaining < 0) return 'overdue'
  return remaining <= 24 * 60 * 60 * 1000 ? 'soon' : 'normal'
}

function expenseUrgency(item, now) {
  const date = parseTime(item.expenseDate || item.createTime)
  if (!date) return 'normal'
  const ageDays = Math.floor((startOfDay(now).getTime() - startOfDay(date).getTime()) / (24 * 60 * 60 * 1000))
  if (ageDays >= 7) return 'overdue'
  if (ageDays >= 3) return 'attention'
  return 'normal'
}

function itemTime(item) {
  return parseTime(item.dueDate || item.expenseDate || item.createTime || item.startTime)?.getTime() || Number.MAX_SAFE_INTEGER
}

export function buildTaskCenterItems({ approvals = [], expenses = [], now = new Date(), preserveOrder = false } = {}) {
  const current = parseTime(now) || new Date()
  const approvalItems = approvals.filter((item) => item?.taskId || item?.id).map((item) => {
    const taskId = String(item.taskId || item.id)
    return {
      ...item,
      key: `approval:${taskId}`,
      type: 'approval',
      taskId,
      title: item.taskName || item.name || item.businessTitle || '未命名审批',
      category: item.processName || item.processDefinitionName || item.processDefinitionKey || '审批任务',
      detail: item.startUserName || item.createBy || item.applyUser || '',
      timeText: item.endTime || item.dueDate || item.createTime || item.startTime || '',
      urgency: approvalUrgency(item, current)
    }
  })
  const expenseItems = expenses.filter((item) => Number(item?.expenseId) > 0).map((item) => ({
    ...item,
    key: `verification:${Number(item.expenseId)}`,
    type: 'verification',
    expenseId: Number(item.expenseId),
    title: item.expenseContent || item.expenseNo || `费用 #${item.expenseId}`,
    category: '费用核销',
    detail: item.expenseAmount === undefined || item.expenseAmount === null ? '' : `¥${Number(item.expenseAmount || 0).toFixed(2)}`,
    timeText: item.expenseDate || item.createTime || '',
    urgency: expenseUrgency(item, current)
  }))

  const items = [...approvalItems, ...expenseItems]
  if (preserveOrder) return items
  return items.sort((a, b) => {
    const priority = URGENCY_RANK[a.urgency] - URGENCY_RANK[b.urgency]
    return priority || itemTime(a) - itemTime(b) || a.key.localeCompare(b.key)
  })
}

/**
 * 任务完成后刷新任务列表、待办计数和统一指标。
 *
 * 刷新策略：
 * 1. 刷新任务列表（当前页 + 待办计数）
 * 2. 刷新统一经营指标（Phase 5 的 getOperatingMetrics）
 * 3. 通知首页/工作台更新徽章
 *
 * @param {Object} options 刷新选项
 * @param {Function} [options.refreshTaskList] 刷新任务列表的回调
 * @param {Function} [options.refreshPendingCount] 刷新待办计数的回调
 * @param {Function} [options.refreshMetrics] 刷新统一指标的回调
 * @returns {Promise<void>}
 */
export async function refreshAfterTaskAction(options = {}) {
  const tasks = []
  if (typeof options.refreshTaskList === 'function') {
    tasks.push(Promise.resolve(options.refreshTaskList()))
  }
  if (typeof options.refreshPendingCount === 'function') {
    tasks.push(Promise.resolve(options.refreshPendingCount()))
  }
  if (typeof options.refreshMetrics === 'function') {
    tasks.push(Promise.resolve(options.refreshMetrics()))
  }
  await Promise.allSettled(tasks)
}
