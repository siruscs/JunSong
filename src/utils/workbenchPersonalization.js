const normalized = (value) => String(value || '').trim().toLowerCase()

export function sanitizeModuleKeys(keys = [], authorized = []) {
  const allowed = new Set(Array.isArray(authorized) ? authorized : [])
  const source = Array.isArray(keys) ? keys : []
  return [...new Set(source)].filter((key) => allowed.has(key))
}

export function recordRecent(keys, key, authorized, limit = 6) {
  const current = sanitizeModuleKeys(keys, authorized)
  const next = sanitizeModuleKeys([key], authorized).length
    ? [key, ...current.filter((item) => item !== key)]
    : current
  const boundedLimit = Math.max(0, Math.min(6, Number.isFinite(limit) ? Math.floor(limit) : 6))
  return next.slice(0, boundedLimit)
}

export function filterModuleGroups(groups = [], query = '') {
  const source = Array.isArray(groups) ? groups : []
  const term = normalized(query)
  if (!term) return source

  return source
    .map((group) => ({
      ...group,
      items: (Array.isArray(group.items) ? group.items : []).filter((item) =>
        normalized(`${group.name || ''} ${item.title || ''} ${item.desc || ''}`).includes(term)
      )
    }))
    .filter((group) => group.items.length)
}

export function filterEntries(entries = [], query = '') {
  const source = Array.isArray(entries) ? entries : []
  const term = normalized(query)
  if (!term) return source

  return source.filter((entry) =>
    normalized(`${entry.title || ''} ${entry.desc || ''}`).includes(term)
  )
}
