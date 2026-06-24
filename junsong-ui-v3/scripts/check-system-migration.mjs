import { readFileSync, existsSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(new URL('..', import.meta.url).pathname)

function read(path) {
  return readFileSync(resolve(root, path), 'utf8')
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

const dynamicRoutes = read('src/router/dynamicRoutes.ts')
assert(
  dynamicRoutes.includes("@/views/system/user/authRole.vue"),
  'user-auth route must load the migrated AuthRole page',
)
assert(
  dynamicRoutes.includes("@/views/system/role/authUser.vue"),
  'role-auth route must load the migrated AuthUser page',
)
assert(
  dynamicRoutes.includes("@/views/system/dict/data.vue"),
  'dict-data route must load the migrated dict data page',
)

const userApi = read('src/api/system/user.ts')
assert(
  userApi.includes("url: '/system/user/switchDept/' + deptId") && userApi.includes("method: 'post'"),
  'switchDept must call POST /system/user/switchDept/{deptId}',
)
assert(
  userApi.includes("url: '/userDept/user/' + userId"),
  'getUserDeptByUserId must use backend /userDept path',
)

const userDeptApi = read('src/api/system/userDept.ts')
for (const expected of [
  "url: '/userDept/list'",
  "url: '/userDept/user/' + userId",
  "url: '/userDept/' + userDeptId",
  "url: '/userDept'",
  "url: '/userDept/' + userDeptIds",
  "url: '/userDept/hire'",
  "url: '/userDept/leave'",
  "url: '/userDept/staff/' + deptId",
]) {
  assert(userDeptApi.includes(expected), `userDept API missing ${expected}`)
}

const expectedViews = [
  'src/views/system/user/authRole.vue',
  'src/views/system/role/authUser.vue',
  'src/views/system/role/selectUser.vue',
  'src/views/system/dict/data.vue',
  'src/views/system/notice/ReadUsers.vue',
]
for (const view of expectedViews) {
  assert(existsSync(resolve(root, view)), `${view} must exist`)
}

const roleView = read('src/views/system/role/index.vue')
assert(
  roleView.includes('@selection-change="handleUnallocatedSelectionChange"'),
  'role unallocated user table must update selected users',
)

const noticeView = read('src/views/system/notice/index.vue')
assert(
  noticeView.includes('handleReadUsers') && noticeView.includes('readUsersDialog.visible'),
  'notice page must include read user dialog behavior',
)

const userView = read('src/views/system/user/index.vue')
assert(userView.includes('handleImport'), 'user page must include import behavior')
assert(userView.includes('handleViewData'), 'user page must include detail behavior')
assert(userView.includes('handleStatusChange'), 'user page must include status switch behavior')
assert(userView.includes('ExcelImportDialog'), 'user page must use the migrated ExcelImportDialog component')
assert(existsSync(resolve(root, 'src/views/system/user/view.vue')), 'user detail drawer must exist')
assert(existsSync(resolve(root, 'src/components/ExcelImportDialog/index.vue')), 'ExcelImportDialog must exist')

const roleIndexView = read('src/views/system/role/index.vue')
assert(roleIndexView.includes('handleStatusChange'), 'role page must include status switch behavior')

const logininforView = read('src/views/monitor/log/logininfor/index.vue')
assert(logininforView.includes('handleUnlock'), 'login log page must include unlock behavior')
assert(logininforView.includes('handleExport'), 'login log page must include export behavior')

const operlogView = read('src/views/monitor/log/operlog/index.vue')
assert(operlogView.includes('handleView'), 'operation log page must include detail behavior')
assert(operlogView.includes('handleExport'), 'operation log page must include export behavior')

console.log('system migration checks passed')
