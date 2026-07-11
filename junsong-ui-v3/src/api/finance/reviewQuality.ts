import request from '@/utils/request'

/**
 * R10-FIX-B: 复盘质量看板 API
 */
export function getReviewQualityDashboard(data: any) {
  return request({
    url: '/finance/review-quality/dashboard',
    method: 'post',
    data
  })
}
