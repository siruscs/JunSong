import request from '@/utils/request'

export function listHealthRules(query: any) {
  return request({ url: '/system/health-rule/list', method: 'get', params: query })
}

export function getHealthRule(ruleId: number) {
  return request({ url: `/system/health-rule/${ruleId}`, method: 'get' })
}

export function updateHealthRule(data: any) {
  return request({ url: '/system/health-rule', method: 'put', data })
}

export function toggleHealthRule(ruleId: number, enabled: string) {
  return request({ url: `/system/health-rule/${ruleId}/toggle`, method: 'put', data: { enabled } })
}
