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
