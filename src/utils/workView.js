const MANAGEMENT_KEYS = new Set([
  'accountingPeriod',
  'profitShare',
  'costAccounting',
  'verificationRecord',
  'userManage',
  'deptManage',
  'wfTodo'
])

const PRIORITY = {
  store: ['member', 'sale', 'expense', 'pointsExchange', 'seckillRecord'],
  management: ['wfTodo', 'expense', 'verificationRecord', 'accountingPeriod', 'costAccounting', 'sale', 'member']
}

export function deriveWorkView({ depts = [], modules = [] } = {}) {
  const management = depts.length > 1 || modules.some((key) => MANAGEMENT_KEYS.has(key))

  return management
    ? { key: 'management', label: '财务运营', homeTitle: '经营管理' }
    : { key: 'store', label: '门店经营', homeTitle: '门店工作' }
}

export function prioritizeModuleKeys(keys = [], view = 'store') {
  const unique = [...new Set(keys)]
  const rank = new Map((PRIORITY[view] || PRIORITY.store).map((key, index) => [key, index]))

  return unique.sort((a, b) => (rank.get(a) ?? 999) - (rank.get(b) ?? 999))
}
