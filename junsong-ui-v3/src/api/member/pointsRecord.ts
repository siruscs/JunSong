import request from '../request'

export function listPointsRecord(query: any) {
  return request({ url: '/member/pointsRecord/list', method: 'get', params: query })
}

export function getPointsRecord(recordId: number) {
  return request({ url: '/member/pointsRecord/' + recordId, method: 'get' })
}

export function addPointsRecord(data: any) {
  return request({ url: '/member/pointsRecord', method: 'post', data })
}

export function updatePointsRecord(data: any) {
  return request({ url: '/member/pointsRecord', method: 'put', data })
}

export function delPointsRecord(recordIds: string | number) {
  return request({ url: '/member/pointsRecord/' + recordIds, method: 'delete' })
}

export function exportPointsRecord() {
  return request({ url: '/member/pointsRecord/export', method: 'get', responseType: 'blob' })
}

export function getMemberPointsDetail(memberId: number) {
  return request({ url: '/member/pointsRecord/member/' + memberId + '/detail', method: 'get' })
}