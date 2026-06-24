import request from '../request'

export interface WorkflowHistoryInstanceRow {
  processInstanceId: string
  processDefinitionId?: string
  processDefinitionKey?: string
  processDefinitionName?: string
  businessKey?: string | null
  startTime?: string | null
  endTime?: string | null
  durationMs?: number | null
  initiator?: string | null
  running?: boolean
}

export interface WorkflowHistoryActivityRow {
  activityId: string
  activityName?: string | null
  activityType?: string | null
  assignee?: string | null
  startTime?: string | null
  endTime?: string | null
  durationMs?: number | null
}

export interface WorkflowHistoryCommentRow {
  id: string
  type?: string | null
  userId?: string | null
  time?: string | null
  message?: string | null
}

export interface WorkflowHistoryInstanceQuery {
  processKey?: string
  limit?: number
}

export function listWorkflowHistoryInstances(params?: WorkflowHistoryInstanceQuery) {
  return request({
    url: '/workflow/history/instances',
    method: 'get',
    params,
  })
}

export function listWorkflowHistoryActivities(processInstanceId: string) {
  return request({
    url: `/workflow/history/instance/${processInstanceId}/activities`,
    method: 'get',
  })
}

export function listWorkflowHistoryComments(processInstanceId: string) {
  return request({
    url: `/workflow/history/instance/${processInstanceId}/comments`,
    method: 'get',
  })
}
