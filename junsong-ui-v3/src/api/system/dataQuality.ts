import request from '@/utils/request'

export interface DataQualityIssue {
  issueType: string
  module: string
  severity: 'HIGH' | 'MEDIUM' | 'LOW'
  issueCount: number
  sourceTables: string
  reason: string
  drilldownPath: string
}

export interface DataQualityDashboard {
  status: 'HEALTHY' | 'WARN' | 'BLOCKED' | 'ERROR'
  totalIssueCount: number
  highIssueCount: number
  mediumIssueCount: number
  lowIssueCount: number
  issues: DataQualityIssue[]
  dbErrorCount: number
  dbErrors: string[]
}

export function getDataQualityDashboard() {
  return request<DataQualityDashboard>({
    url: '/system/data-quality/dashboard',
    method: 'get',
  })
}
