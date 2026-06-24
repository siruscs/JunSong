import request from '../request'

export function listTable(query: any) {
  return request({ url: '/code/gen/list', method: 'get', params: query })
}

export function previewTable(tableId: number) {
  return request({ url: '/code/gen/preview/' + tableId, method: 'get' })
}

export function delTable(tableId: number | string) {
  return request({ url: '/code/gen/' + tableId, method: 'delete' })
}

export function genCode(tableName: string) {
  return request({ url: '/code/gen/genCode/' + tableName, method: 'get' })
}

export function synchDb(tableName: string) {
  return request({ url: '/code/gen/synchDb/' + tableName, method: 'get' })
}
