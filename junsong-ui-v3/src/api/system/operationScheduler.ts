import request from '@/utils/request'

export interface OperationScheduleLog {
  logId: number
  jobCode: string
  jobName: string
  triggerType: string
  status: 'SUCCESS' | 'FAILED' | 'SKIPPED' | 'PARTIAL' | 'RUNNING'
  startedAt: string
  finishedAt: string
  durationMs: number
  affectedRows: number
  resultSummary: string
  errorMessage: string
}

export interface OperationScheduleDashboard {
  recentLogs: OperationScheduleLog[]
  failureCount24h: number
  recentFailures: OperationScheduleLog[]
}

export interface OperationScheduleTriggerResult {
  logId: number
  jobCode: string
  status: string
  resultSummary: string
  errorMessage: string
}

export function getOperationSchedulerDashboard() {
  return request<OperationScheduleDashboard>({
    url: '/system/operation-scheduler/dashboard',
    method: 'get',
  })
}

export function listOperationSchedulerRecent(params: { jobCode?: string; limit?: number }) {
  return request<OperationScheduleLog[]>({
    url: '/system/operation-scheduler/recent',
    method: 'get',
    params,
  })
}

export function triggerOperationScheduler(jobCode: string) {
  return request<OperationScheduleTriggerResult>({
    url: `/system/operation-scheduler/${jobCode}/trigger`,
    method: 'post',
  })
}
