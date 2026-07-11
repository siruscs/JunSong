import request from '@/utils/request'

/**
 * 获取每日经营复盘看板
 * R8-A: 老板/店长每天打开首页后的行动清单
 */
export function getDailyReviewBoard(data: any) {
  return request({
    url: '/finance/daily-review/board',
    method: 'post',
    data
  })
}

/**
 * 获取每周经营复盘看板
 * R8-F: 周复盘摘要，不做复杂 BI
 */
export function getWeeklyReviewBoard(data: any) {
  return request({
    url: '/finance/daily-review/weekly-board',
    method: 'post',
    data
  })
}

/**
 * R10-F: 获取周经营纪要
 */
export function getWeeklyMemo(data: any) {
  return request({
    url: '/finance/daily-review/weekly-memo',
    method: 'post',
    data
  })
}
