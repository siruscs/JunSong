import request from '../request'

export function listInvestorPayment(query: any) {
  return request({ url: '/finance/investorPayment/list', method: 'get', params: query })
}

export function getInvestorPayment(paymentId: number) {
  return request({ url: '/finance/investorPayment/' + paymentId, method: 'get' })
}

export function addInvestorPayment(data: any) {
  return request({ url: '/finance/investorPayment', method: 'post', data })
}

export function updateInvestorPayment(data: any) {
  return request({ url: '/finance/investorPayment', method: 'put', data })
}

export function delInvestorPayment(paymentId: number) {
  return request({ url: '/finance/investorPayment/' + paymentId, method: 'delete' })
}

export function getInvestorPaymentSummary(query: any) {
  return request({ url: '/finance/investorPayment/summary', method: 'get', params: query })
}