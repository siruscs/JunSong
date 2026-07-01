import request from '@/utils/request'

export function getNodeDurationStats(processDefinitionKey: string) {
  return request({ url: '/workflow/analytics/node-duration', method: 'get', params: { processDefinitionKey } })
}

export function getUserEfficiencyStats() {
  return request({ url: '/workflow/analytics/user-efficiency', method: 'get' })
}

export function getProcessDurationStats(processDefinitionKey: string) {
  return request({ url: '/workflow/analytics/process-duration', method: 'get', params: { processDefinitionKey } })
}
