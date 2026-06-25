/**
 * 财务模块压测场景
 */
import http from 'k6/http'
import { check, sleep } from 'k6'
import { BASE_URL } from '../config.js'
import { buildHeaders } from '../login.js'

export function financeSaleList(token) {
  const headers = buildHeaders(token)
  const res = http.get(`${BASE_URL}/finance/sale/list?pageNum=1&pageSize=10`, headers)
  check(res, {
    'finance sale list status 200': (r) => r.status === 200,
    'finance sale list response ok': (r) => {
      const body = JSON.parse(r.body)
      return body.code === 200
    },
  })
  sleep(1)
}

export function financePurchaseList(token) {
  const headers = buildHeaders(token)
  const res = http.get(`${BASE_URL}/finance/purchase/list?pageNum=1&pageSize=10`, headers)
  check(res, {
    'finance purchase list status 200': (r) => r.status === 200,
  })
  sleep(1)
}

export function financeExpenseList(token) {
  const headers = buildHeaders(token)
  const res = http.get(`${BASE_URL}/finance/expense/list?pageNum=1&pageSize=10`, headers)
  check(res, {
    'finance expense list status 200': (r) => r.status === 200,
  })
  sleep(1)
}

export function financeInvestorList(token) {
  const headers = buildHeaders(token)
  const res = http.get(`${BASE_URL}/finance/investor/list?pageNum=1&pageSize=10`, headers)
  check(res, {
    'finance investor list status 200': (r) => r.status === 200,
  })
  sleep(1)
}
