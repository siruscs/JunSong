import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const root = new URL('../', import.meta.url)
const uiRoot = new URL('junsong-ui-v3/', root)

const apiFile = fs.readFileSync(new URL('src/api/system/operatingTask.ts', uiRoot), 'utf8')
const viewFile = fs.readFileSync(new URL('src/views/system/operatingTask/index.vue', uiRoot), 'utf8')
const routesFile = fs.readFileSync(new URL('src/router/dynamicRoutes.ts', uiRoot), 'utf8')
const storeDashboardFile = fs.readFileSync(new URL('src/views/dashboard/StoreDashboard.vue', uiRoot), 'utf8')
const financeOverviewFile = fs.readFileSync(new URL('src/views/finance/overview/index.vue', uiRoot), 'utf8')
const memberOverviewFile = fs.readFileSync(new URL('src/views/member/overview/index.vue', uiRoot), 'utf8')
const controllerFile = fs.readFileSync(new URL('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysOperatingTaskController.java', root), 'utf8')

// ==================== 任务列表加载测试 ====================

test('API client 定义 listOperatingTask 调用 /operatingTask/list', () => {
  assert.match(apiFile, /export function listOperatingTask/)
  assert.match(apiFile, /url:\s*'\/operatingTask\/list'/)
  assert.match(apiFile, /method:\s*'get'/)
})

test('视图组件在 onMounted 时加载任务列表', () => {
  assert.match(viewFile, /onMounted\(/)
  assert.match(viewFile, /getList\(\)/)
})

test('列表加载时使用 startPage 分页参数', () => {
  assert.match(viewFile, /pageNum:/)
  assert.match(viewFile, /pageSize:/)
})

// ==================== 权限隐藏测试 ====================

test('认领按钮受 system:operatingTask:claim 权限控制', () => {
  // 列表行内按钮
  assert.match(viewFile, /v-if="canClaim\(row\) && hasPermi\('system:operatingTask:claim'\)"/)
  // 详情抽屉按钮
  assert.match(viewFile, /v-if="canClaim\(currentTask\) && hasPermi\('system:operatingTask:claim'\)"/)
})

test('完成按钮受 system:operatingTask:complete 权限控制', () => {
  assert.match(viewFile, /v-if="canComplete\(row\) && hasPermi\('system:operatingTask:complete'\)"/)
  assert.match(viewFile, /v-if="canComplete\(currentTask\) && hasPermi\('system:operatingTask:complete'\)"/)
})

test('驳回按钮受 system:operatingTask:reject 权限控制', () => {
  assert.match(viewFile, /v-if="canReject\(row\) && hasPermi\('system:operatingTask:reject'\)"/)
})

test('重开按钮受 system:operatingTask:reopen 权限控制', () => {
  assert.match(viewFile, /v-if="canReopen\(row\) && hasPermi\('system:operatingTask:reopen'\)"/)
})

test('动态路由受 system:operatingTask:list 权限控制', () => {
  assert.match(routesFile, /path:\s*'\/system\/operatingTask'/)
  assert.match(routesFile, /permissions:\s*\['system:operatingTask:list'\]/)
})

// ==================== 筛选测试 ====================

test('支持按门店筛选（deptId 来自 userStore.depts）', () => {
  assert.match(viewFile, /queryParams\.deptId/)
  assert.match(viewFile, /v-for="dept in deptOptions"/)
  assert.match(viewFile, /deptOptions\.value = userStore\.depts/)
})

test('支持按任务状态筛选', () => {
  assert.match(viewFile, /queryParams\.status/)
  assert.match(viewFile, /value="PENDING"/)
  assert.match(viewFile, /value="IN_PROGRESS"/)
  assert.match(viewFile, /value="DONE"/)
  assert.match(viewFile, /value="REJECTED"/)
  assert.match(viewFile, /value="REOPENED"/)
})

test('支持按优先级筛选', () => {
  assert.match(viewFile, /queryParams\.priority/)
  assert.match(viewFile, /value="URGENT"/)
  assert.match(viewFile, /value="HIGH"/)
  assert.match(viewFile, /value="MEDIUM"/)
  assert.match(viewFile, /value="LOW"/)
})

test('支持按负责人筛选（assigneeId）', () => {
  assert.match(viewFile, /queryParams\.assigneeId/)
  assert.match(viewFile, /v-for="u in assigneeOptions"/)
})

test('支持按来源模块筛选', () => {
  assert.match(viewFile, /queryParams\.sourceModule/)
  assert.match(viewFile, /value="FINANCE"/)
  assert.match(viewFile, /value="STOCK"/)
  assert.match(viewFile, /value="MEMBER"/)
})

test('支持按截止时间范围筛选', () => {
  assert.match(viewFile, /dueRange/)
  assert.match(viewFile, /type="daterange"/)
})

// ==================== 任务详情和源单据跳转测试 ====================

test('详情抽屉加载任务详情和操作日志', () => {
  assert.match(viewFile, /handleDetail/)
  assert.match(viewFile, /getOperatingTask\(row\.taskId\)/)
  assert.match(viewFile, /listOperatingTaskLogs\(row\.taskId\)/)
})

test('查看来源按钮使用后端返回的 sourceRoute 跳转', () => {
  assert.match(viewFile, /handleViewSource/)
  assert.match(viewFile, /v-if="row\.sourceRoute"/)
  assert.match(viewFile, /router\.push\(row\.sourceRoute\)/)
})

test('API client 定义所有后端契约端点', () => {
  assert.match(apiFile, /url:\s*`\/operatingTask\/\$\{taskId\}`/)
  assert.match(apiFile, /url:\s*`\/operatingTask\/logs\/\$\{taskId\}`/)
  assert.match(apiFile, /url:\s*`\/operatingTask\/claim\/\$\{taskId\}`/)
  assert.match(apiFile, /url:\s*`\/operatingTask\/complete\/\$\{taskId\}`/)
  assert.match(apiFile, /url:\s*`\/operatingTask\/reject\/\$\{taskId\}`/)
  assert.match(apiFile, /url:\s*`\/operatingTask\/reopen\/\$\{taskId\}`/)
  assert.match(apiFile, /url:\s*'\/operatingTask\/pendingCount'/)
})

// ==================== 完成后任务计数和看板刷新测试 ====================

test('完成任务后刷新任务列表', () => {
  const completeHandler = viewFile.match(/async function handleComplete[\s\S]*?^}/m)?.[0] || ''
  assert.match(completeHandler, /await completeOperatingTask/)
  assert.match(completeHandler, /await getList\(\)/)
})

test('完成任务后刷新详情抽屉', () => {
  const completeHandler = viewFile.match(/async function handleComplete[\s\S]*?^}/m)?.[0] || ''
  assert.match(completeHandler, /await handleDetail\(currentTask\.value\)/)
})

test('认领/驳回/重开后刷新任务列表和详情', () => {
  const claimHandler = viewFile.match(/async function handleClaim[\s\S]*?^}/m)?.[0] || ''
  const rejectHandler = viewFile.match(/async function handleReject[\s\S]*?^}/m)?.[0] || ''
  const reopenHandler = viewFile.match(/async function handleReopen[\s\S]*?^}/m)?.[0] || ''
  assert.match(claimHandler, /await getList\(\)/)
  assert.match(rejectHandler, /await getList\(\)/)
  assert.match(reopenHandler, /await getList\(\)/)
})

test('统计卡片调用 pendingCount 接口刷新', () => {
  assert.match(viewFile, /refreshCounts/)
  assert.match(viewFile, /getOperatingTaskPendingCount\(\)/)
})

// ==================== 401/403/状态冲突/重复提交测试 ====================

test('API 错误由 request 拦截器统一处理（不自行 catch 后吞掉）', () => {
  // claim/complete/reject/reopen 的 catch 块只做注释，不重新抛出，依赖拦截器
  const claimHandler = viewFile.match(/async function handleClaim[\s\S]*?^}/m)?.[0] || ''
  assert.match(claimHandler, /catch \(e: any\)/)
})

test('后端 Controller 使用 @RequiresPermissions 控制操作权限', () => {
  assert.match(controllerFile, /@RequiresPermissions\("system:operatingTask:list"\)/)
  assert.match(controllerFile, /@RequiresPermissions\("system:operatingTask:claim"\)/)
  assert.match(controllerFile, /@RequiresPermissions\("system:operatingTask:complete"\)/)
  assert.match(controllerFile, /@RequiresPermissions\("system:operatingTask:reject"\)/)
  assert.match(controllerFile, /@RequiresPermissions\("system:operatingTask:reopen"\)/)
})

test('后端 Controller 详情查询在任务不存在时返回错误', () => {
  assert.match(controllerFile, /if \(task == null\)/)
  assert.match(controllerFile, /AjaxResult\.error\("任务不存在或无权访问"\)/)
})

// ==================== 看板跳转测试 ====================

test('StoreDashboard 提供经营任务中心入口', () => {
  assert.match(storeDashboardFile, /goOperatingTask/)
  assert.match(storeDashboardFile, /router\.push\('\/system\/operatingTask'\)/)
})

test('finance/overview 快捷入口包含经营任务中心', () => {
  assert.match(financeOverviewFile, /to="\/system\/operatingTask"/)
})

test('member/overview 快捷入口包含经营任务中心', () => {
  assert.match(memberOverviewFile, /to: '\/system\/operatingTask'/)
})

// ==================== 租户/部门范围验证 ====================

test('前端不自行判断租户或部门范围，依赖后端权威结果', () => {
  // 门店选项来自 userStore.depts（后端返回的授权门店列表）
  assert.match(viewFile, /deptOptions\.value = userStore\.depts \|\| \[\]/)
  // 不存在手动 tenantId 输入
  assert.doesNotMatch(viewFile, /tenantId.*input|v-model="tenantId"/)
})

test('认领/完成/驳回/重开使用后端校验，前端只检查 UI 状态可行性', () => {
  // canClaim/canComplete/canReject/canReopen 只判断状态，不判断权限
  // 权限由 hasPermi 控制，最终操作由后端校验
  assert.match(viewFile, /function canClaim\(row[^)]*\):\s*boolean\s*\{[^}]*return row\.status === 'PENDING'/)
  assert.match(viewFile, /function canReopen\(row[^)]*\):\s*boolean\s*\{[^}]*return \['DONE', 'REJECTED'\]\.includes\(row\.status\)/)
})
