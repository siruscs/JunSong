import request from '@/utils/request'

export function getDefinitionVersions(processKey: string) {
  return request({ url: `/workflow/definition/${processKey}/versions`, method: 'get' })
}

export function suspendDefinitionVersion(definitionId: string) {
  return request({ url: `/workflow/definition/${definitionId}/suspend`, method: 'post' })
}

export function activateDefinitionVersion(definitionId: string) {
  return request({ url: `/workflow/definition/${definitionId}/activate`, method: 'post' })
}
