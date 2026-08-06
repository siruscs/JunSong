import request from '../request'

export function previewConfigSync(data: any) {
  return request({ url: '/member/config-sync/preview', method: 'post', data })
}

export function executeConfigSync(data: any) {
  return request({ url: '/member/config-sync/execute', method: 'post', data })
}

export function getConfigSyncBatch(batchId: number) {
  return request({ url: `/member/config-sync/${batchId}`, method: 'get' })
}
