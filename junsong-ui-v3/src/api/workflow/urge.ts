import request from '@/utils/request'

export function urgeWorkflowTask(taskId: string, comment?: string) {
  return request({
    url: `/workflow/task/${taskId}/urge`,
    method: 'post',
    data: { comment },
  })
}
