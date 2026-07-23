const AUTH_EXPIRED_MESSAGES = [
  '登录已超时',
  '请重新登录',
  '登录状态已过期',
  '令牌已过期',
  '令牌不能为空'
]

export function isAuthExpiredResponse(statusCode, data = {}) {
  const message = String(data.msg || data.message || data.errMsg || '')
  return statusCode === 401 ||
    Number(data.code) === 401 ||
    AUTH_EXPIRED_MESSAGES.some((text) => message.includes(text))
}

export function shouldRecoverAuth(requestToken, currentToken) {
  return Boolean(requestToken) && requestToken === currentToken
}

export function createAuthSession({ recover }) {
  let recovery = null

  return {
    recoverOnce() {
      if (recovery) return recovery

      try {
        recovery = Promise.resolve(recover())
      } catch (error) {
        recovery = Promise.reject(error)
      }

      recovery = recovery.finally(() => { recovery = null })
      return recovery
    },
    isRecovering() {
      return recovery !== null
    }
  }
}
