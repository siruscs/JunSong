import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

function read(path) {
  assert.equal(existsSync(path), true, `${path} must exist`)
  return readFileSync(path, 'utf8')
}

test('store dashboard uses recent review and health capabilities', () => {
  const src = read('junsong-ui-v3/src/views/dashboard/StoreDashboard.vue')
  assert.match(src, /getDailyReviewBoard/, '店长首页必须接入每日复盘')
  assert.match(src, /getWeeklyReviewBoard/, '店长首页必须接入周复盘')
  assert.match(src, /getWeeklyMemo/, '店长首页必须接入周经营纪要')
  assert.match(src, /getAuthorizedStorePortfolio/, '主管首页必须接入授权多店健康矩阵')
  assert.match(src, /listReviewTasks/, '首页必须展示持久化复盘任务')
})

test('store dashboard first screen is an action workbench, not legacy member chart center', () => {
  const src = read('junsong-ui-v3/src/views/dashboard/StoreDashboard.vue')
  assert.match(src, /门店经营复盘工作台|经营复盘工作台/)
  assert.match(src, /今日待办|今日复盘|优先处理/)
  assert.match(src, /门店健康|健康分|风险门店/)
  assert.doesNotMatch(src, /WEB DATA VISUALIZATION/)
  assert.doesNotMatch(src, /会员积分排行/)
})

test('store dashboard keeps accounting cost summary cards for store managers', () => {
  const src = read('junsong-ui-v3/src/views/dashboard/StoreDashboard.vue')
  assert.match(src, /核算总览/)
  assert.match(src, /销售缴款/)
  assert.match(src, /已核销费用/)
  assert.match(src, /进货款/)
  assert.match(src, /借支未核销/)
  assert.match(src, /净利润/)
  assert.match(src, /盈亏平衡/)
})
