import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('refreshWorkContext hydrates the complete department set from system user info', () => {
  const api = read('src/api/index.js')

  assert.match(api, /export async function refreshWorkContext\(/)
  assert.match(api, /url: '\/system\/user\/getInfo'/)
  assert.match(api, /import \{[^}]*resolveDeptCollection[^}]*\} from '@\/utils\/workContext\.js'/)
  assert.match(api, /const depts = resolveDeptCollection\(info, storedUser\)/)
  assert.match(api, /storedUser\.currentDeptId \?\? storedUser\.deptId/)
  assert.match(api, /workContext\.hydrate\(\{[\s\S]*?user:[\s\S]*?depts:[\s\S]*?currentDeptId:/)
  assert.match(api, /url: '\/system\/user\/getInfo',[\s\S]*?withContextMeta:\s*true/)
  assert.match(api, /const snapshot = workContext\.hydrate\(/)
  assert.match(api, /mergePersistedUser\(storedUser,\s*user,\s*snapshot\)/)
  assert.match(api, /uni\.setStorageSync\('userInfo', persistedUser\)/)
})

test('refreshWorkContext ignores a stale response before parsing or writing user state', () => {
  const api = read('src/api/index.js')
  const refreshBlock = api.match(/export async function refreshWorkContext\(options = \{\}\) \{([\s\S]*?)\n\}\n\nexport function listData/)

  assert.ok(refreshBlock, 'refreshWorkContext should exist')
  const body = refreshBlock[1]
  const staleGuard = body.indexOf('if (response?.contextMeta?.staleContext)')
  const snapshotReturn = body.indexOf('return workContext.snapshot()', staleGuard)
  const storageWrite = body.indexOf("uni.setStorageSync('userInfo'")
  const hydrate = body.indexOf('workContext.hydrate(')

  assert.ok(staleGuard >= 0, 'stale response should be detected')
  assert.ok(snapshotReturn > staleGuard, 'stale response should return the current snapshot')
  assert.ok(snapshotReturn < storageWrite, 'stale response must not write storage')
  assert.ok(snapshotReturn < hydrate, 'stale response must not hydrate work context')
  assert.match(body, /const \{ contextMeta: _contextMeta, \.\.\.payload \} = response \|\| \{\}/)
  assert.match(body, /const info = payload\?\.data/)
})

test('all successful login paths converge on completeLogin and refreshWorkContext', () => {
  const login = read('src/pages/login/index.vue')

  assert.match(login, /import \{[^}]*refreshWorkContext[^}]*\} from '@\/api\/index\.js'/)
  assert.match(login, /async completeLogin\(\)[\s\S]*?await refreshWorkContext\(\)/)
  assert.match(login, /handleWechatLogin\(\)[\s\S]*?await this\.completeLogin\(\)/)
  assert.match(login, /handleLogin\(\)[\s\S]*?await this\.completeLogin\(\)/)
})

test('completeLogin preserves mini-program modules and permissions while hydrating departments', () => {
  const login = read('src/pages/login/index.vue')
  const completeLogin = login.match(/async completeLogin\(\) \{([\s\S]*?)\n    \},\n    async handleLogin/)

  assert.ok(completeLogin, 'completeLogin method should exist')
  assert.match(completeLogin[1], /url: '\/member\/mp\/userinfo'/)
  assert.match(completeLogin[1], /uni\.setStorageSync\('modules', persistedUser\.modules \|\| \[\]\)/)
  assert.match(completeLogin[1], /uni\.setStorageSync\('permissions', persistedUser\.permissions \|\| \[\]\)/)
  assert.match(completeLogin[1], /await refreshWorkContext\(\)/)
  assert.match(completeLogin[1], /const storedUser = uni\.getStorageSync\('userInfo'\) \|\| \{\}/)
  assert.match(completeLogin[1], /mergePersistedUser\(storedUser, userInfo\)/)
  assert.doesNotMatch(completeLogin[1], /workContext\.hydrate\([\s\S]*?depts:\s*\[\]/)
})

test('password login stores the complete department collection before member user info loads', () => {
  const login = read('src/pages/login/index.vue')
  const handleLogin = login.match(/async handleLogin\(\) \{([\s\S]*?)\n    \},\n    goSettings/)

  assert.ok(handleLogin, 'handleLogin method should exist')
  assert.match(handleLogin[1], /const baseInfo = \{[\s\S]*?depts:\s*deptList/)
})

test('app foreground uses the shared foreground session coordinator', () => {
  const app = read('src/App.vue')

  assert.match(app, /import \{ refreshForegroundSession \} from '@\/utils\/foregroundSession\.js'/)
  assert.match(app, /onShow\(\) \{[\s\S]*?refreshForegroundSession\(\)\.catch\(\(\) => \{\}\)/)
  assert.doesNotMatch(app, /refreshAuthSession|refreshWorkContext/)
})
