import request from '../request'

// 查询复盘任务列表
export function listReviewTasks(params: Record<string, any>) {
  return request({
    url: '/finance/review-task/list',
    method: 'get',
    params,
  })
}

// 生成复盘任务
export function generateReviewTasks(data: Record<string, any>) {
  return request({
    url: '/finance/review-task/generate',
    method: 'post',
    data,
  })
}

// 标记处理中
export function markInProgress(taskId: number) {
  return request({
    url: `/finance/review-task/${taskId}/in-progress`,
    method: 'post',
  })
}

// 标记完成
export function markDone(taskId: number, data: { handlerNote: string }) {
  return request({
    url: `/finance/review-task/${taskId}/done`,
    method: 'post',
    data,
  })
}

// 标记忽略
export function markIgnored(taskId: number, data: { ignoreReason: string }) {
  return request({
    url: `/finance/review-task/${taskId}/ignored`,
    method: 'post',
    data,
  })
}

// 重开任务
export function reopenReviewTask(taskId: number | string, reason: string) {
  return request({
    url: `/finance/review-task/${taskId}/reopen`,
    method: 'post',
    data: { reason },
  })
}

// 查询任务动作效果评估
export function getReviewTaskEffect(taskId: number | string, windowDays = 7) {
  return request({
    url: `/finance/review-task/${taskId}/effect`,
    method: 'get',
    params: { windowDays },
  })
}

// 查询复盘任务处理轨迹
export function getTaskLogs(taskId: number) {
  return request({
    url: `/finance/review-task/${taskId}/logs`,
    method: 'get',
  })
}

// 从会员动作生成复盘任务
export function createTaskFromMemberAction(data: Record<string, any>) {
  return request({
    url: '/finance/review-task/from-member-action',
    method: 'post',
    data,
  })
}

// 查询动作成效汇总
export function getEffectSummary(params?: { deptIds?: number[], windowDays?: number }) {
  return request({
    url: '/finance/review-task/effect-summary',
    method: 'get',
    params,
  })
}

// 生成应收催收复盘任务
export function generateReceivableCollectionTasks(data: {
  deptId: number | string
  minAgeDays?: number
  minUnpaidAmount?: number | string
}) {
  return request({
    url: '/finance/review-task/generate/receivable-collection',
    method: 'post',
    data,
  })
}
