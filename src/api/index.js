const DEFAULT_BASE_URL = 'http://192.168.1.8:8081'
const REQUEST_TIMEOUT = 8000

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
    uni.request({
      url: requestUrl,
      method: options.method || 'GET',
      data: options.data || {},
      header: buildHeader(options.header),
      timeout: options.timeout || REQUEST_TIMEOUT,
      success: (res) => {
        const data = res.data || {}
        const ok = res.statusCode >= 200 && res.statusCode < 300
        const bizOk = data.code === undefined || data.code === 200
        if (ok && bizOk) {
          resolve(data)
          return
        }
        // 401 token 过期
        if (res.statusCode === 401) {
          if (!options.noRedirect) {
            setToken('')
            uni.removeStorageSync('userInfo')
            uni.removeStorageSync('modules')
            uni.reLaunch({ url: '/pages/login/index' })
          }
          reject(data)
          return
        }
        const message = data.msg || data.message || '请求失败'
        if (!options.silent) {
          uni.showToast({ title: message, icon: 'none' })
        }
        reject(data)
      },
      fail: (err) => {
        const isTimeout = String(err?.errMsg || err?.message || '').includes('timeout')
        const message = isTimeout ? '请求超时，请检查后端地址' : '网络请求失败'
        const error = {
          code: isTimeout ? 'REQUEST_TIMEOUT' : 'REQUEST_FAIL',
          msg: message,
          url: requestUrl,
          errMsg: err?.errMsg || err?.message || ''
        }
        if (!options.silent) {
          console.warn('request failed', error)
          uni.showToast({ title: message, icon: 'none' })
        }
        reject(error)
      }
    })
  })
}

export function listData(path, params) {
  return request({ url: path + '/list', method: 'GET', data: params })
}

export function getData(path, id) {
  return request({ url: path + '/' + id, method: 'GET' })
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
