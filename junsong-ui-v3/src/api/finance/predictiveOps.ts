import request from '../request'

export function getPredictiveOpsDashboard(data?: Record<string, any>) {
  return request({
    url: '/finance/predictive-ops/dashboard',
    method: 'post',
    data,
  })
}

export function createPredictiveOpsSnapshot(data?: Record<string, any>) {
  return request({
    url: '/finance/predictive-ops/snapshot',
    method: 'post',
    data,
  })
}

export function simulatePredictiveOpsWhatIf(data: Record<string, any>) {
  return request({
    url: '/finance/predictive-ops/what-if',
    method: 'post',
    data,
  })
}
