const STORAGE_KEY = 'mpErrorReports'
const MAX_QUEUE = 20
const SENSITIVE_KEYS = new Set([
  'token', 'accessToken', 'refreshToken', 'authorization', 'password', 'pwd',
  'phone', 'mobile', 'phonenumber', 'idCard', 'idCardNo', 'bankCard', 'bankAccount'
])

let flushing = false

function truncate(value, max = 500) {
  const text = String(value ?? '')
  return text.length > max ? text.slice(0, max) + '…' : text
}

export function sanitize(value, key = '') {
  if (SENSITIVE_KEYS.has(String(key))) return '[REDACTED]'
  if (value === null || value === undefined) return value
  if (typeof value === 'string') return truncate(value)
  if (typeof value !== 'object') return value
  if (Array.isArray(value)) return value.slice(0, 20).map((item) => sanitize(item))
  return Object.entries(value).reduce((result, [childKey, childValue]) => {
    result[childKey] = sanitize(childValue, childKey)
    return result
  }, {})
}

export function buildErrorReport(error = {}, context = {}) {
  return {
    requestId: truncate(context.requestId || error.requestId || `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`, 80),
    category: truncate(context.category || error.code || 'UNKNOWN', 80),
    message: truncate(error.msg || error.message || error.errMsg || error, 500),
    page: truncate(context.page || '', 200),
    module: truncate(context.module || '', 100),
    method: truncate(context.method || '', 12),
    url: truncate(context.url || error.url || '', 300),
    contextVersion: context.contextVersion ?? error.contextVersion ?? null,
    occurredAt: new Date().toISOString(),
    detail: sanitize(error.detail || context.detail || {})
  }
}

function readQueue() {
  if (typeof uni === 'undefined') return []
  const queue = uni.getStorageSync(STORAGE_KEY)
  return Array.isArray(queue) ? queue : []
}

function writeQueue(queue) {
  uni.setStorageSync(STORAGE_KEY, queue.slice(-MAX_QUEUE))
}

export function getErrorReports() {
  return readQueue()
}

export function clearErrorReports() {
  if (typeof uni !== 'undefined') uni.removeStorageSync(STORAGE_KEY)
}

export function flushErrorReports() {
  if (flushing || typeof uni === 'undefined' || !uni.request || !uni.getStorageSync('token')) return Promise.resolve(false)
  const queue = readQueue()
  if (!queue.length) return Promise.resolve(true)
  flushing = true
  const baseUrl = uni.getStorageSync('baseUrl') || 'https://www.junsong.vip/prod-api'
  return new Promise((resolve) => {
    uni.request({
      url: baseUrl + '/member/mp/error-report',
      method: 'POST',
      data: queue[0],
      header: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + uni.getStorageSync('token') },
      timeout: 5000,
      success: (response) => {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          writeQueue(queue.slice(1))
          flushing = false
          flushErrorReports().then(resolve)
          return
        }
        flushing = false
        resolve(false)
      },
      fail: () => {
        flushing = false
        resolve(false)
      }
    })
  })
}

export function reportError(error, context = {}) {
  if (typeof uni === 'undefined') return Promise.resolve(false)
  const queue = readQueue()
  queue.push(buildErrorReport(error, context))
  writeQueue(queue)
  return flushErrorReports()
}
