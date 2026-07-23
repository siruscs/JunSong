export function resolveListState({ loading = false, error = '', rows = [] } = {}) {
  if (loading && rows.length === 0) return 'loading'
  if (error && rows.length === 0) return 'error'
  return rows.length === 0 ? 'empty' : 'content'
}

export function isUnknownWriteOutcome(error = {}) {
  return error?.code === 'REQUEST_TIMEOUT' || error?.code === 'NETWORK_ERROR'
}
