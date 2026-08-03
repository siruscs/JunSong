function unwrap(response) {
  if (!response || typeof response !== 'object') return {}
  const data = response.data
  return data && typeof data === 'object' && !Array.isArray(data) ? data : response
}

export function normalizeListResponse(response) {
  const data = unwrap(response)
  const rows = data.rows || data.records || data.items || (Array.isArray(data) ? data : [])
  return {
    rows: Array.isArray(rows) ? rows : [],
    total: Number(data.total ?? data.totalCount ?? data.count ?? 0) || 0
  }
}

export function normalizeObjectResponse(response) {
  const data = unwrap(response)
  return data && typeof data === 'object' && !Array.isArray(data) ? data : {}
}
