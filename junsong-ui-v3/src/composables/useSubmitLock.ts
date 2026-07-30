import { ref, type Ref } from 'vue'

/**
 * 统一的提交锁与幂等键生成器。
 *
 * - execute：基于 key 的防重入执行包装器，相同 key 重复触发会被忽略
 * - buildIdempotencyKey：生成包含前缀、业务标识、时间戳与随机串的幂等键
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

  function buildIdempotencyKey(prefix: string, ...parts: (string | number)[]): string {
    return `${prefix}-${parts.join('-')}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  }

  return { loading, execute, buildIdempotencyKey }
}
