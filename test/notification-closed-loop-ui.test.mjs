import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const page = fs.readFileSync(new URL('../src/pages/notification/index.vue', import.meta.url), 'utf8')

test('notification page uses backend read and type fields', () => {
  const unread = page.match(/isUnread\(item\) \{([\s\S]*?)(?=\n    noticeTitle)/)?.[1] || ''
  assert.match(unread, /item\.isRead/)
  const typeText = page.match(/noticeTypeText\(item\) \{([\s\S]*?)(?=\n    noticeTypeClass)/)?.[1] || ''
  assert.match(typeText, /wf_todo/)
  assert.match(typeText, /wf_finished/)
  assert.match(typeText, /wf_rejected/)
})

test('notification targets are capability aware, allowlisted, and opened after read handling', () => {
  assert.match(page, /resolveNotificationTarget/)
  assert.match(page, /hasModulePermission\('wfTodo'\)/)
  assert.match(page, /hasModulePermission\('wfDone'\)/)
  assert.match(page, /hasExactPermission\('workflow:task:list'\)/)
  assert.match(page, /hasModulePermission\('expense'\)/)
  assert.match(page, /hasExactPermission\('finance:expense:list'\)/)
  const openNotice = page.match(/async openNotice\(item\) \{([\s\S]*?)(?=\n    handleMarkAllRead)/)?.[1] || ''
  assert.match(openNotice, /await markRead\(item\.id\)/)
  assert.match(openNotice, /item\.isRead = '1'/)
  assert.match(openNotice, /this\.markingReadIds\[item\.id\]/)
  assert.match(openNotice, /delete this\.markingReadIds\[item\.id\]/)
  assert.doesNotMatch(openNotice, /item\.taskId \|\| item\.businessId/)
  assert.match(openNotice, /uni\.navigateTo\(\{ url: target \}\)/)
  assert.match(openNotice, /暂无可打开的移动端页面/)
})

test('notification list distinguishes failure and protects pagination from stale responses', () => {
  assert.match(page, /class="load-error"/)
  assert.match(page, /@tap="refresh"/)
  assert.match(page, /loadError:/)
  assert.match(page, /requestVersion:/)
  const loadMore = page.match(/async loadMore\(\) \{([\s\S]*?)(?=\n    async fetchList)/)?.[1] || ''
  assert.match(loadMore, /const previousPage = this\.pageNum/)
  assert.match(loadMore, /if \(!loaded && requestVersion === this\.requestVersion\) this\.pageNum = previousPage/)
  const fetchList = page.match(/async fetchList\(reset\) \{([\s\S]*?)(?=\n    async loadUnreadCount)/)?.[1] || ''
  assert.match(fetchList, /const requestVersion = \+\+this\.requestVersion/)
  assert.match(fetchList, /if \(requestVersion !== this\.requestVersion\) return false/)
  assert.match(fetchList, /this\.loadError =/)
  assert.doesNotMatch(fetchList, /console\.(error|log)/)
})
