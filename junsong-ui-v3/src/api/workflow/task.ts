import request from '../request'
import type { WorkflowInstanceRow } from './instance'
import { useUserStore } from '@/stores/user'

export interface WorkflowTodoTaskRow {
  taskId: string
  taskName?: string
  description?: string | null
  assignee?: string | null
  owner?: string | null
  createTime?: string | null
  dueDate?: string | null
  processInstanceId: string
  processDefinitionId?: string
  processDefinitionKey?: string
  businessKey?: string | null
}

export interface WorkflowDoneTaskRow {
  taskId: string
  taskName?: string
  assignee?: string | null
  startTime?: string | null
  endTime?: string | null
  durationMs?: number | null
  processInstanceId: string
  processDefinitionId?: string
  processDefinitionKey?: string
  businessKey?: string | null
  deleteReason?: string | null
}

export interface WorkflowAppliedTaskRow extends WorkflowInstanceRow {}

export interface WorkflowTaskDetail extends WorkflowTodoTaskRow {
  variables?: Record<string, any>
}

export interface WorkflowApprovePayload {
  comment?: string
  variables?: Record<string, any>
}

export interface WorkflowRejectPayload {
  comment?: string
}

function getWorkflowUserQuery() {
  const userStore = useUserStore()
  return userStore.name ? { user: userStore.name } : {}
}

export function listTodoWorkflowTasks() {
  return request({
    url: '/workflow/task/todo',
    method: 'get',
    params: getWorkflowUserQuery(),
  })
}

export function listDoneWorkflowTasks() {
  return request({
    url: '/workflow/task/done',
    method: 'get',
    params: getWorkflowUserQuery(),
  })
}

export function listAppliedWorkflowTasks() {
  return request({
    url: '/workflow/task/applied',
    method: 'get',
    params: getWorkflowUserQuery(),
  })
}

export function getWorkflowTaskDetail(taskId: string) {
  return request({
    url: `/workflow/task/${taskId}`,
    method: 'get',
  })
}

export function claimWorkflowTask(taskId: string) {
  return request({
    url: `/workflow/task/${taskId}/claim`,
    method: 'post',
  })
}

export function approveWorkflowTask(taskId: string, data?: WorkflowApprovePayload) {
  return request({
    url: `/workflow/task/${taskId}/approve`,
    method: 'post',
    data,
  })
}

export function rejectWorkflowTask(taskId: string, data?: WorkflowRejectPayload) {
  return request({
    url: `/workflow/task/${taskId}/reject`,
    method: 'post',
    data,
  })
}

export function transferWorkflowTask(taskId: string, toUser: string) {
  return request({
    url: `/workflow/task/${taskId}/transfer`,
    method: 'post',
    params: { toUser },
  })
}
