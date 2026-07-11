import request from '../request'

// 查询签到记录列表
export function listSignIn(query: any) {
  return request({ url: '/member/signIn/list', method: 'get', params: query })
}

// 会员签到
export function doSignIn(data: any) {
  return request({ url: '/member/signIn', method: 'post', data })
}

// 查询今日签到状态
export function getTodaySignIn(memberId: number) {
  return request({ url: '/member/signIn/today', method: 'get', params: { memberId } })
}

// 查询签到日历
export function getSignInCalendar(memberId: number, month: string) {
  return request({ url: '/member/signIn/calendar', method: 'get', params: { memberId, month } })
}

// 批量补录签到
export function backfillSignIn(data: any) {
  return request({ url: '/member/signIn/backfill', method: 'post', data })
}

// 删除签到/补签到记录
export function delSignIn(signIds: number | string) {
  return request({ url: '/member/signIn/' + signIds, method: 'delete' })
}

// 签到预览：查询会员当前等级名称、单次签到积分、单次签到成长值
export function previewSignIn(memberId: number) {
  return request({ url: '/member/signIn/preview', method: 'get', params: { memberId } })
}
