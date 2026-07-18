# Mini-Program Session and Work Context Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a testable session and department work-context foundation so authentication expiry always enters one recovery flow and every page/request uses the same current department.

**Architecture:** Extract pure session, work-context, and request-error rules from page components into focused utilities. Keep `src/api/index.js` as the uni-app transport adapter, make `App.vue` coordinate foreground validation, and migrate the home page from private department state to the shared context without rewriting its dashboard UI.

**Tech Stack:** uni-app, Vue 3 Options API, JavaScript ES modules, WeChat Mini Program APIs, Node.js built-in test runner.

---

## Scope and Dependency Order

This is implementation package 1 of the approved evolution design. It delivers working software independently and must finish before the following packages begin:

1. `session-work-context` — this plan.
2. `role-home-workbench` — role-aware home, search, favorites, and recent modules.
3. `high-frequency-flow-templates` — member, sale, expense, and verification list/detail/form patterns.
4. `task-center-cross-dept` — unified tasks, notifications, and cross-department processing.
5. `operations-insight` — explainable alerts, comparisons, and adoption metrics.

Package 1 does not redesign page visuals or migrate every business page. It establishes the shared interfaces those packages consume.

## File Structure

- Create `junsong-miniprogram/src/utils/authSession.js`: pure authentication-expiry detection and recovery gate.
- Create `junsong-miniprogram/src/utils/workContext.js`: normalized user/departments/current-department state with a monotonic version.
- Create `junsong-miniprogram/src/utils/requestPolicy.js`: pure error classification and retry policy.
- Create `junsong-miniprogram/test/auth-session-state.test.mjs`: functional tests for auth state and single recovery.
- Create `junsong-miniprogram/test/work-context.test.mjs`: functional tests for department normalization, switching, and stale-response rejection.
- Create `junsong-miniprogram/test/request-policy.test.mjs`: functional tests for error classes and retry decisions.
- Modify `junsong-miniprogram/src/api/index.js`: delegate rules to utilities, inject context metadata, and expose response-version checks.
- Modify `junsong-miniprogram/src/App.vue`: bootstrap and foreground session/context coordination.
- Modify `junsong-miniprogram/src/pages/login/index.vue`: persist validated identity through the shared context.
- Modify `junsong-miniprogram/src/pages/index/index.vue`: consume shared department context and discard stale responses.
- Modify `junsong-miniprogram/test/auth-session.test.mjs`: retain only integration-contract assertions not covered by functional tests.
- Modify `junsong-miniprogram/test/home-control.test.mjs`: keep display normalization tests; move state behavior to `work-context.test.mjs`.

### Task 1: Pure Authentication Session State

**Files:**
- Create: `junsong-miniprogram/src/utils/authSession.js`
- Create: `junsong-miniprogram/test/auth-session-state.test.mjs`

- [ ] **Step 1: Write the failing functional tests**

```js
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  createAuthSession,
  isAuthExpiredResponse
} from '../src/utils/authSession.js'

test('recognizes HTTP, business-code, and message authentication expiry', () => {
  assert.equal(isAuthExpiredResponse(401, {}), true)
  assert.equal(isAuthExpiredResponse(200, { code: 401 }), true)
  assert.equal(isAuthExpiredResponse(200, { msg: '登录已超时，请重新登录' }), true)
  assert.equal(isAuthExpiredResponse(500, { msg: '服务异常' }), false)
})

test('coalesces concurrent recovery attempts and allows a later recovery', async () => {
  let recoveries = 0
  let release
  const blocked = new Promise((resolve) => { release = resolve })
  const session = createAuthSession({
    recover: async () => {
      recoveries += 1
      await blocked
    }
  })

  const first = session.recoverOnce()
  const second = session.recoverOnce()
  assert.equal(first, second)
  assert.equal(recoveries, 1)

  release()
  await first
  await session.recoverOnce()
  assert.equal(recoveries, 2)
})
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `cd junsong-miniprogram && node --test test/auth-session-state.test.mjs`

Expected: FAIL with `ERR_MODULE_NOT_FOUND` for `src/utils/authSession.js`.

- [ ] **Step 3: Implement the pure session utility**

```js
const AUTH_EXPIRED_MESSAGES = [
  '登录已超时',
  '请重新登录',
  '登录状态已过期',
  '令牌已过期',
  '令牌不能为空'
]

export function isAuthExpiredResponse(statusCode, data = {}) {
  const message = String(data.msg || data.message || data.errMsg || '')
  return statusCode === 401 ||
    Number(data.code) === 401 ||
    AUTH_EXPIRED_MESSAGES.some((text) => message.includes(text))
}

export function createAuthSession({ recover }) {
  let recovery = null
  return {
    recoverOnce() {
      if (recovery) return recovery
      recovery = Promise.resolve()
        .then(() => recover())
        .finally(() => { recovery = null })
      return recovery
    },
    isRecovering() {
      return recovery !== null
    }
  }
}
```

- [ ] **Step 4: Run the focused test**

Run: `cd junsong-miniprogram && node --test test/auth-session-state.test.mjs`

Expected: 2 tests PASS.

- [ ] **Step 5: Commit the task**

```bash
git -C junsong-miniprogram add src/utils/authSession.js test/auth-session-state.test.mjs
git -C junsong-miniprogram commit -m "refactor(miniprogram): extract authentication session state"
```

### Task 2: Work Context With Versioned Department Switching

**Files:**
- Create: `junsong-miniprogram/src/utils/workContext.js`
- Create: `junsong-miniprogram/test/work-context.test.mjs`

- [ ] **Step 1: Write the failing work-context tests**

```js
import test from 'node:test'
import assert from 'node:assert/strict'

import { createWorkContext } from '../src/utils/workContext.js'

const departments = [
  { deptId: 202, deptName: '盛和里' },
  { deptId: 220, deptName: '兴议家园店' }
]

test('stores the complete department collection and selected department', () => {
  const context = createWorkContext()
  context.hydrate({ user: { userId: 102 }, depts: departments, currentDeptId: 202 })

  assert.equal(context.snapshot().depts.length, 2)
  assert.equal(context.snapshot().currentDept.name, '盛和里')
})

test('increments version when department changes and rejects stale responses', () => {
  const context = createWorkContext()
  context.hydrate({ user: { userId: 102 }, depts: departments, currentDeptId: 202 })
  const oldVersion = context.captureVersion()

  context.selectDept(220)

  assert.equal(context.snapshot().currentDept.name, '兴议家园店')
  assert.equal(context.isCurrent(oldVersion), false)
  assert.equal(context.isCurrent(context.captureVersion()), true)
})

test('fails closed when selecting an unauthorized department', () => {
  const context = createWorkContext()
  context.hydrate({ user: { userId: 102 }, depts: departments, currentDeptId: 202 })

  assert.throws(() => context.selectDept(999), /无权访问该部门/)
  assert.equal(context.snapshot().currentDeptId, 202)
})
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `cd junsong-miniprogram && node --test test/work-context.test.mjs`

Expected: FAIL with `ERR_MODULE_NOT_FOUND` for `src/utils/workContext.js`.

- [ ] **Step 3: Implement normalized versioned context**

```js
function normalizeDept(dept) {
  const id = dept?.deptId ?? dept?.id
  const name = dept?.deptName || dept?.name || ''
  return id === undefined || id === null || !name ? null : { ...dept, id, name }
}

export function createWorkContext(initial = {}) {
  let version = 0
  let state = { user: null, depts: [], currentDeptId: null, currentDept: null }

  const select = (deptId, increment = true) => {
    const currentDept = state.depts.find((dept) => String(dept.id) === String(deptId))
    if (!currentDept) throw new Error('无权访问该部门')
    state = { ...state, currentDeptId: currentDept.id, currentDept }
    if (increment) version += 1
  }

  const context = {
    hydrate({ user, depts = [], currentDeptId }) {
      const normalized = depts.map(normalizeDept).filter(Boolean)
      state = { user: user || null, depts: normalized, currentDeptId: null, currentDept: null }
      if (normalized.length) select(currentDeptId ?? normalized[0].id, false)
      version += 1
      return context.snapshot()
    },
    selectDept(deptId) {
      select(deptId, true)
      return context.snapshot()
    },
    clear() {
      state = { user: null, depts: [], currentDeptId: null, currentDept: null }
      version += 1
    },
    snapshot() {
      return { ...state, depts: [...state.depts], version }
    },
    captureVersion() {
      return version
    },
    isCurrent(candidate) {
      return candidate === version
    }
  }

  if (initial.user || initial.depts?.length) context.hydrate(initial)
  return context
}

export const workContext = createWorkContext()
```

- [ ] **Step 4: Run the focused test**

Run: `cd junsong-miniprogram && node --test test/work-context.test.mjs`

Expected: 3 tests PASS.

- [ ] **Step 5: Commit the task**

```bash
git -C junsong-miniprogram add src/utils/workContext.js test/work-context.test.mjs
git -C junsong-miniprogram commit -m "feat(miniprogram): add versioned department work context"
```

### Task 3: Request Error and Retry Policy

**Files:**
- Create: `junsong-miniprogram/src/utils/requestPolicy.js`
- Create: `junsong-miniprogram/test/request-policy.test.mjs`

- [ ] **Step 1: Write failing policy tests**

```js
import test from 'node:test'
import assert from 'node:assert/strict'

import { classifyRequestError, canRetryRequest } from '../src/utils/requestPolicy.js'

test('classifies timeout, network, permission, validation, and server failures', () => {
  assert.equal(classifyRequestError({ errMsg: 'request:fail timeout' }).kind, 'timeout')
  assert.equal(classifyRequestError({ errMsg: 'request:fail network' }).kind, 'network')
  assert.equal(classifyRequestError({ statusCode: 403 }).kind, 'permission')
  assert.equal(classifyRequestError({ statusCode: 400 }).kind, 'validation')
  assert.equal(classifyRequestError({ statusCode: 503 }).kind, 'server')
})

test('only retries safe read operations automatically', () => {
  assert.equal(canRetryRequest({ method: 'GET', kind: 'network' }), true)
  assert.equal(canRetryRequest({ method: 'POST', kind: 'network' }), false)
  assert.equal(canRetryRequest({ method: 'GET', kind: 'permission' }), false)
})
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `cd junsong-miniprogram && node --test test/request-policy.test.mjs`

Expected: FAIL with `ERR_MODULE_NOT_FOUND` for `src/utils/requestPolicy.js`.

- [ ] **Step 3: Implement the policy**

```js
export function classifyRequestError(error = {}) {
  const statusCode = Number(error.statusCode || error.code)
  const message = String(error.errMsg || error.message || error.msg || '')
  if (message.includes('timeout')) return { kind: 'timeout', message: '请求超时，请稍后重试' }
  if (message.includes('network')) return { kind: 'network', message: '网络连接失败，请检查网络' }
  if (statusCode === 401) return { kind: 'auth', message: '登录已超时，请重新登录' }
  if (statusCode === 403) return { kind: 'permission', message: error.msg || '暂无操作权限' }
  if (statusCode >= 400 && statusCode < 500) return { kind: 'validation', message: error.msg || '提交内容有误' }
  if (statusCode >= 500) return { kind: 'server', message: '服务暂时不可用' }
  return { kind: 'unknown', message: error.msg || '请求失败' }
}

export function canRetryRequest({ method = 'GET', kind }) {
  return ['GET', 'HEAD'].includes(method.toUpperCase()) && ['timeout', 'network', 'server'].includes(kind)
}
```

- [ ] **Step 4: Run the focused test**

Run: `cd junsong-miniprogram && node --test test/request-policy.test.mjs`

Expected: 2 tests PASS.

- [ ] **Step 5: Commit the task**

```bash
git -C junsong-miniprogram add src/utils/requestPolicy.js test/request-policy.test.mjs
git -C junsong-miniprogram commit -m "feat(miniprogram): classify request recovery policy"
```

### Task 4: Integrate Session and Context Into the Request Adapter

**Files:**
- Modify: `junsong-miniprogram/src/api/index.js`
- Modify: `junsong-miniprogram/test/auth-session.test.mjs`
- Create: `junsong-miniprogram/test/request-context.test.mjs`

- [ ] **Step 1: Add failing integration-contract tests**

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const api = fs.readFileSync(new URL('../src/api/index.js', import.meta.url), 'utf8')

test('request adapter delegates authentication recovery to auth session', () => {
  assert.match(api, /import \{ createAuthSession, isAuthExpiredResponse \}/)
  assert.match(api, /authSession\.recoverOnce\(\)/)
})

test('request captures work-context version and exposes stale response marker', () => {
  assert.match(api, /const contextSnapshot = workContext\.snapshot\(\)/)
  assert.match(api, /const contextVersion = contextSnapshot\.version/)
  assert.match(api, /staleContext: !workContext\.isCurrent\(contextVersion\)/)
})

test('request metadata records the captured department id', () => {
  assert.match(api, /const contextSnapshot = workContext\.snapshot\(\)/)
  assert.match(api, /currentDeptId: contextSnapshot\.currentDeptId/)
})
```

- [ ] **Step 2: Run the integration tests and verify they fail**

Run: `cd junsong-miniprogram && node --test test/auth-session.test.mjs test/request-context.test.mjs`

Expected: existing tests PASS and the new request-context assertions FAIL.

- [ ] **Step 3: Replace the local redirect flag with shared recovery**

Add imports and one adapter instance at the top of `src/api/index.js`:

```js
import { createAuthSession, isAuthExpiredResponse } from '@/utils/authSession.js'
import { workContext } from '@/utils/workContext.js'
import { classifyRequestError } from '@/utils/requestPolicy.js'

const authSession = createAuthSession({
  recover: async () => {
    clearSession()
    uni.showToast({ title: '登录已超时，请重新登录', icon: 'none' })
    await new Promise((resolve) => setTimeout(resolve, 300))
    await new Promise((resolve) => {
      uni.reLaunch({ url: '/pages/login/index', complete: resolve })
    })
  }
})
```

Remove `authRedirecting`, the private `isAuthExpiredResponse`, and `redirectToLogin`. In the authentication-expiry branch use:

```js
if (isAuthExpiredResponse(res.statusCode, data)) {
  if (!options.noRedirect) authSession.recoverOnce().catch(() => {})
  finish(data, false)
  return
}
```

- [ ] **Step 4: Add department context and stale-response metadata**

Capture the context before `uni.request`:

```js
const contextSnapshot = workContext.snapshot()
const contextVersion = contextSnapshot.version
```

Before successful resolution, preserve the response shape and add metadata only when requested:

```js
const result = options.withContextMeta
  ? {
      ...data,
      contextMeta: {
        contextVersion,
        currentDeptId: contextSnapshot.currentDeptId,
        staleContext: !workContext.isCurrent(contextVersion)
      }
    }
  : data
finish(result, true)
```

Do not invent a department header in this package. The existing `/system/user/switchDept/{id}` call remains the authoritative server-side context switch; the captured client context prevents responses started before that switch from updating the new page.

Use `classifyRequestError` in the `fail` branch to set `code` and user-facing `msg`; do not add automatic POST/PUT/DELETE retry.

- [ ] **Step 5: Clear work context with the session**

Update `clearSession`:

```js
export function clearSession() {
  setToken('')
  workContext.clear()
  uni.removeStorageSync('userInfo')
  uni.removeStorageSync('modules')
  uni.removeStorageSync('permissions')
}
```

- [ ] **Step 6: Run focused and full utility tests**

Run: `cd junsong-miniprogram && node --test test/auth-session-state.test.mjs test/request-policy.test.mjs test/request-context.test.mjs test/auth-session.test.mjs`

Expected: all tests PASS.

- [ ] **Step 7: Commit the task**

```bash
git -C junsong-miniprogram add src/api/index.js test/auth-session.test.mjs test/request-context.test.mjs
git -C junsong-miniprogram commit -m "refactor(miniprogram): coordinate request session and department context"
```

### Task 5: Hydrate Context at Login and App Foreground

**Files:**
- Modify: `junsong-miniprogram/src/pages/login/index.vue`
- Modify: `junsong-miniprogram/src/App.vue`
- Create: `junsong-miniprogram/test/work-context-integration.test.mjs`

- [ ] **Step 1: Add failing source-contract tests**

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('login hydrates shared context with all departments', () => {
  const login = read('src/pages/login/index.vue')
  assert.match(login, /workContext\.hydrate\(\{[\s\S]*?user:[\s\S]*?depts: deptList[\s\S]*?currentDeptId/)
})

test('app foreground refreshes identity after token refresh', () => {
  const app = read('src/App.vue')
  assert.match(app, /await refreshAuthSession\(\)/)
  assert.match(app, /await refreshWorkContext\(\)/)
})
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `cd junsong-miniprogram && node --test test/work-context-integration.test.mjs`

Expected: both assertions FAIL.

- [ ] **Step 3: Export a context refresh adapter**

Add to `src/api/index.js`:

```js
export async function refreshWorkContext() {
  if (!getToken()) return null
  const res = await request({ url: '/system/user/getInfo', silent: true })
  const data = res.data || res
  const user = data.user || data
  const depts = data.depts || user.depts || []
  const currentDeptId = data.currentDeptId || user.currentDeptId || user.deptId
  const snapshot = workContext.hydrate({ user, depts, currentDeptId })
  uni.setStorageSync('userInfo', { ...user, depts, currentDeptId: snapshot.currentDeptId })
  return snapshot
}
```

Use `/system/user/getInfo`, which is already used by the password-login flow and returns the authorized department collection.

- [ ] **Step 4: Hydrate after both password and WeChat login paths**

Import `workContext` in `src/pages/login/index.vue`. Immediately after the existing code has `user`, `deptList`, and `currentDeptId`, add:

```js
const snapshot = workContext.hydrate({
  user,
  depts: deptList,
  currentDeptId
})
uni.setStorageSync('userInfo', {
  ...user,
  depts: deptList,
  deptId: snapshot.currentDeptId,
  currentDeptId: snapshot.currentDeptId
})
```

Apply the same helper call to password and WeChat completion paths; do not keep two different normalization rules.

- [ ] **Step 5: Validate session and identity on foreground**

Update the `App.vue` script:

```js
import { getToken, refreshAuthSession, refreshWorkContext } from '@/api/index.js'

export default {
  // keep the existing onLaunch error handlers
  async onShow() {
    if (!getToken()) return
    try {
      await refreshAuthSession()
      await refreshWorkContext()
    } catch (_) {
      // The request/session layer owns user-visible recovery.
    }
  }
}
```

- [ ] **Step 6: Run focused tests**

Run: `cd junsong-miniprogram && node --test test/auth-session.test.mjs test/work-context.test.mjs test/work-context-integration.test.mjs`

Expected: all tests PASS.

- [ ] **Step 7: Commit the task**

```bash
git -C junsong-miniprogram add src/App.vue src/api/index.js src/pages/login/index.vue test/work-context-integration.test.mjs
git -C junsong-miniprogram commit -m "feat(miniprogram): hydrate work context after authentication"
```

### Task 6: Migrate Home Department Switching

**Files:**
- Modify: `junsong-miniprogram/src/pages/index/index.vue`
- Modify: `junsong-miniprogram/test/home-control.test.mjs`
- Create: `junsong-miniprogram/test/home-work-context.test.mjs`

- [ ] **Step 1: Add failing home integration assertions**

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const home = fs.readFileSync(new URL('../src/pages/index/index.vue', import.meta.url), 'utf8')

test('home derives current department from shared work context', () => {
  assert.match(home, /workContext\.snapshot\(\)/)
  assert.match(home, /snapshot\.currentDept/)
})

test('department switching selects shared context before reloading dashboard', () => {
  assert.match(home, /workContext\.selectDept\(this\.pendingDeptId\)/)
  assert.match(home, /await this\.loadAll\(\)/)
})

test('dashboard loaders request context metadata and reject stale responses', () => {
  assert.match(home, /withContextMeta: true/)
  assert.match(home, /contextMeta\?\.staleContext/)
})
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `cd junsong-miniprogram && node --test test/home-work-context.test.mjs`

Expected: all three assertions FAIL.

- [ ] **Step 3: Read shared context on page show**

Import `workContext` and add one synchronization method:

```js
syncWorkContext() {
  const snapshot = workContext.snapshot()
  this.userInfo = snapshot.user || this.userInfo
  this.deptList = normalizeDeptOptions(snapshot.depts)
  this.allDepts = snapshot.depts
  this.currentDeptId = snapshot.currentDeptId
}
```

Call `this.syncWorkContext()` before existing dashboard loads in `onShow`. Keep component fields temporarily for template compatibility; they become projections of shared state, not an independent source.

- [ ] **Step 4: Switch the server and shared context as one UI operation**

After the existing `/system/user/switchDept/{id}` request succeeds:

```js
const snapshot = workContext.selectDept(this.pendingDeptId)
this.currentDeptId = snapshot.currentDeptId
this.userInfo = {
  ...this.userInfo,
  deptId: snapshot.currentDeptId,
  currentDeptId: snapshot.currentDeptId,
  deptName: snapshot.currentDept.name
}
uni.setStorageSync('userInfo', this.userInfo)
this.closeDeptPicker()
await this.loadAll()
```

If the server switch fails, do not call `selectDept`; the visible department remains unchanged.

- [ ] **Step 5: Reject late responses from the old department**

For each home request that mutates dashboard state, request metadata:

```js
const res = await request({
  url: endpoint,
  data: params,
  withContextMeta: true
})
if (res.contextMeta?.staleContext) return
```

Extract `contextMeta` before mapping business data so it cannot be mistaken for dashboard content. Apply this to overview, member, growth, action, points, period, and task loaders that run during `loadAll`.

- [ ] **Step 6: Run home and context tests**

Run: `cd junsong-miniprogram && node --test test/home-control.test.mjs test/home-work-context.test.mjs test/work-context.test.mjs`

Expected: all tests PASS.

- [ ] **Step 7: Commit the task**

```bash
git -C junsong-miniprogram add src/pages/index/index.vue test/home-control.test.mjs test/home-work-context.test.mjs
git -C junsong-miniprogram commit -m "refactor(miniprogram): use shared department context on home"
```

### Task 7: Regression, Build, and Release Gate

**Files:**
- Verify: `junsong-miniprogram/src/`, `junsong-miniprogram/test/`, and generated build output.

- [ ] **Step 1: Run all mini-program tests**

Run: `cd junsong-miniprogram && node --test test/*.test.mjs`

Expected: all tests PASS with no unhandled rejection.

- [ ] **Step 2: Build the WeChat mini-program**

Run: `cd junsong-miniprogram && npm run build:mp-weixin`

Expected: build exits 0 and produces `dist/build/mp-weixin` (or the repository's currently configured equivalent). Existing Vite deprecation warnings may remain, but no compile error is allowed.

- [ ] **Step 3: Verify generated files were not hand-edited**

Run: `git -C junsong-miniprogram status --short`

Expected: source and test changes match the commits above; generated `dist/` changes come only from the build and are reviewed according to the nested repository's existing release convention.

- [ ] **Step 4: Perform WeChat Developer Tools acceptance**

Use two accounts: one with a single department and one with multiple departments. Verify this exact matrix:

```text
Password login -> background 30+ minutes -> foreground -> save succeeds or login is shown
WeChat login -> expired/cleared server session -> save -> one login recovery UI is shown
Department A dashboard request delayed -> switch to Department B -> delayed A data never replaces B
Switch Department B -> Home, Workbench, list request, and profile all show Department B
Repeated save taps -> one business record and one success result
```

- [ ] **Step 5: Run nested repository impact review before release commit**

Run from the parent repository using the project GitNexus workflow:

```bash
PATH=/usr/local/bin:$PATH /usr/local/bin/node .gitnexus/run.cjs detect-changes --scope staged
git -C junsong-miniprogram diff --cached --check
```

Expected: only session, request, login, work-context, and home execution flows are affected; no unexpected module flow appears.

- [ ] **Step 6: Tag the package-1 acceptance point**

```bash
git -C junsong-miniprogram tag miniprogram-session-context-accepted-20260718
```

Expected: tag resolves to the tested nested-repository commit. Do not deploy or upload the mini-program solely because the tag exists; production release still follows the deployment handbook and WeChat review process.

## Package Completion Criteria

- Concurrent authentication failures produce one recovery UI.
- Password and WeChat login hydrate the same complete identity/departments structure.
- Foreground resume validates both session and work context.
- Department switch changes the shared context only after server success.
- Late responses from the old department cannot overwrite the new department dashboard.
- Non-idempotent writes are never automatically retried.
- All mini-program tests and the WeChat build pass.
- The five-item Developer Tools acceptance matrix passes for single- and multi-department users.
