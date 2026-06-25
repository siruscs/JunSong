/**
 * 低代码模块压测场景
 */
import http from 'k6/http'
import { check, sleep } from 'k6'
import { BASE_URL } from '../config.js'
import { buildHeaders } from '../login.js'

export function lowcodeList(token) {
  const headers = buildHeaders(token)
  const res = http.get(`${BASE_URL}/workflow/lowcode/biz/leave_apply/list?pageNum=1&pageSize=10`, headers)
  check(res, {
    'lowcode list status 200': (r) => r.status === 200,
    'lowcode list response ok': (r) => {
      const body = JSON.parse(r.body)
      return body.code === 200
    },
  })
  sleep(1)
}

export function lowcodeDetail(token) {
  const headers = buildHeaders(token)
  const res = http.get(`${BASE_URL}/workflow/lowcode/biz/leave_apply/1`, headers)
  check(res, {
    'lowcode detail status 200': (r) => r.status === 200,
  })
  sleep(1)
}

export function lowcodeMetadata(token) {
  const headers = buildHeaders(token)
  const res = http.get(`${BASE_URL}/workflow/lowcode/meta/config/leave_apply`, headers)
  check(res, {
    'lowcode metadata status 200': (r) => r.status === 200,
    'lowcode metadata has fields': (r) => {
      const body = JSON.parse(r.body)
      return body.code === 200 && body.data && body.data.fields
    },
  })
  sleep(1)
}

export function lowcodeSubmit(token) {
  const headers = buildHeaders(token)
  const payload = JSON.stringify({
    formData: {
      applicant: 'admin',
      reason: '性能测试',
      days: 1,
    },
  })
  const res = http.post(`${BASE_URL}/workflow/lowcode/biz/leave_apply`, payload, headers)
  check(res, {
    'lowcode submit status 200': (r) => r.status === 200,
    'lowcode submit success': (r) => {
      const body = JSON.parse(r.body)
      return body.code === 200
    },
  })
  sleep(1)
}
