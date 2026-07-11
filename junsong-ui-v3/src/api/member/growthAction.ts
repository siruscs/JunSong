import request from '@/api/request'

export function getGrowthActionDashboard(data?: Record<string, any>) {
  return request({ url: '/member/growth-action/dashboard', method: 'post', data })
}

export function listGrowthActionCandidates(data?: Record<string, any>) {
  return request({ url: '/member/growth-action/candidates', method: 'post', data })
}

export function listGrowthActionMembers(actionId: number) {
  return request({ url: '/member/growth-action/members', method: 'get', params: { actionId } })
}

export function generateGrowthAction(data?: Record<string, any>) {
  return request({ url: '/member/growth-action/generate', method: 'post', data })
}

export function executeGrowthAction(data?: Record<string, any>) {
  return request({ url: '/member/growth-action/execute', method: 'post', data })
}

export function getGrowthActionEffect(data?: Record<string, any>) {
  return request({ url: '/member/growth-action/effect', method: 'post', data })
}
