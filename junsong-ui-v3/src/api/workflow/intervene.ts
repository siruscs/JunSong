import request from '@/utils/request'

export function jumpInstance(processInstanceId: string, targetActivityId: string) {
  return request({ url: `/workflow/instance/${processInstanceId}/jump`, method: 'post', params: { targetActivityId } })
}

export function suspendInstance(processInstanceId: string) {
  return request({ url: `/workflow/instance/${processInstanceId}/suspend`, method: 'post' })
}

export function activateInstance(processInstanceId: string) {
  return request({ url: `/workflow/instance/${processInstanceId}/activate`, method: 'post' })
}

export function getActivityHistory(processInstanceId: string) {
  return request({ url: `/workflow/instance/${processInstanceId}/activity-history`, method: 'get' })
}
