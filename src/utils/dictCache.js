const DEFAULT_TTL = 10 * 60 * 1000

function defaultStorage() {
  return typeof uni !== 'undefined'
    ? uni
    : {
        getStorageSync: () => null,
        setStorageSync: () => undefined,
        removeStorageSync: () => undefined
      }
}

export function createDictCache({ ttl = DEFAULT_TTL, now = () => Date.now(), storage = defaultStorage() } = {}) {
  const memory = new Map()
  const inFlight = new Map()

  function keyOf(dictType) {
    return `dict-cache:${dictType}`
  }

  function read(dictType) {
    const inMemory = memory.get(dictType)
    if (inMemory) return inMemory
    const persisted = storage.getStorageSync(keyOf(dictType))
    if (persisted?.rows && persisted.expiresAt) {
      memory.set(dictType, persisted)
      return persisted
    }
    return null
  }

  async function get(dictType, fetcher) {
    const cached = read(dictType)
    if (cached && cached.expiresAt > now()) return cached.rows

    if (inFlight.has(dictType)) return inFlight.get(dictType)

    const task = Promise.resolve().then(fetcher).then(rows => {
      const record = { rows: Array.isArray(rows) ? rows : [], expiresAt: now() + ttl }
      memory.set(dictType, record)
      storage.setStorageSync(keyOf(dictType), record)
      return record.rows
    }).catch(error => {
      if (cached?.rows) return cached.rows
      throw error
    }).finally(() => inFlight.delete(dictType))
    inFlight.set(dictType, task)
    return task
  }

  function clear(dictType) {
    memory.delete(dictType)
    storage.removeStorageSync(keyOf(dictType))
  }

  return { get, clear }
}

export const dictCache = createDictCache()
