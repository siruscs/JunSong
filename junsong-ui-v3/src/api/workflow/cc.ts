import request from '@/utils/request'

export function ccWorkflowTask(taskId: string, toUsers: string[]) {
  return request({
    url: `/workflow/task/${taskId}/cc`,
    method: 'post',
    data: { toUsers },
  })
}
