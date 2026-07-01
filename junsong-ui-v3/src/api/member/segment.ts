import request from '../request'

export interface MemberSegmentQuery {
  deptId?: number
  segmentType?: string
  beginTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}

export function getSegmentList(data: MemberSegmentQuery) {
  return request({
    url: '/member/segment/list',
    method: 'post',
    data,
  })
}
