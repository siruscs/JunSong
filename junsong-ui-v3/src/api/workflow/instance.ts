import request from '../request'

export interface WorkflowInstanceRow {
  processInstanceId: string
  processDefinitionId?: string
  processDefinitionKey?: string
  processDefinitionName?: string
  businessKey?: string | null
  startTime?: string | null
  endTime?: string | null
  startUserId?: string | null
  suspended?: boolean
  running?: boolean
  durationMs?: number | null
}

export interface WorkflowInstanceDetail {
  running: boolean
  instance: WorkflowInstanceRow
}

export interface WorkflowRunningTask {
  taskId: string
  taskName?: string
  description?: string | null
  assignee?: string | null
  createTime?: string | null
  dueDate?: string | null
}

export interface WorkflowInstanceQuery {
  processKey?: string
  businessKey?: string
}

export interface StartWorkflowInstancePayload {
  processKey: string
  businessKey?: string
  variables?: Record<string, any>
}

export function listWorkflowInstances(params?: WorkflowInstanceQuery) {
  return request({
    url: '/workflow/instance/list',
    method: 'get',
    params,
  })
}

export function getWorkflowInstanceDetail(processInstanceId: string) {
  return request({
    url: `/workflow/instance/${processInstanceId}`,
    method: 'get',
  })
}

export function terminateWorkflowInstance(processInstanceId: string, reason?: string) {
  return request({
    url: `/workflow/instance/${processInstanceId}/terminate`,
    method: 'post',
    params: reason ? { reason } : undefined,
  })
}

export function listWorkflowRunningTasks(processInstanceId: string) {
  return request({
    url: '/workflow/instance/tasks/running',
    method: 'get',
    params: { processInstanceId },
  })
}

export function startWorkflowInstance(data: StartWorkflowInstancePayload) {
  return request({
    url: '/workflow/instance/start',
    method: 'post',
    data,
  })
}
