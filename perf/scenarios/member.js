/**
 * 会员模块压测场景
 */
import http from 'k6/http'
import { check, sleep } from 'k6'
import { BASE_URL } from '../config.js'
import { buildHeaders } from '../login.js'

export function memberList(token) {
  const headers = buildHeaders(token)
  const res = http.get(`${BASE_URL}/member/member/list?pageNum=1&pageSize=10`, headers)
  check(res, {
    'member list status 200': (r) => r.status === 200,
    'member list response ok': (r) => {
      const body = JSON.parse(r.body)
      return body.code === 200
    },
  })
  sleep(1)
}

export function memberDashboard(token) {
  const headers = buildHeaders(token)
  const res = http.get(`${BASE_URL}/member/dashboard/statistics`, headers)
  check(res, {
    'member dashboard status 200': (r) => r.status === 200,
  })
  sleep(1)
}

export function seckillList(token) {
  const headers = buildHeaders(token)
  const res = http.get(`${BASE_URL}/member/seckill/list?pageNum=1&pageSize=10`, headers)
  check(res, {
    'seckill list status 200': (r) => r.status === 200,
  })
  sleep(1)
}

export function seckillGrab(token) {
  const headers = buildHeaders(token)
  const payload = JSON.stringify({
    seckillId: 1,
    memberId: 1,
  })
  const res = http.post(`${BASE_URL}/member/seckill/grab`, payload, headers)
  check(res, {
    'seckill grab status 200': (r) => r.status === 200,
  })
  sleep(1)
}
