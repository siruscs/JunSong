export function formatDateTime(value, fallback = '-') {
  if (!value) return fallback
  const text = String(value).trim()
  if (!text) return fallback
  const normalized = text.replace('T', ' ').replace(/Z$/, '').replace(/\.\d{3}$/, '')
  return normalized.length >= 19 ? normalized.slice(0, 19) : normalized
}
