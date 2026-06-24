import axios, { type AxiosRequestConfig } from 'axios'
import { ElNotification, ElMessageBox, ElMessage, ElLoading } from 'element-plus'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { tansParams, blobValidate } from '@/utils/junsong'
import cache from '@/utils/cache'
import { saveAs } from 'file-saver'

let downloadLoadingInstance: ReturnType<typeof ElLoading.service>
export const isRelogin = { show: false }

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 10000,
})

service.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'

service.interceptors.request.use(
  (config) => {
    const headers = (config.headers || {}) as any
    const isToken = headers.isToken === false
    const isRepeatSubmit = headers.repeatSubmit === false
    const interval = headers.interval || 1000
    const isFormData = typeof FormData !== 'undefined' && config.data instanceof FormData

    if (isFormData) {
      delete headers['Content-Type']
      delete headers['content-type']
    }

    if (getToken() && !isToken) {
      config.headers!['Authorization'] = 'Bearer ' + getToken()
    }

    if (config.method === 'get' && config.params) {
      let url = config.url + '?' + tansParams(config.params)
      url = url.slice(0, -1)
      config.params = {}
      config.url = url
    }

    if (!isFormData && !isRepeatSubmit && (config.method === 'post' || config.method === 'put')) {
      const requestObj = {
        url: config.url,
        data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data,
        time: new Date().getTime(),
      }
      const requestSize = Object.keys(JSON.stringify(requestObj)).length
      const limitSize = 5 * 1024 * 1024
      if (requestSize >= limitSize) {
        console.warn(`[${config.url}]: 请求数据大小超出允许的5M限制，无法进行防重复提交验证。`)
        return config
      }
      const sessionObj = cache.session.getJSON('sessionObj')
      if (sessionObj === undefined || sessionObj === null || sessionObj === '') {
        cache.session.setJSON('sessionObj', requestObj)
      } else {
        const s_url = sessionObj.url
        const s_data = sessionObj.data
        const s_time = sessionObj.time
        if (
          s_data === requestObj.data &&
          requestObj.time - s_time < interval &&
          s_url === requestObj.url
        ) {
          const message = '数据正在处理，请勿重复提交'
          console.warn(`[${s_url}]: ${message}`)
          return Promise.reject(new Error(message))
        } else {
          cache.session.setJSON('sessionObj', requestObj)
        }
      }
    }
    return config
  },
  (error) => {
    console.log(error)
    return Promise.reject(error)
  },
)

service.interceptors.response.use(
  (res) => {
    const code = res.data.code || 200
    const msg = errorCode[code] || res.data.msg || errorCode['default']
    if (res.request.responseType === 'blob' || res.request.responseType === 'arraybuffer') {
      return res.data
    }
    if (code === 401) {
      const requestConfig = res.config
      const isTokenRequest = !(requestConfig as any)._isToken
      const currentPath = window.location.pathname
      const isLoginPage = currentPath === '/login' || currentPath === '/register'
      if (isTokenRequest && !isRelogin.show && !isLoginPage) {
        isRelogin.show = true
        const isKickout = msg && msg.includes('其他设备登录')
        const confirmText = isKickout
          ? '您的账号已在其他设备登录，请重新登录'
          : '登录状态已过期，您可以继续留在该页面，或者重新登录'
        ElMessageBox.confirm(confirmText, '系统提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning',
        })
          .then(() => {
            isRelogin.show = false
            import('@/stores/user').then(({ useUserStore }) => {
              useUserStore().logout().then(() => {
                location.href = '/index'
              })
            })
          })
          .catch(() => {
            isRelogin.show = false
          })
      }
      return Promise.reject('无效的会话，或者会话已过期，请重新登录。')
    } else if (code === 500) {
      ElMessage({ message: msg, type: 'error' })
      return Promise.reject(new Error(msg))
    } else if (code === 601) {
      ElMessage({ message: msg, type: 'warning' })
      return Promise.reject('error')
    } else if (code !== 200) {
      ElNotification.error({ title: msg })
      return Promise.reject('error')
    } else {
      return res.data
    }
  },
  (error) => {
    console.log('err' + error)
    let { message } = error
    if (message === 'Network Error') {
      message = '后端接口连接异常'
    } else if (message.includes('timeout')) {
      message = '系统接口请求超时'
    } else if (message.includes('Request failed with status code')) {
      message = '系统接口' + message.slice(-3) + '异常'
    }
    ElMessage({ message, type: 'error', duration: 5000 })
    return Promise.reject(error)
  },
)

export function download(url: string, params: any, filename: string, config?: AxiosRequestConfig) {
  downloadLoadingInstance = ElLoading.service({
    text: '正在下载数据，请稍候',
    background: 'rgba(0, 0, 0, 0.7)',
  })
  return service
    .post(url, params, {
      transformRequest: [(params) => tansParams(params)],
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      responseType: 'blob',
      ...config,
    })
    .then(async (data: any) => {
      const isBlob = blobValidate(data)
      if (isBlob) {
        const blob = new Blob([data])
        saveAs(blob, filename)
      } else {
        const resText = await (data as Blob).text()
        const rspObj = JSON.parse(resText)
        const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
        ElMessage.error(errMsg)
      }
      downloadLoadingInstance.close()
    })
    .catch((r) => {
      console.error(r)
      ElMessage.error('下载文件出现错误，请联系管理员！')
      downloadLoadingInstance.close()
    })
}

export default service
