const ROUTE_MAP: Record<string, string> = {
  '/finance/report/sale': '/finance/salesOperation',
  '/finance/report/expense': '/finance/expenseAnomaly',
  '/finance/report/profit': '/finance/profitDrilldown',
  '/finance/report/profitShare': '/finance/profitShareSettlement',
  '/member/report/member': '/member/contribution',
}

export function normalizeNotificationLink(linkUrl?: string | null) {
  if (!linkUrl) return ''
  return ROUTE_MAP[linkUrl] || linkUrl
}
