import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const page = fs.readFileSync(new URL('../src/pages/operating-task/index.vue', import.meta.url), 'utf8')
const workbench = fs.readFileSync(new URL('../src/pages/workbench/index.vue', import.meta.url), 'utf8')
const home = fs.readFileSync(new URL('../src/pages/index/index.vue', import.meta.url), 'utf8')

// 1. 小程序任务列表加载
test('operating task page loads list from backend with pagination', () => {
  assert.match(page, /import \{ request \} from '@\/api\/index\.js'/)
  assert.match(page, /url: '\/operatingTask\/list'/)
  assert.match(page, /method: 'GET'/)
  assert.match(page, /pageNum:/)
  assert.match(page, /pageSize:/)
  // 处理返回数据 rows 和 total
  assert.match(page, /data\.rows \|\| \[\]/)
  assert.match(page, /data\.total \|\| 0/)
  // 分页加载更多
  assert.match(page, /loadMore\(\)/)
  assert.match(page, /this\.pageNum \+= 1/)
  // 第一页替换，后续页追加
  assert.match(page, /if \(this\.pageNum === 1\)/)
  assert.match(page, /this\.rows = \[\.\.\.this\.rows, \.\.\.rows\]/)
})

// 2. 当前部门切换后任务重新加载
test('reloads tasks after department context changes', () => {
  assert.match(page, /import \{ workContext \} from '@\/utils\/workContext\.js'/)
  // onLoad 记录初始版本
  assert.match(page, /onLoad\(\)/)
  assert.match(page, /this\.contextVersion = workContext\.captureVersion\(\)/)
  // onShow 检测版本变化后刷新
  assert.match(page, /onShow\(\)/)
  assert.match(page, /const currentVersion = workContext\.captureVersion\(\)/)
  assert.match(page, /if \(this\.contextVersion !== null && this\.contextVersion !== currentVersion\)/)
  assert.match(page, /this\.refresh\(\)/)
  // buildQuery 使用当前部门
  assert.match(page, /const snap = workContext\.snapshot\(\)/)
  assert.match(page, /deptId: snap\.currentDeptId \|\| undefined/)
})

// 3. 任务权限隐藏（后端权限是权威，不能仅靠隐藏按钮）
test('action buttons are gated by exact permissions from backend', () => {
  assert.match(page, /import \{ hasExactPermission \} from '@\/utils\/permission\.js'/)
  // 四个操作权限
  assert.match(page, /hasExactPermission\('system:operatingTask:claim'\)/)
  assert.match(page, /hasExactPermission\('system:operatingTask:complete'\)/)
  assert.match(page, /hasExactPermission\('system:operatingTask:reject'\)/)
  assert.match(page, /hasExactPermission\('system:operatingTask:reopen'\)/)
  // 按钮同时检查状态条件和权限
  assert.match(page, /v-if="canClaim\(currentTask\) && hasClaimPerm"/)
  assert.match(page, /v-if="canComplete\(currentTask\) && hasCompletePerm"/)
  assert.match(page, /v-if="canReject\(currentTask\) && hasRejectPerm"/)
  assert.match(page, /v-if="canReopen\(currentTask\) && hasReopenPerm"/)
})

// 4. 认领/完成/驳回/重开按钮状态
test('action availability follows task status state machine', () => {
  // 认领：仅 PENDING
  const canClaim = page.match(/canClaim\(task\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(canClaim, /task\?\.status === 'PENDING'/)
  // 完成：IN_PROGRESS 或 REOPENED
  const canComplete = page.match(/canComplete\(task\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(canComplete, /\['IN_PROGRESS', 'REOPENED'\]\.includes\(task\?\.status\)/)
  // 驳回：IN_PROGRESS 或 REOPENED
  const canReject = page.match(/canReject\(task\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(canReject, /\['IN_PROGRESS', 'REOPENED'\]\.includes\(task\?\.status\)/)
  // 重开：DONE 或 REJECTED
  const canReopen = page.match(/canReopen\(task\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(canReopen, /\['DONE', 'REJECTED'\]\.includes\(task\?\.status\)/)
})

// 5. 重复提交保护
test('all task actions guard against duplicate submission', () => {
  assert.match(page, /submitting: false/)
  // 每个操作方法都有 submitting 守卫
  const claimBlock = page.match(/async handleClaim\(\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(claimBlock, /if \(this\.submitting\) return/)
  assert.match(claimBlock, /this\.submitting = true/)
  assert.match(claimBlock, /finally \{[\s\S]*?this\.submitting = false/)

  const completeBlock = page.match(/async handleComplete\(\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(completeBlock, /if \(this\.submitting\) return/)
  assert.match(completeBlock, /this\.submitting = true/)
  assert.match(completeBlock, /finally \{[\s\S]*?this\.submitting = false/)

  const rejectBlock = page.match(/async handleReject\(\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(rejectBlock, /if \(this\.submitting\) return/)
  assert.match(rejectBlock, /this\.submitting = true/)
  assert.match(rejectBlock, /finally \{[\s\S]*?this\.submitting = false/)

  const reopenBlock = page.match(/async handleReopen\(\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(reopenBlock, /if \(this\.submitting\) return/)
  assert.match(reopenBlock, /this\.submitting = true/)
  assert.match(reopenBlock, /finally \{[\s\S]*?this\.submitting = false/)

  // 按钮在提交时禁用
  assert.match(page, /:disabled="submitting"/)
})

// 6. 401 自动恢复登录（由 authSession 处理，页面不重复处理）
test('401 auth expiry is delegated to authSession and not handled by the page', () => {
  const errorBlock = page.match(/handleActionError\(e\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  // 401 直接 return，不显示 toast（由 request 层 authSession.recoverOnce 处理）
  assert.match(errorBlock, /if \(code === 401\) return/)
})

// 7. 403 权限提示
test('403 shows a clear permission denied toast', () => {
  const errorBlock = page.match(/handleActionError\(e\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(errorBlock, /if \(code === 403\)/)
  assert.match(errorBlock, /暂无操作权限/)
  assert.match(errorBlock, /icon: 'none'/)
})

// 8. 状态冲突后的刷新
test('status conflict prompts user to refresh the list', () => {
  const errorBlock = page.match(/handleActionError\(e\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  // 检测状态冲突：消息包含"状态"/"已变更"或 code 409
  assert.match(errorBlock, /msg\.includes\('状态'\) \|\| msg\.includes\('已变更'\) \|\| code === 409/)
  // 弹出模态框提供刷新选项
  assert.match(errorBlock, /uni\.showModal/)
  assert.match(errorBlock, /title: '任务状态冲突'/)
  assert.match(errorBlock, /confirmText: '刷新'/)
  assert.match(errorBlock, /if \(res\.confirm\) this\.refresh\(\)/)
})

// 11. 不同角色的工作台快捷入口
test('workbench shows operating task entry only for authorized users', () => {
  assert.match(workbench, /import \{[^}]*hasExactPermission[^}]*\} from '@\/utils\/permission\.js'/)
  assert.match(workbench, /canViewOperatingTask\(\)/)
  assert.match(workbench, /return hasExactPermission\('system:operatingTask:list'\)/)
  assert.match(workbench, /v-if="canViewOperatingTask/)
  assert.match(workbench, /openOperatingTask\(\)/)
  assert.match(workbench, /uni\.navigateTo\(\{ url: '\/pages\/operating-task\/index' \}\)/)
})

test('home page shows operating task entry with pending count badge', () => {
  assert.match(home, /canViewOperatingTask\(\)/)
  assert.match(home, /return hasExactPermission\('system:operatingTask:list'\)/)
  assert.match(home, /v-if="canViewOperatingTask"/)
  assert.match(home, /operatingTaskCount/)
  assert.match(home, /v-if="operatingTaskCount > 0"/)
  assert.match(home, /loadOperatingTaskCount\(\)/)
  assert.match(home, /url: '\/operatingTask\/pendingCount'/)
  assert.match(home, /openOperatingTask\(\)/)
  assert.match(home, /uni\.navigateTo\(\{ url: '\/pages\/operating-task\/index' \}\)/)
})

test('home page role-based view uses deriveWorkView from departments and modules', () => {
  assert.match(home, /import \{ deriveWorkView \} from '@\/utils\/workView\.js'/)
  assert.match(home, /workView\(\)/)
  assert.match(home, /workContext\.snapshot\(\)/)
  assert.match(home, /deriveWorkView\(\{/)
  assert.match(home, /depts: context\.depts/)
  assert.match(home, /modules: authorizedModules/)
  assert.match(home, /\{\{ workView\.label \}\}/)
})

test('workbench operating task entry checks permission before navigation', () => {
  const block = workbench.match(/openOperatingTask\(\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(block, /if \(!this\.canViewOperatingTask\)/)
  assert.match(block, /暂无该功能权限/)
})
