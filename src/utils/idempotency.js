/**
 * 小程序幂等键工具（纯函数，可在 Node 测试中验证）。
 *
 * 协议：
 * - 写请求（POST/PUT/DELETE/PATCH）自动携带 X-Idempotency-Key
 * - 显式传入 idempotencyKey 时复用（用于失败重试），不参与自动键管理
 * - idempotencyNewKey=true 时强制生成新键（另存为新业务）
 * - 否则按 scene 自动生成，并通过表单会话级签名复用同键
 *
 * 表单会话级键复用：
 * - 同一签名（method + 规范化 URL + body 指纹）在网络超时/失败后重试复用同键
 * - 业务成功（code=200）后清除暂存，下次视为新业务
 * - 业务失败/网络失败保留暂存，供下次重试复用
 * - body 内容改变 → 指纹改变 → 视为新请求，生成新键
 *
 * 与 PC 端 junsong-ui-v3/src/api/request.ts 保持一致语义。
 */

const MAX_KEY_LENGTH = 128

// ============================================================
// 表单会话级幂等键复用管理
// ============================================================
//
// 设计目标：保证同一业务动作在网络超时/失败后重试时复用同一幂等键，
// 业务内容改变或成功完成时生成新键。
//
// 键存储位置：uni storage（通过可注入的 storage adapter 抽象）。
// 键索引：method + 规范化 URL + bodyFingerprint（业务内容改变 → 索引变化 → 新键）。
// 生命周期：
//   - 请求前 consumeReusableKey：有则复用，无则生成新键并暂存
//   - 响应成功（code=200）：clearReusableKey 清除暂存
//   - 响应业务失败（非 200）：releaseReusableKey 保留暂存
//   - 网络层失败：releaseReusableKey 保留暂存（最关键的重试场景）
//   - 调用方显式传入 idempotencyKey：跳过自动管理
//   - 调用方设置 idempotencyNewKey=true：强制生成新键（另存为新业务）

const REUSABLE_KEY_STORAGE = 'idem:reusable-keys' // storage 命名空间
const REUSABLE_KEY_TTL_MS = 30 * 60 * 1000 // 30 分钟 TTL，防止脏数据长期驻留

let storageAdapter = null

/**
 * 注入自定义 storage adapter（用于 Node 测试）。
 * 默认使用 uni.getStorageSync / uni.setStorageSync；
 * uni 不可用时退化为空 storage（每次生成新键，安全降级）。
 * @param {{get: (key: string) => any, set: (key: string, value: any) => void} | null} adapter
 */
export function setStorageAdapter(adapter) {
  storageAdapter = adapter
}

/**
 * 默认 storage adapter：基于 uni.getStorageSync / uni.setStorageSync。
 * uni 不存在时（Node 环境）所有读返回 null、写静默跳过。
 */
function getDefaultAdapter() {
  return {
    get(key) {
      if (typeof uni === 'undefined' || !uni || typeof uni.getStorageSync !== 'function') return null
      try {
        return uni.getStorageSync(key)
      } catch {
        return null
      }
    },
    set(key, value) {
      if (typeof uni === 'undefined' || !uni || typeof uni.setStorageSync !== 'function') return
      try {
        uni.setStorageSync(key, value)
      } catch {
        // storage 不可用时静默失败
      }
    }
  }
}

function getAdapter() {
  return storageAdapter || getDefaultAdapter()
}

function isWriteMethod(method) {
  if (!method) return false
  return ['POST', 'PUT', 'DELETE', 'PATCH'].includes(String(method).toUpperCase())
}

/**
 * 生成幂等键。
 * @param {string} scene 幂等场景标识（如 sale:create）
 * @returns {string} 幂等键
 */
export function generateIdempotencyKey(scene = 'biz') {
  const ts = Date.now()
  const rand = Math.random().toString(36).slice(2, 10)
  const key = `${scene}-${ts}-${rand}`
  return key.length > MAX_KEY_LENGTH ? key.slice(0, MAX_KEY_LENGTH) : key
}

/**
 * 推断幂等场景标识。
 * 例如 POST /sale → "sale:create"，PUT /sale/1 → "sale:update"
 */
export function inferIdempotencyScene(url, method) {
  if (!url) return 'biz'
  const cleaned = String(url).replace(/^\/?(api\/)?/, '').split('?')[0]
  const segments = cleaned.split('/').filter(Boolean)
  const resource = segments[0] || 'biz'
  const actionMap = {
    post: 'create',
    put: 'update',
    patch: 'update',
    delete: 'delete'
  }
  const action = actionMap[String(method).toLowerCase()] || 'write'
  return `${resource}:${action}`
}

/**
 * 稳定序列化（按键递归排序），保证相同内容产生相同字符串。
 * @param {unknown} v
 * @returns {string}
 */
function stableStringify(v) {
  if (v == null) return ''
  if (typeof v === 'object') {
    if (Array.isArray(v)) {
      return '[' + v.map(stableStringify).join(',') + ']'
    }
    try {
      return '{' + Object.keys(v).sort().map((k) => JSON.stringify(k) + ':' + stableStringify(v[k])).join(',') + '}'
    } catch {
      return String(v)
    }
  }
  return JSON.stringify(v)
}

/**
 * 计算请求体指纹（轻量级，用于签名而非加密）。
 * - 对象：按键排序后递归稳定序列化
 * - FormData：返回固定标识（不参与指纹，避免大文件哈希开销）
 * - 其他：String(data)
 * @param {unknown} data
 * @returns {string}
 */
export function computeBodyFingerprint(data) {
  if (data == null) return 'null'
  if (typeof FormData !== 'undefined' && data instanceof FormData) {
    return 'formdata'
  }
  if (typeof data === 'object') {
    try {
      return Object.keys(data)
        .sort()
        .map((k) => `${k}=${stableStringify(data[k])}`)
        .join('&')
    } catch {
      return String(data)
    }
  }
  return String(data)
}

/**
 * 计算请求签名（用于索引待重试键）。
 * signature = method + '|' + normalizedUrl + '|' + bodyFingerprint
 *
 * URL 规范化：去除查询参数和路径变量中的动态 ID
 * 例如 /api/sale/123 → /api/sale/{id}，避免相同表单不同 ID 被视为不同签名
 * 但保留 CRUD 语义区分（add/edit/remove 路径不同）
 * @param {string} url
 * @param {string} method
 * @param {unknown} data
 * @returns {string}
 */
export function computeRequestSignature(url, method, data) {
  const m = String(method || 'get').toLowerCase()
  const cleanedUrl = (url || '').split('?')[0].replace(/\/\d+(?=\/|$)/g, '/{id}')
  return `${m}|${cleanedUrl}|${computeBodyFingerprint(data)}`
}

/**
 * 读取待重试键映射表。
 * @returns {Record<string, {key: string, ts: number}>}
 */
function readReusableKeyMap() {
  try {
    const raw = getAdapter().get(REUSABLE_KEY_STORAGE)
    if (!raw || typeof raw !== 'object') return {}
    return raw
  } catch {
    return {}
  }
}

/**
 * 写入待重试键映射表。
 */
function writeReusableKeyMap(map) {
  try {
    getAdapter().set(REUSABLE_KEY_STORAGE, map)
  } catch {
    // storage 不可用时静默失败，键管理退化为每次新键
  }
}

/**
 * 消费待重试键：命中则复用，未命中则生成新键并暂存。
 * @param {string} signature 请求签名
 * @param {string} scene 幂等场景标识
 * @returns {string} 幂等键
 */
export function consumeReusableKey(signature, scene) {
  const map = readReusableKeyMap()
  const now = Date.now()
  const existing = map[signature]
  if (existing && now - existing.ts < REUSABLE_KEY_TTL_MS) {
    // 命中：复用原键（不删除，等响应决定）
    return existing.key
  }
  // 未命中或已过期：生成新键并暂存
  const newKey = generateIdempotencyKey(scene)
  map[signature] = { key: newKey, ts: now }
  writeReusableKeyMap(map)
  return newKey
}

/**
 * 释放待重试键（响应失败时调用）：保留键以供下次重试。
 * 仅当暂存键与实际使用键一致时才保留，防止并发请求互相覆盖。
 * @param {string} signature
 * @param {string} usedKey 实际使用的幂等键
 */
export function releaseReusableKey(signature, usedKey) {
  const map = readReusableKeyMap()
  const existing = map[signature]
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
 * @param {string} signature
 */
export function clearReusableKey(signature) {
  const map = readReusableKeyMap()
  if (map[signature]) {
    delete map[signature]
    writeReusableKeyMap(map)
  }
}

/**
 * 为写请求注入 X-Idempotency-Key 头部。
 *
 * 优先级：
 * 1. headers 已显式设置 X-Idempotency-Key → 不覆盖（调用方显式控制）
 * 2. options.idempotencyKey → 复用该键（不参与自动管理，用于失败重试）
 * 3. options.idempotencyNewKey=true → 强制生成新键（另存为新业务）
 * 4. 自动键管理 → 按请求签名复用同键（表单会话级复用）
 *
 * @param {Object} options 请求选项（method/url/header/data/idempotencyKey/idempotencyNewKey/idempotencyScene）
 * @returns {Object} 注入后的 header 对象
 */
export function applyIdempotencyHeader(options) {
  const method = String(options.method || 'GET').toUpperCase()
  if (!isWriteMethod(method)) return options.header || {}

  const existingHeader =
    options.header && (options.header['X-Idempotency-Key'] || options.header['x-idempotency-key'])
  if (existingHeader) return options.header || {}

  const explicitKey = options.idempotencyKey
  const forceNewKey = options.idempotencyNewKey === true
  const scene = options.idempotencyScene || inferIdempotencyScene(options.url, method)

  let key
  if (explicitKey) {
    // 优先级2：显式键，不参与自动管理
    key = explicitKey
  } else if (forceNewKey) {
    // 优先级3：强制新键（另存为新业务）
    key = generateIdempotencyKey(scene)
  } else {
    // 优先级4：自动键管理，按签名复用同键
    const signature = computeRequestSignature(options.url, method, options.data)
    key = consumeReusableKey(signature, scene)
  }

  return {
    ...(options.header || {}),
    'X-Idempotency-Key': key
  }
}

/**
 * 响应成功时清除暂存键（业务成功 code=200 时调用）。
 * 仅对自动键管理路径生效；显式 idempotencyKey / idempotencyNewKey 跳过。
 * @param {Object} options 原始请求选项
 * @param {string} usedKey 实际使用的幂等键（从响应头中提取）
 */
export function clearIdempotencyKeyOnSuccess(options, usedKey) {
  if (!usedKey) return
  if (!isWriteMethod(options.method)) return
  if (options.idempotencyKey) return
  if (options.idempotencyNewKey === true) return
  const signature = computeRequestSignature(options.url, options.method, options.data)
  clearReusableKey(signature)
}

/**
 * 响应失败时保留暂存键（业务失败或网络层失败时调用）。
 * 仅对自动键管理路径生效；显式 idempotencyKey / idempotencyNewKey 跳过。
 * @param {Object} options 原始请求选项
 * @param {string} usedKey 实际使用的幂等键（从响应头中提取）
 */
export function releaseIdempotencyKeyOnFailure(options, usedKey) {
  if (!usedKey) return
  if (!isWriteMethod(options.method)) return
  if (options.idempotencyKey) return
  if (options.idempotencyNewKey === true) return
  const signature = computeRequestSignature(options.url, options.method, options.data)
  releaseReusableKey(signature, usedKey)
}
