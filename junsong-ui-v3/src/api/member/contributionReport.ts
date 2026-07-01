import request from '../request'

export function getContributionReport(data: any) {
  return request({ url: '/member/report/contribution', method: 'post', data })
}
