import request from '@/utils/request'

export function withdrawWorkflowInstance(processInstanceId: string) {
  return request({
    url: `/workflow/instance/${processInstanceId}/withdraw`,
    method: 'post',
  })
}
