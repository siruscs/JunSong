import request from '../request'

export function listTable(query: any) {
  return request({ url: '/code/gen/list', method: 'get', params: query })
}

export function getGenTable(tableId: number) {
  return request({ url: '/code/gen/' + tableId, method: 'get' })
}

export function updateGenTable(data: any) {
  return request({ url: '/code/gen', method: 'put', data })
}

export function delTable(tableId: number | string) {
  return request({ url: '/code/gen/' + tableId, method: 'delete' })
}

export function previewTable(tableId: number) {
  return request({ url: '/code/gen/preview/' + tableId, method: 'get' })
}

export function listDbTable(query: any) {
  return request({ url: '/code/gen/db/list', method: 'get', params: query })
}

export function importTable(tables: string, tplWebType: string) {
  return request({ url: '/code/gen/importTable', method: 'post', params: { tables, tplWebType } })
}

export function columnList(tableId: number) {
  return request({ url: '/code/gen/column/' + tableId, method: 'get' })
}

export function downloadCode(tableName: string) {
  return request({ url: '/code/gen/download/' + tableName, method: 'get', responseType: 'blob' })
}

export function batchGenCode(tables: string) {
  return request({ url: '/code/gen/batchGenCode', method: 'get', params: { tables }, responseType: 'blob' })
}

export function genCode(tableName: string) {
  return request({ url: '/code/gen/genCode/' + tableName, method: 'get' })
}

export function synchDb(tableName: string) {
  return request({ url: '/code/gen/synchDb/' + tableName, method: 'get' })
}
