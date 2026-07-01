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
