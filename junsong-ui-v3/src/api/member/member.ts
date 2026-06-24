import request from '../request'

export function listMember(query: any) {
  return request({ url: '/member/member/list', method: 'get', params: query })
}

export function getMember(memberId: number) {
  return request({ url: '/member/member/' + memberId, method: 'get' })
}

export function addMember(data: any) {
  return request({ url: '/member/member', method: 'post', data })
}

export function updateMember(data: any) {
  return request({ url: '/member/member', method: 'put', data })
}

export function delMember(memberIds: string | number) {
  return request({ url: '/member/member/' + memberIds, method: 'delete' })
}

export function exportMember() {
  return request({ url: '/member/member/export', method: 'get', responseType: 'blob' })
}

export function getMemberByNo(memberNo: string) {
  return request({ url: '/member/member/no/' + memberNo, method: 'get' })
}

export function renewMember(data: any) {
  return request({
    url: '/member/member', method: 'put',
    data: { memberId: data.memberId, expireDate: data.newValidityDate, remark: data.remark }
  })
}

export function cancelMember(memberId: number) {
  return request({ url: '/member/member', method: 'put', data: { memberId, status: '2' } })
}

export function disableMember(memberId: number) {
  return request({ url: '/member/member', method: 'put', data: { memberId, status: '1' } })
}

export function invalidMember(memberId: number) {
  return disableMember(memberId)
}

export function getNextMemberNo(deptId: number) {
  return request({ url: '/member/member/nextNo', method: 'get', params: { deptId } })
}