import request from '../request'

/**
 * 经营任务 PC 端 API client。
 *
 * 后端契约（SysOperatingTaskController）：
 * - GET    /operatingTask/list                list
 * - GET    /operatingTask/{taskId}            getInfo
 * - POST   /operatingTask/create              create（内部幂等投递）
 * - PUT    /operatingTask/claim/{taskId}      claim
 * - PUT    /operatingTask/complete/{taskId}   complete
 * - PUT    /operatingTask/reject/{taskId}     reject
 * - PUT    /operatingTask/reopen/{taskId}     reopen
 * - GET    /operatingTask/logs/{taskId}       logs
 * - GET    /operatingTask/pendingCount        pendingCount
 *
 * 权限：system:operatingTask:list/claim/complete/reject/reopen
 * 租户/部门/权限范围由后端权威决定，前端只展示服务端返回的数据。
 */

export interface OperatingTask {
  taskId: number
  tenantId?: string
  sourceModule: string
  sourceType: string
  sourceId: string
  sourceRoute?: string
  taskType: string
  deptId?: number
  deptName?: string
  title: string
  priority: string
  severity?: string
  status: string
  assigneeId?: number
  assigneeName?: string
  occurTime?: string
  dueTime?: string
  impactAmount?: number | string
  handlerNote?: string
  rejectReason?: string
  version?: number
  createTime?: string
  updateTime?: string
}

export interface OperatingTaskLog {
  logId: number
  taskId: number
  action: string
  oldStatus?: string
  newStatus?: string
  operatorId?: number
  operatorName?: string
  note?: string
  createTime?: string
}

export interface OperatingTaskListQuery {
  pageNum?: number
  pageSize?: number
  status?: string
  assigneeId?: number
  sourceModule?: string
  sourceType?: string
  priority?: string
  deptId?: number
}

export interface OperatingTaskListResult {
  rows: OperatingTask[]
  total: number
}

/** 查询经营任务列表（分页） */
export function listOperatingTask(params: OperatingTaskListQuery) {
  return request<OperatingTaskListResult>({
    url: '/operatingTask/list',
    method: 'get',
    params,
  })
}

/** 查询任务详情 */
export function getOperatingTask(taskId: number | string) {
  return request<OperatingTask>({
    url: `/operatingTask/${taskId}`,
    method: 'get',
  })
}

/** 查询任务操作日志 */
export function listOperatingTaskLogs(taskId: number | string) {
  return request<OperatingTaskLog[]>({
    url: `/operatingTask/logs/${taskId}`,
    method: 'get',
  })
}

/** 当前用户待办计数 */
export function getOperatingTaskPendingCount() {
  return request<number>({
    url: '/operatingTask/pendingCount',
    method: 'get',
    silentError: true,
  })
}

/** 认领任务 */
export function claimOperatingTask(taskId: number | string) {
  return request({
    url: `/operatingTask/claim/${taskId}`,
    method: 'put',
  })
}

/** 完成任务 */
export function completeOperatingTask(taskId: number | string, handlerNote: string) {
  return request({
    url: `/operatingTask/complete/${taskId}`,
    method: 'put',
    data: { handlerNote },
  })
}

/** 驳回任务 */
export function rejectOperatingTask(taskId: number | string, rejectReason: string) {
  return request({
    url: `/operatingTask/reject/${taskId}`,
    method: 'put',
    data: { rejectReason },
  })
}

/** 重开任务 */
export function reopenOperatingTask(taskId: number | string, reason: string) {
  return request({
    url: `/operatingTask/reopen/${taskId}`,
    method: 'put',
    data: { reason },
  })
}
