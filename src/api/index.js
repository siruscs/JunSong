import { createAuthSession, createSessionRestorer, extractAccessToken, isAuthExpiredResponse } from '@/utils/authSession.js'
import { shouldRecoverAuth } from '@/utils/authSession.js'
import { workContext } from '@/utils/workContext.js'
import { mergePersistedUser, resolveDeptCollection } from '@/utils/workContext.js'
import { classifyRequestError } from '@/utils/requestPolicy.js'
import { normalizeListResponse, normalizeObjectResponse } from '@/utils/responseNormalize.js'
import {
  applyIdempotencyHeader,
  clearIdempotencyKeyOnSuccess,
  releaseIdempotencyKeyOnFailure
} from '@/utils/idempotency.js'
import { reportError } from '@/utils/errorReporter.js'

const DEFAULT_BASE_URL = 'https://www.junsong.vip/prod-api'
const REQUEST_TIMEOUT = 30000

export function getBaseUrl() {
  return uni.getStorageSync('baseUrl') || DEFAULT_BASE_URL
}

export function setBaseUrl(url) {
  uni.setStorageSync('baseUrl', url || DEFAULT_BASE_URL)
}

export function getToken() {
  return uni.getStorageSync('token') || ''
}

export function setToken(token) {
  uni.setStorageSync('token', token || '')
}

export function clearSession() {
  setToken('')
  workContext.clear()
  uni.removeStorageSync('userInfo')
  uni.removeStorageSync('modules')
  uni.removeStorageSync('permissions')
}

const authSession = createAuthSession({
  recover: async () => {
    clearSession()
    uni.showToast({ title: '登录已超时，请重新登录', icon: 'none' })
    await new Promise((resolve) => setTimeout(resolve, 300))
    await new Promise((resolve) => {
      uni.reLaunch({ url: '/pages/login/index', complete: resolve })
    })
  }
})

function buildHeader(header = {}) {
  const token = getToken()
  const { isToken, ...rest } = header
  const needToken = isToken !== false
  return {
    'Content-Type': 'application/json',
    ...(token && needToken ? { Authorization: 'Bearer ' + token } : {}),
    ...rest
  }
}

/**
 * 通用请求方法
 * @param {Object} options
 * @param {string} options.url - 请求路径（不含 baseUrl）
 * @param {string} [options.method] - 请求方法
 * @param {Object} [options.data] - 请求数据
 * @param {Object} [options.header] - 额外请求头
 * @param {boolean} [options.noRedirect] - 为 true 时 401 不自动跳转登录页
 */
export function request(options) {
  return new Promise((resolve, reject) => {
    const requestUrl = getBaseUrl() + options.url
    const timeoutMs = options.timeout || REQUEST_TIMEOUT
    const requestToken = getToken()
    const contextSnapshot = workContext.snapshot()
    const contextVersion = contextSnapshot.version
    const startTime = Date.now()
    let completed = false
    let requestTask = null
    
    const finish = (result, isSuccess) => {
      if (completed) return
      completed = true
      const duration = Date.now() - startTime
      if (duration > 5000) {
        console.warn('[request] slow request:', requestUrl, 'duration:', duration + 'ms')
      }
      if (isSuccess) resolve(result)
      else {
        reportError(result, {
          category: result?.code || 'REQUEST_FAILURE',
          method: options.method || 'GET',
          url: options.url,
          contextVersion
        }).catch(() => {})
        reject(result)
      }
    }
    
    const guardTimer = setTimeout(() => {
      if (!completed) {
        console.warn('[request] guard timeout:', requestUrl)
        requestTask?.abort?.()
        finish({
          code: 'REQUEST_TIMEOUT',
          msg: '请求超时（守护定时器触发）',
          url: requestUrl,
          errMsg: 'request:fail timeout (guard)',
          detail: { errMsg: 'timeout' }
        }, false)
      }
    }, timeoutMs)
    
    // 幂等键：applyIdempotencyHeader 返回注入后的 header 对象，
    // 从中提取实际使用的 X-Idempotency-Key，供响应拦截时管理键生命周期
    const idempotencyHeader = applyIdempotencyHeader(options)
    const usedKey = idempotencyHeader['X-Idempotency-Key'] || idempotencyHeader['x-idempotency-key'] || ''

    requestTask = uni.request({
      url: requestUrl,
      method: options.method || 'GET',
      data: options.data || {},
      header: buildHeader(idempotencyHeader),
      timeout: timeoutMs + 5000,
      success: (res) => {
        clearTimeout(guardTimer)
        const data = res.data || {}
        const ok = res.statusCode >= 200 && res.statusCode < 300
        const bizOk = data.code === undefined || data.code === 200
        if (ok && bizOk) {
          if (options.contextSensitive && !workContext.isCurrent(contextVersion)) {
            finish({ code: 'STALE_CONTEXT', msg: '部门已切换，操作结果待确认', url: requestUrl, contextVersion }, false)
            return
          }
          // 业务成功：清除暂存键，下次相同请求视为新业务
          clearIdempotencyKeyOnSuccess(options, usedKey)
          const result = options.withContextMeta
            ? {
                ...data,
                contextMeta: {
                  contextVersion,
                  currentDeptId: contextSnapshot.currentDeptId,
                  staleContext: !workContext.isCurrent(contextVersion)
                }
              }
            : data
          finish(result, true)
          return
        }
        // 业务失败：保留暂存键以供同键安全重试
        releaseIdempotencyKeyOnFailure(options, usedKey)
        // 网关 AuthFilter 返回 HTTP 200 + {"code": 401}（非标准 HTTP 401），
        // 后端 HeaderInterceptor 验证失败也可能返回业务码 401。
        // 同时检查 HTTP 状态码和业务码，确保两种情况都能正确处理。
        if (isAuthExpiredResponse(res.statusCode, data)) {
          if (!options.noRedirect && shouldRecoverAuth(requestToken, getToken())) {
            authSession.recoverOnce().catch(() => {})
          }
          finish(data, false)
          return
        }
        const message = data.msg || data.message || '请求失败'
        if (!options.silent) {
          uni.showToast({ title: message, icon: 'none' })
        }
        finish(data, false)
      },
      fail: (err) => {
        clearTimeout(guardTimer)
        // 网络层失败：保留幂等键以供同键安全重试
        // 这是最关键的重试场景——业务可能未执行也可能已执行，
        // 必须使用同一键让后端按 SUCCEEDED/PROCESSING 状态判定，避免重复执行
        releaseIdempotencyKeyOnFailure(options, usedKey)
        const isCertificate = String(err?.errMsg || '').includes('certificate') || String(err?.errMsg || '').includes('SSL')
        const classified = classifyRequestError(err)
        const codeByKind = {
          timeout: 'REQUEST_TIMEOUT',
          network: 'NETWORK_ERROR'
        }
        const message = isCertificate ? '证书验证失败，请检查SSL配置' : classified.message
        const error = {
          code: isCertificate ? 'SSL_ERROR' : (codeByKind[classified.kind] || 'REQUEST_FAIL'),
          msg: message,
          url: requestUrl,
          errMsg: err?.errMsg || err?.message || '',
          detail: err
        }
        if (!options.silent) {
          console.warn('request failed', JSON.stringify(error, null, 2))
          uni.showModal({
            title: '请求失败',
            content: `地址: ${requestUrl}\n错误: ${err?.errMsg || '未知错误'}\n提示: ${message}`,
            showCancel: false
          })
        }
        finish(error, false)
      }
    })
  })
}

export function refreshAuthSession(options = {}) {
  if (!getToken()) return Promise.resolve(null)
  return request({
    url: '/auth/refresh',
    method: 'POST',
    noRedirect: options.noRedirect === true,
    silent: true,
    timeout: options.timeout || 8000,
    header: { isToken: true }
  }).then((response) => {
    const refreshedToken = extractAccessToken(response)
    if (refreshedToken) setToken(refreshedToken)
    return response
  })
}

export async function refreshWorkContext(options = {}) {
  if (!getToken()) return null

  const response = await request({
    url: '/system/user/getInfo',
    method: 'GET',
    noRedirect: options.noRedirect === true,
    silent: true,
    withContextMeta: true,
    timeout: options.timeout || 12000
  })
  if (response?.contextMeta?.staleContext) return workContext.snapshot()

  const { contextMeta: _contextMeta, ...payload } = response || {}
  const info = payload?.data && typeof payload.data === 'object' ? payload.data : payload
  const user = info?.user || {}
  const storedUser = uni.getStorageSync('userInfo') || {}
  const depts = resolveDeptCollection(info, storedUser)
  const currentDeptId = info?.currentDeptId ?? user.deptId ?? storedUser.currentDeptId ?? storedUser.deptId ?? null
  const mergedUser = { ...storedUser, ...user, currentDeptId, deptId: currentDeptId }
  const snapshot = workContext.hydrate({
    user: mergedUser,
    depts: depts,
    currentDeptId: currentDeptId
  })
  const persistedUser = mergePersistedUser(storedUser, user, snapshot)
  uni.setStorageSync('userInfo', persistedUser)
  return snapshot
}

const sessionRestorer = createSessionRestorer({
  getToken,
  refresh: async () => {
    await refreshAuthSession({ noRedirect: true })
    return refreshWorkContext({ noRedirect: true })
  }
})

export function restoreSession() {
  return sessionRestorer.restoreSession()
}

export function listData(path, params) {
  return request({ url: path + '/list', method: 'GET', data: params }).then(normalizeListResponse)
}

export function getData(path, id) {
  return request({ url: path + '/' + id, method: 'GET' }).then(normalizeObjectResponse)
}

export function addData(path, data) {
  return request({ url: path, method: 'POST', data })
}

export function updateData(path, data) {
  return request({ url: path, method: 'PUT', data })
}

export function deleteData(path, ids) {
  return request({ url: path + '/' + ids, method: 'DELETE' })
}

export function actionRequest(action, record, payload = {}) {
  const id = record?.[action.idKey || 'id']
  const source = { ...(record || {}), ...(payload || {}), id }
  const url = typeof action.url === 'function'
    ? action.url(record, payload)
    : action.url.replace(/\{(\w+)\}/g, (_, key) => source[key] === undefined || source[key] === null ? '' : source[key])
  return request({
    url,
    method: action.method || 'PUT',
    data: action.body === 'ids' ? [id] : payload
  })
}
