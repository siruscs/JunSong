export function classifyRequestError(error = {}) {
  const statusCode = Number(error.statusCode || error.code)
  const message = String(error.errMsg || error.message || error.msg || '')
  if (message.includes('timeout')) return { kind: 'timeout', message: '请求超时，请稍后重试' }
  if (message.includes('network')) return { kind: 'network', message: '网络连接失败，请检查网络' }
  if (statusCode === 401) return { kind: 'auth', message: '登录已超时，请重新登录' }
  if (statusCode === 403) return { kind: 'permission', message: error.msg || '暂无操作权限' }
  if (statusCode >= 400 && statusCode < 500) return { kind: 'validation', message: error.msg || '提交内容有误' }
  if (statusCode >= 500) return { kind: 'server', message: '服务暂时不可用' }
  return { kind: 'unknown', message: error.msg || '请求失败' }
}

export function canRetryRequest({ method = 'GET', kind }) {
  return ['GET', 'HEAD'].includes(method.toUpperCase()) && ['timeout', 'network', 'server'].includes(kind)
}

/**
 * 判断写操作结果是否未知（超时/网络错误时可能已成功，客户端不能直接判断为失败）。
 * @param {Object} error 错误对象
 * @returns {boolean}
 */
export function isUnknownWriteOutcome(error = {}) {
  return error?.code === 'REQUEST_TIMEOUT' || error?.code === 'NETWORK_ERROR'
}

/**
 * 安全重试策略：
 * 1. GET 请求：超时/网络/服务器错误可重试，最多 3 次
 * 2. 写操作（POST/PUT/DELETE）：超时/网络错误不自动重试，因为可能已成功
 *    - 客户端应保留相同幂等键，由用户手动确认后重试
 *    - 重试时使用相同的业务单号（如 expenseNo / takeNo）保证幂等
 * 3. 401 不重试，交给 authSession 处理
 * 4. 403 不重试，权限不足
 *
 * @param {Object} options 重试选项
 * @param {string} options.method 请求方法
 * @param {Object} options.error 错误对象
 * @param {number} options.retryCount 已重试次数
 * @returns {{shouldRetry: boolean, delay: number}}
 */
export function shouldRetrySafely({ method = 'GET', error = {}, retryCount = 0 } = {}) {
  const classified = classifyRequestError(error)
  const maxRetry = 3
  const baseDelay = 1000

  // 写操作不自动重试（可能已成功），由用户手动确认
  if (!['GET', 'HEAD'].includes(method.toUpperCase())) {
    return { shouldRetry: false, delay: 0 }
  }

  // 只有超时/网络/服务器错误可重试
  if (!['timeout', 'network', 'server'].includes(classified.kind)) {
    return { shouldRetry: false, delay: 0 }
  }

  if (retryCount >= maxRetry) {
    return { shouldRetry: false, delay: 0 }
  }

  // 指数退避：1s, 2s, 4s
  const delay = baseDelay * Math.pow(2, retryCount)
  return { shouldRetry: true, delay }
}

/**
 * 生成幂等键（用于写操作重试时保持相同业务单号）。
 * @param {string} prefix 前缀（如 'EXP' / 'TK'）
 * @returns {string}
 */
export function generateIdempotencyKey(prefix = 'BIZ') {
  const ts = Date.now()
  const rand = Math.floor(Math.random() * 10000).toString().padStart(4, '0')
  return prefix + '-' + ts + '-' + rand
}
