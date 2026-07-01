import request from '@/utils/request'

export function addSignWorkflowTask(taskId: string, data: { addSignUser: string; type: string }) {
  return request({
    url: `/workflow/task/${taskId}/addsign`,
    method: 'post',
    data,
  })
}
