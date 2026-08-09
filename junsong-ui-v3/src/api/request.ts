import axios, { type AxiosRequestConfig } from 'axios'
import { ElNotification, ElMessageBox, ElMessage, ElLoading } from 'element-plus'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { resolveOpenApiError } from '@/utils/openApiErrorCode'
import { tansParams, blobValidate } from '@/utils/junsong'
import cache from '@/utils/cache'
import { saveAs } from 'file-saver'

declare module 'axios' {
  export interface AxiosRequestConfig {
    silentError?: boolean
    /** 自定义幂等场景（默认按 URL 自动推断）；显式传入 idempotencyKey 时复用该键 */
    idempotencyScene?: string
    /** 显式幂等键（用于失败重试时复用原键，优先级最高，不参与自动键管理） */
    idempotencyKey?: string
    /** true 时强制生成新键（用于"另存为新业务"场景）；默认 false，重试时复用同键 */
    idempotencyNewKey?: boolean
  }
}

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

    // 幂等键自动注入：所有写请求（POST/PUT/DELETE/PATCH）必须携带 X-Idempotency-Key
    // 后端 @Idempotent AOP 切面基于此键 + tenantId + scene 做原子占位
    //
    // 键管理策略（表单会话级重试复用）：
    // 1. 优先级：headers 已显式设置 > config.idempotencyKey > 自动键管理
    // 2. 自动键管理：按 method+url+bodyFingerprint 维护"待重试键"
    //    - 同一签名重复请求（网络超时/失败后重试）→ 复用原键
    //    - 业务内容改变 → bodyFingerprint 改变 → 视为新请求，生成新键
    //    - 成功响应后清除该签名的键（下次视为新业务）
    //    - 失败响应保留键，供下次重试复用
    // 3. 人工 PUT 修改默认生成新键，避免失败后的旧业务结果污染下一次保存；显式幂等键仍由调用方控制。
    const method = String(config.method || 'get').toLowerCase()
    const isWrite = ['post', 'put', 'delete', 'patch'].includes(method)
    if (isWrite && !headers['X-Idempotency-Key'] && !headers['x-idempotency-key']) {
      const explicitKey = (config as any).idempotencyKey
      const forceNewKey = (config as any).idempotencyNewKey === true || (method === 'put' && !explicitKey)
      const scene = (config as any).idempotencyScene || inferIdempotencyScene(config.url, method)
      let autoKey: string | undefined
      if (explicitKey) {
        // 显式键优先，不参与自动管理
        autoKey = explicitKey
      } else if (forceNewKey) {
        // 强制新键（另存为新业务）
        autoKey = generateAutoIdempotencyKey(scene)
      } else {
        // 自动键管理：按请求签名复用同键
        const signature = computeRequestSignature(config.url, method, config.data)
        ;(config as any)._idempotencySig = signature
        autoKey = consumeReusableKey(signature, scene)
      }
      config.headers!['X-Idempotency-Key'] = autoKey
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
    const msg = res.data.msg || errorCode[code] || errorCode['default']
    const silentError = res.config.silentError === true
    if (res.request.responseType === 'blob' || res.request.responseType === 'arraybuffer') {
      return res.data
    }
    // 幂等键生命周期管理：业务成功（code=200）后清除待重试键，
    // 业务失败（非 200）保留键以供重试复用（自动键管理路径才生效；
    // 显式 idempotencyKey 由调用方自行管理，这里不动）。
    if (isWriteMethod(res.config.method)) {
      const explicitKey = (res.config as any).idempotencyKey
      const forceNewKey = (res.config as any).idempotencyNewKey === true
        || (String(res.config.method || '').toLowerCase() === 'put' && !explicitKey)
      if (!explicitKey && !forceNewKey) {
        // 优先使用请求拦截器存储的签名，避免 axios 序列化 data 后签名不一致
        const signature = (res.config as any)._idempotencySig
          || computeRequestSignature(res.config.url, res.config.method, res.config.data)
        const usedKey = res.config.headers?.['X-Idempotency-Key'] as string | undefined
        if (usedKey) {
          if (code === 200) {
            clearReusableKey(signature)
          } else {
            // 业务失败：保留键以供同键重试
            releaseReusableKey(signature, usedKey)
          }
        }
      }
    }
    if (code === 401) {
      // 开放平台 API 错误码：显示友好提示而非重新登录弹窗
      const openApiHint = resolveOpenApiError(msg)
      if (openApiHint) {
        ElMessage({ message: openApiHint, type: 'error', duration: 5000 })
        return Promise.reject(new Error(openApiHint))
      }
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
      if (silentError) {
        return res.data
      }
      if (!silentError) {
        ElNotification.error({ title: msg })
      }
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
    // 网络层/超时错误：保留幂等键以供同键安全重试
    // 这是最关键的重试场景——业务可能未执行也可能已执行，
    // 必须使用同一键让后端按 SUCCEEDED/PROCESSING 状态判定，避免重复执行。
    const cfg = error?.config
    if (cfg && isWriteMethod(cfg.method)) {
      const explicitKey = (cfg as any).idempotencyKey
      const forceNewKey = (cfg as any).idempotencyNewKey === true
        || (String(cfg.method || '').toLowerCase() === 'put' && !explicitKey)
      if (!explicitKey && !forceNewKey) {
        const signature = (cfg as any)._idempotencySig
          || computeRequestSignature(cfg.url, cfg.method, cfg.data)
        const usedKey = cfg.headers?.['X-Idempotency-Key'] as string | undefined
        if (usedKey) {
          releaseReusableKey(signature, usedKey)
        }
      }
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

/**
 * 根据 URL 和方法推断幂等场景标识。
 * 用于自动生成 X-Idempotency-Key 时构建 {scene}-{ts}-{rand} 格式。
 * 例如 POST /api/sale → "sale:create"，PUT /api/sale/1 → "sale:update"
 */
function inferIdempotencyScene(url: string | undefined, method: string): string {
  if (!url) return 'biz'
  // 提取 URL 中第一段业务路径（去掉 /api、/system 等前缀）
  const cleaned = url.replace(/^\/?(api\/)?/, '').split('?')[0]
  const segments = cleaned.split('/').filter(Boolean)
  const resource = segments[0] || 'biz'
  const actionMap: Record<string, string> = {
    post: 'create',
    put: 'update',
    patch: 'update',
    delete: 'delete',
  }
  const action = actionMap[method] || 'write'
  return `${resource}:${action}`
}

/**
 * 生成自动幂等键（不依赖 idempotency.ts 的 Vue ref，纯函数）。
 * 格式：{scene}-{timestamp}-{random}
 */
function generateAutoIdempotencyKey(scene: string): string {
  const ts = Date.now()
  const rand = Math.random().toString(36).slice(2, 10)
  const key = `${scene}-${ts}-${rand}`
  return key.length > 128 ? key.slice(0, 128) : key
}

// ============================================================
// 表单会话级幂等键复用管理
// ============================================================
//
// 设计目标：保证同一业务动作在网络超时/失败后重试时复用同一幂等键，
// 业务内容改变或成功完成时生成新键。
//
// 键存储位置：sessionStorage（跨页面刷新仍保留，关闭标签页清理）。
// 键索引：method + url + bodyFingerprint（业务内容改变 → 索引变化 → 新键）。
// 生命周期：
//   - 请求前 consumeReusableKey：有则复用，无则生成新键并暂存
//   - 响应成功（code=200）：clearReusableKey 清除暂存
//   - 响应业务失败（非 200）：releaseReusableKey 保留暂存
//   - 网络层失败：releaseReusableKey 保留暂存（最关键的重试场景）
//   - 调用方显式传入 idempotencyKey：跳过自动管理
//   - 调用方设置 idempotencyNewKey=true：强制生成新键（另存为新业务）
//
// 注意：bodyFingerprint 不等于完整请求体哈希，仅取关键字段做轻量签名，
// 避免对大文件上传等场景产生过重计算开销。FormData 不参与指纹计算。

const REUSABLE_KEY_STORAGE = 'idem:reusable-keys' // sessionStorage 中的命名空间

interface ReusableKeyEntry {
  key: string
  // 占位时间，超过 30 分钟自动失效（防止脏数据长期驻留）
  ts: number
}

const REUSABLE_KEY_TTL_MS = 30 * 60 * 1000

function isWriteMethod(method: string | undefined): boolean {
  if (!method) return false
  return ['post', 'put', 'delete', 'patch'].includes(String(method).toLowerCase())
}

/**
 * 计算请求签名（用于索引待重试键）。
 * signature = method + '|' + normalizedUrl + '|' + bodyFingerprint
 */
function computeRequestSignature(
  url: string | undefined,
  method: string | undefined,
  data: unknown,
): string {
  const m = String(method || 'get').toLowerCase()
  // URL 规范化：去除查询参数和路径变量中的动态 ID
  // 例如 /api/sale/123 → /api/sale/{id}，避免相同表单不同 ID 被视为不同签名
  // 但保留 CRUD 语义区分（add/edit/remove 路径不同）
  const cleanedUrl = (url || '').split('?')[0].replace(/\/\d+(?=\/|$)/g, '/{id}')
  return `${m}|${cleanedUrl}|${computeBodyFingerprint(data)}`
}

/**
 * 计算请求体指纹（轻量级，仅取关键字段）。
 * - 对象：按键排序后取值序列化
 * - FormData：返回固定标识（不参与指纹，避免大文件哈希开销）
 * - 其他：String(data)
 */
function computeBodyFingerprint(data: unknown): string {
  if (data == null) return 'null'
  if (typeof FormData !== 'undefined' && data instanceof FormData) {
    return 'formdata'
  }
  if (typeof data === 'object') {
    try {
      // 简单稳定序列化：按键排序，过滤函数和 undefined
      const sorted = Object.keys(data as object)
        .sort()
        .map((k) => `${k}=${stableStringify((data as Record<string, unknown>)[k])}`)
        .join('&')
      return sorted
    } catch {
      return String(data)
    }
  }
  return String(data)
}

function stableStringify(v: unknown): string {
  if (v == null) return ''
  if (typeof v === 'object') {
    try {
      return JSON.stringify(v, Object.keys(v as object).sort())
    } catch {
      return String(v)
    }
  }
  return String(v)
}

/**
 * 读取待重试键映射表。
 */
function readReusableKeyMap(): Record<string, ReusableKeyEntry> {
  try {
    const raw = cache.session.getJSON(REUSABLE_KEY_STORAGE)
    if (!raw || typeof raw !== 'object') return {}
    return raw as Record<string, ReusableKeyEntry>
  } catch {
    return {}
  }
}

function writeReusableKeyMap(map: Record<string, ReusableKeyEntry>): void {
  try {
    cache.session.setJSON(REUSABLE_KEY_STORAGE, map)
  } catch {
    // sessionStorage 不可用时静默失败，键管理退化为每次新键
  }
}

/**
 * 消费待重试键：有则复用，无则生成新键并暂存。
 */
function consumeReusableKey(signature: string, scene: string): string {
  const map = readReusableKeyMap()
  const now = Date.now()
  const existing = map[signature]
  if (existing && now - existing.ts < REUSABLE_KEY_TTL_MS) {
    // 命中：复用原键（不删除，等响应决定）
    return existing.key
  }
  // 未命中或已过期：生成新键并暂存
  const newKey = generateAutoIdempotencyKey(scene)
  map[signature] = { key: newKey, ts: now }
  writeReusableKeyMap(map)
  return newKey
}

/**
 * 释放待重试键（响应失败时调用）：保留键以供下次重试。
 */
function releaseReusableKey(signature: string, usedKey: string): void {
  const map = readReusableKeyMap()
  const existing = map[signature]
  // 仅当暂存键与实际使用键一致时才保留（防止并发请求互相覆盖）
  if (existing && existing.key === usedKey) {
    // 保留原 ts 不更新，让 TTL 自然过期
    return
  }
  // 暂存键与使用键不一致（可能是新键已生成），重新写入使用键
  map[signature] = { key: usedKey, ts: Date.now() }
  writeReusableKeyMap(map)
}

/**
 * 清除待重试键（响应成功时调用）：下次相同请求视为新业务。
 */
function clearReusableKey(signature: string): void {
  const map = readReusableKeyMap()
  if (map[signature]) {
    delete map[signature]
    writeReusableKeyMap(map)
  }
}

export default service
