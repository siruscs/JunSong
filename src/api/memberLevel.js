import { request } from './index.js'

export function listMemberLevels(params = {}) {
  return request({ url: '/member/level/list', method: 'GET', data: params })
}

export function getMemberLevel(typeCode) {
  return request({ url: `/member/level/${encodeURIComponent(typeCode)}`, method: 'GET' })
}

export function createMemberLevel(data) {
  return request({ url: '/member/level', method: 'POST', data })
}

export function updateMemberLevel(data) {
  return request({ url: '/member/level', method: 'PUT', data })
}
