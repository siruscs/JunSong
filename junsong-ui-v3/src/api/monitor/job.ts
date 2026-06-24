import request from '../request'

export function listJob(query: any) {
  return request({ url: '/schedule/job/list', method: 'get', params: query })
}

export function getJob(jobId: number) {
  return request({ url: '/schedule/job/' + jobId, method: 'get' })
}

export function addJob(data: any) {
  return request({ url: '/schedule/job', method: 'post', data })
}

export function updateJob(data: any) {
  return request({ url: '/schedule/job', method: 'put', data })
}

export function delJob(jobId: number) {
  return request({ url: '/schedule/job/' + jobId, method: 'delete' })
}

export function runJob(jobId: number, jobGroup: string) {
  return request({ url: '/schedule/job/run', method: 'put', data: { jobId, jobGroup } })
}

export function changeJobStatus(jobId: number, status: string) {
  return request({ url: '/schedule/job/changeStatus', method: 'put', data: { jobId, status } })
}

export function listJobLog(query: any) {
  return request({ url: '/schedule/job/log/list', method: 'get', params: query })
}

export function delJobLog(jobLogId: number) {
  return request({ url: '/schedule/job/log/' + jobLogId, method: 'delete' })
}

export function cleanJobLog() {
  return request({ url: '/schedule/job/log/clean', method: 'delete' })
}
