/**
 * 登录获取 Token
 */
import http from 'k6/http'
import { check } from 'k6'
import { BASE_URL, CREDENTIALS } from './config.js'

export function login() {
  const url = `${BASE_URL}/auth/login`
  const payload = JSON.stringify({
    username: CREDENTIALS.username,
    password: CREDENTIALS.password,
  })
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  }

  const res = http.post(url, payload, params)
  const passed = check(res, {
    'login status is 200': (r) => r.status === 200,
    'login returns token': (r) => {
      const body = JSON.parse(r.body)
      return body.code === 200 && body.data && body.data.access_token
    },
  })

  if (!passed) {
    throw new Error(`登录失败: ${res.status} ${res.body}`)
  }

  const body = JSON.parse(res.body)
  return body.data.access_token
}

export function buildHeaders(token) {
  return {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
  }
}
