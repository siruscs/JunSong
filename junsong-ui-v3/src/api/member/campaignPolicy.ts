import request from '../request'

export function listCampaignPolicies(query: any = {}) {
  return request({ url: '/member/campaign/policy/list', method: 'get', params: query })
}

export function getCampaignPolicy(policyId: number) {
  return request({ url: '/member/campaign/policy/' + policyId, method: 'get' })
}

export function addCampaignPolicy(data: any) {
  return request({ url: '/member/campaign/policy', method: 'post', data })
}

export function updateCampaignPolicy(policyId: number, data: any) {
  return request({ url: '/member/campaign/policy/' + policyId, method: 'put', data })
}

export function changeCampaignPolicyStatus(policyId: number, status: string) {
  return request({ url: `/member/campaign/policy/${policyId}/status`, method: 'put', params: { status } })
}
