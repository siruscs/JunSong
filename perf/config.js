/**
 * k6 压测全局配置
 */

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'

export const CREDENTIALS = {
  username: __ENV.TEST_USERNAME || 'admin',
  password: __ENV.TEST_PASSWORD || 'admin123',
}

export const THRESHOLDS = {
  http_req_duration: ['p(95)<500', 'p(99)<1000'],
  http_req_failed: ['rate<0.05'],
}

export const STAGES_SMOKE = [
  { duration: '1m', target: 1 },
]

export const STAGES_LOAD = [
  { duration: '2m', target: 50 },
  { duration: '5m', target: 50 },
  { duration: '2m', target: 100 },
  { duration: '5m', target: 100 },
  { duration: '2m', target: 0 },
]

export const STAGES_STRESS = [
  { duration: '2m', target: 100 },
  { duration: '5m', target: 100 },
  { duration: '2m', target: 300 },
  { duration: '5m', target: 300 },
  { duration: '2m', target: 500 },
  { duration: '5m', target: 500 },
  { duration: '2m', target: 0 },
]

export const STAGES_SPIKE = [
  { duration: '30s', target: 100 },
  { duration: '1m', target: 1000 },
  { duration: '30s', target: 100 },
  { duration: '30s', target: 0 },
]
