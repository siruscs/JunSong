import request from '@/api/request'

export interface WorkflowDefinitionSummary {
  definitionId: string
  processKey: string
  processName: string
  version: number
  suspended: boolean
  deploymentId: string
  resourceName: string
  deploymentTime: string | null
  category?: string | null
}

export interface WorkflowDefinitionXmlDetail {
  definitionId: string
  processKey: string
  processName: string
  version: number
  xml: string
}

export interface WorkflowDefinitionValidationResult {
  valid: boolean
  validationMessages: string[]
  processKey: string
  processName: string
}

export interface WorkflowDefinitionDeployResult {
  deploymentId: string
  definitionId: string
  processKey: string
  processName: string
  version: number
}

export interface WorkflowDefinitionQuery {
  key?: string
  keyword?: string
  category?: string
  latestOnly?: boolean
}

export interface WorkflowDefinitionPayload {
  xml: string
  processKey: string
  processName: string
}

export interface WorkflowDefinitionDeployPayload extends WorkflowDefinitionPayload {
  deploymentName?: string
}

export function listWorkflowDefinitions(params?: WorkflowDefinitionQuery) {
  return request({
    url: '/workflow/definition/list',
    method: 'get',
    params,
  })
}

export function listWorkflowCategories() {
  return request({
    url: '/workflow/definition/categories',
    method: 'get',
  })
}

export function getWorkflowDefinitionDetail(definitionId: string) {
  return request({
    url: `/workflow/definition/${definitionId}`,
    method: 'get',
  })
}

export function getWorkflowDefinitionXml(definitionId: string) {
  return request({
    url: `/workflow/definition/${definitionId}/xml`,
    method: 'get',
  })
}

export function validateWorkflowDefinition(data: WorkflowDefinitionPayload) {
  return request({
    url: '/workflow/definition/validate',
    method: 'post',
    data,
  })
}

export function deployWorkflowDefinition(data: WorkflowDefinitionDeployPayload) {
  return request({
    url: '/workflow/definition/deploy',
    method: 'post',
    data,
  })
}

export function suspendWorkflowDefinition(definitionId: string) {
  return request({
    url: `/workflow/definition/${definitionId}/suspend`,
    method: 'post',
  })
}

export function activateWorkflowDefinition(definitionId: string) {
  return request({
    url: `/workflow/definition/${definitionId}/activate`,
    method: 'post',
  })
}

export function deleteWorkflowDeployment(deploymentId: string, cascade = false) {
  return request({
    url: `/workflow/definition/deployment/${deploymentId}`,
    method: 'delete',
    params: { cascade },
  })
}
