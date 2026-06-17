export const SERVICE_STATUS_TARGETS = [
  { key: 'gateway', name: '网关服务', url: '/actuator/health' },
  { key: 'auth', name: '认证服务', url: '/auth/actuator/health' },
  { key: 'system', name: '系统服务', url: '/system/actuator/health' },
  { key: 'member', name: '会员服务', url: '/member/actuator/health' },
  { key: 'finance', name: '财务服务', url: '/finance/actuator/health' },
  { key: 'file', name: '文件服务', url: '/file/actuator/health' },
  { key: 'gen', name: '代码生成', url: '/gen/actuator/health' },
  { key: 'job', name: '任务调度', url: '/job/actuator/health' }
]

const SYSTEM_HEALTH_BASE_ITEMS = [
  { key: 'cpu', label: 'CPU 使用率', value: 35, color: '#2A6F97' },
  { key: 'memory', label: '内存使用率', value: 62, color: '#F59E0B' },
  { key: 'disk', label: '磁盘使用率', value: 45, color: '#10B981' }
]

export function normalizeDeptOptions(list = [], level = 0) {
  if (!Array.isArray(list)) return []
  return list.reduce((items, dept) => {
    if (!dept) return items
    const id = dept.deptId ?? dept.id ?? dept.value
    const name = dept.deptName || dept.label || dept.name || dept.title || ''
    if (id !== undefined && id !== null && name) {
      items.push({
        ...dept,
        id,
        name,
        displayName: `${'　'.repeat(level)}${level ? '└ ' : ''}${name}`
      })
    }
    const children = dept.children || []
    if (children.length) {
      items.push(...normalizeDeptOptions(children, level + 1))
    }
    return items
  }, [])
}

export function resolveCurrentDept(depts = [], currentDeptId) {
  if (!Array.isArray(depts) || depts.length === 0) return null
  return depts.find((dept) => String(dept.id) === String(currentDeptId)) || depts[0]
}

export function normalizeServerStatus(list = []) {
  const services = (Array.isArray(list) ? list : []).map((item) => {
    const status = String(item.status || item.code || '').toUpperCase()
    const ok = item.ok === true || status === 'UP' || status === 'OK' || status === 'ONLINE' || item.code === 200
    return {
      ...item,
      ok,
      statusText: ok ? '在线' : '异常'
    }
  })
  const online = services.filter((item) => item.ok).length
  const total = services.length
  const healthRate = total > 0 ? Math.round((online / total) * 100) : 0
  return {
    services,
    summary: {
      total,
      online,
      healthRate,
      abnormal: Math.max(total - online, 0)
    }
  }
}

export function buildSystemHealthItems(serverStatus) {
  const summary = serverStatus?.summary || {}
  const total = Number(summary.total) || 0
  const online = Number(summary.online) || 0
  const healthRate = Number(summary.healthRate ?? (total > 0 ? Math.round((online / total) * 100) : 0)) || 0
  return [
    ...SYSTEM_HEALTH_BASE_ITEMS,
    { key: 'service', label: '服务在线率', value: healthRate, color: healthRate >= 80 ? '#10B981' : '#EF4444' }
  ]
}

export function isSystemAdminUser(user = {}, grants = []) {
  const userId = user.userId ?? user.userid ?? user.id
  const userName = user.userName || user.username || user.loginName
  const normalizedGrants = Array.isArray(grants) ? grants.map((item) => {
    if (typeof item === 'string') return item
    if (item && typeof item === 'object') return item.key || item.perms || item.permission || ''
    return ''
  }).filter(Boolean) : []
  return String(userId) === '1' || userName === 'admin' || normalizedGrants.some((grant) => grant === 'admin' || grant.startsWith('system:user'))
}
