import { ref, type Ref } from 'vue'

/**
 * 全系统幂等键管理工具。
 *
 * 协议：
 * - 写请求（POST/PUT/DELETE）自动携带 X-Idempotency-Key 请求头
 * - 同一表单会话重试复用原键
 * - 业务内容改变或新建新业务时生成新键
 * - 键格式：{scene}-{timestamp}-{random}
 *
 * 用法：
 * 1. 在表单组件中调用 const { key, regenerate } = useIdempotencyKey('sale:create')
 * 2. submit 时通过 withIdempotency(config) 注入请求头
 * 3. 成功后调用 regenerate() 生成新键，失败后保持原键用于重试
 */

const STORAGE_PREFIX = 'idem:'
const MAX_KEY_LENGTH = 128

/**
 * 生成幂等键。
 * @param scene 幂等场景标识（如 sale:create）
 * @returns 幂等键字符串
 */
export function generateIdempotencyKey(scene: string = 'biz'): string {
  const ts = Date.now()
  const rand = Math.random().toString(36).slice(2, 10)
  const key = `${scene}-${ts}-${rand}`
  return key.length > MAX_KEY_LENGTH ? key.slice(0, MAX_KEY_LENGTH) : key
}

/**
 * 幂等键管理 composable。
 *
 * - key：当前表单会话的幂等键（ref）
 * - regenerate：生成新键（业务内容改变或新建时调用）
 * - preserve：保持原键（用于失败重试）
 */
export function useIdempotencyKey(scene: string = 'biz') {
  const key: Ref<string> = ref(generateIdempotencyKey(scene))

  function regenerate(): string {
    key.value = generateIdempotencyKey(scene)
    return key.value
  }

  function preserve(): string {
    return key.value
  }

  return { key, regenerate, preserve }
}

/**
 * 为 axios 请求配置注入幂等键请求头。
 *
 * @param config axios 请求配置
 * @param idempotencyKey 幂等键（可选，默认自动生成）
 * @param scene 幂等场景（可选，用于自动生成时）
 * @returns 注入后的配置
 */
export function withIdempotency<T extends { headers?: Record<string, string> }>(
  config: T,
  idempotencyKey?: string,
  scene?: string,
): T {
  const method = (config as any).method || 'get'
  const isWrite = ['post', 'put', 'delete', 'patch'].includes(String(method).toLowerCase())

  if (!isWrite) {
    return config
  }

  const key = idempotencyKey || generateIdempotencyKey(scene || 'biz')
  if (!config.headers) {
    ;(config as any).headers = {}
  }
  ;(config as any).headers['X-Idempotency-Key'] = key
  return config
}

/**
 * 统一提交锁。
 *
 * - execute：基于 key 的防重入执行包装器，相同 key 重复触发会被忽略
 * - loading：当前是否正在执行
 *
 * 与 useIdempotencyKey 配合使用：
 * <pre>
 * const { key, regenerate } = useIdempotencyKey('sale:create')
 * const { execute, loading } = useSubmitLock()
 *
 * async function submit() {
 *   await execute(key.value, async () => {
 *     await api.saleCreate(withIdempotency(data, key.value))
 *     regenerate() // 成功后生成新键
 *   })
 * }
 * </pre>
 */
export function useSubmitLock() {
  const loading: Ref<boolean> = ref(false)
  const lockKey: Ref<string> = ref('')

  async function execute<T>(key: string, fn: () => Promise<T>): Promise<T | undefined> {
    if (loading.value && lockKey.value === key) {
      return undefined
    }
    loading.value = true
    lockKey.value = key
    try {
      return await fn()
    } finally {
      loading.value = false
      lockKey.value = ''
    }
  }

  return { loading, execute }
}
