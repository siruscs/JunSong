import request from '../request'

export function getDashboardStats() {
  return request({ url: '/system/dashboard/stats', method: 'get' })
}

export function getDashboardHealth() {
  return request({ url: '/system/dashboard/health', method: 'get' })
}

export function getDashboardGovernance(includeArchived: boolean = false) {
  return request({ url: '/system/dashboard/governance', method: 'get', params: { includeArchived } })
}

export function getWorkbenchTasks() {
  return request({ url: '/system/workbench/tasks', method: 'get' })
}

// 记录治理动作
export function recordGovernanceAction(data: Record<string, any>) {
  return request({ url: '/system/dashboard/governance/action', method: 'post', data })
}

// 查询治理轨迹
export function getGovernanceLogs(taskType: string) {
  return request({ url: '/system/dashboard/governance/logs', method: 'get', params: { taskType } })
}
