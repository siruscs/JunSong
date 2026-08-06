import { request } from './index.js'

// 套餐档位字段与 PC 端及服务端 DTO 保持同名，避免同步时丢失赠送规则。
export const campaignPackageFields = ['packageName', 'purchaseQuantity', 'giftQuantity', 'totalQuantity', 'packagePrice', 'sortNo']

export function normalizeCampaignPackage(pkg = {}, index = 0) {
  return {
    packageName: pkg.packageName || '',
    purchaseQuantity: pkg.purchaseQuantity ?? '',
    giftQuantity: pkg.giftQuantity ?? '',
    totalQuantity: pkg.totalQuantity ?? '',
    packagePrice: pkg.packagePrice ?? '',
    sortNo: pkg.sortNo ?? index + 1
  }
}

export function listCampaignPolicies(params = {}) {
  return request({ url: '/member/campaign/policy/list', method: 'GET', data: params })
}

export function getCampaignPolicy(policyId) {
  return request({ url: `/member/campaign/policy/${policyId}`, method: 'GET' })
}

export function createCampaignPolicy(data) {
  return request({ url: '/member/campaign/policy', method: 'POST', data })
}

export function updateCampaignPolicy(policyId, data) {
  return request({ url: `/member/campaign/policy/${policyId}`, method: 'PUT', data })
}

export function changeCampaignPolicyStatus(policyId, status) {
  return request({ url: `/member/campaign/policy/${policyId}/status`, method: 'PUT', data: { status } })
}
