import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('login does not enter workbench when required user context times out', () => {
  const login = read('src/pages/login/index.vue')
  const getInfoCatch = login.match(/catch \(e\) \{\n\s+console\.log\('获取用户信息失败，直接进入'[\s\S]+?\n\s+\}/)

  assert.equal(getInfoCatch, null, 'getInfo failure must stop login instead of entering the workbench')
  assert.match(login, /加载用户信息失败，请重试/)
})

test('department confirmation request is silent and does not print error stacks', () => {
  const login = read('src/pages/login/index.vue')

  assert.match(
    login,
    /url: '\/auth\/mp\/login'[\s\S]{0,160}silent: true[\s\S]{0,120}timeout:/
  )
  assert.match(
    login,
    /url: '\/system\/user\/switchDept\/' \+ this\.selectedDeptId[\s\S]{0,160}silent: true[\s\S]{0,120}timeout:/
  )
  assert.match(
    login,
    /url: '\/member\/mp\/userinfo'[\s\S]{0,180}silent: true[\s\S]{0,120}timeout:/
  )
  assert.doesNotMatch(login, /console\.error\('切换部门失败', e\)/)
})

test('workbench background requests are silent and bounded', () => {
  const index = read('src/pages/index/index.vue')

  for (const url of [
    '/system/user/getInfo',
    '/system/user/deptTree',
    '/member/mp/dashboard/stats',
    '/member/mp/dashboard/overview',
    '/finance/accountingPeriod/current',
    '/member/mp/modules',
    '/member/seckill/list',
    '/member/seckillRecord/statistics',
    '/finance/expense/summary'
  ]) {
    const escaped = url.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    assert.match(
      index,
      new RegExp(`url: '${escaped}'[\\s\\S]{0,120}silent: true[\\s\\S]{0,120}timeout:`),
      `${url} should be silent with an explicit timeout`
    )
  }
})

test('workbench timeout handling does not print raw error objects', () => {
  const index = read('src/pages/index/index.vue')

  assert.doesNotMatch(index, /console\.(log|error|warn)\([^)\n]*,\s*(e|err)\)/)
  assert.match(index, /logRequestFailure\(label, error\)/)
})

test('request wrapper aborts before native request timeout', () => {
  const api = read('src/api/index.js')

  assert.match(api, /let requestTask\s*=\s*null/)
  assert.match(api, /requestTask\s*=\s*uni\.request/)
  assert.match(api, /timeout:\s*timeoutMs\s*\+\s*5000/)
  assert.match(api, /requestTask\?\.abort\?\.\(\)/)
  assert.match(api, /},\s*timeoutMs\)/)
})

test('manual department picker failure still tells the user to retry', () => {
  const index = read('src/pages/index/index.vue')
  const openDeptPicker = index.match(/async openDeptPicker\(\) \{[\s\S]+?\n    \}/)

  assert.ok(openDeptPicker, 'openDeptPicker should exist')
  assert.match(openDeptPicker[0], /silent: true/)
  assert.match(openDeptPicker[0], /uni\.showToast\(\{ title: '加载部门失败，请重试', icon: 'none' \}\)/)
})

test('build config removes dcloud runtime preload asset injection', () => {
  const config = read('vite.config.js')

  assert.match(config, /patchUniMpWeixinRuntime/)
  assert.match(config, /@dcloudio\/uni-mp-weixin\/dist/)
  assert.match(config, /preloadAsset\(\);/)
})

test('login startup avoids system info api prompts', () => {
  for (const path of [
    'src/pages/login/index.vue',
    'src/pages/index/index.vue',
    'src/pages/mine/index.vue',
    'src/utils/systemInfo.js'
  ]) {
    assert.doesNotMatch(read(path), /getSystemInfoSync/, `${path} should not call deprecated getSystemInfoSync`)
    assert.doesNotMatch(read(path), /getWindowInfo|getAppBaseInfo|getDeviceInfo|getSystemInfo/, `${path} should not call system info APIs during startup`)
  }
})

test('build config removes uni runtime system info startup calls', () => {
  const config = read('vite.config.js')

  assert.match(config, /patchUniRuntimeSystemInfo/)
  assert.match(config, /generateBundle/)
  assert.match(config, /writeBundle/)
  assert.match(config, /common\/vendor\.js/)
  assert.match(config, /stripSystemInfoApis/)
  assert.match(config, /replaceAll\('wx\.getSystemInfoSync\(\)'/)
})

test('build config keeps generated project config on the requested base library', () => {
  const config = read('vite.config.js')

  assert.match(config, /patchGeneratedProjectConfig/)
  assert.match(config, /patchGeneratedPrivateProjectConfig/)
  assert.match(config, /project\.config\.json/)
  assert.match(config, /project\.private\.config\.json/)
  assert.match(config, /libVersion/)
  assert.match(config, /sourceProjectConfig\.libVersion/)
})

test('local devtools config keeps api hook recommendations disabled', () => {
  const privateConfig = JSON.parse(read('project.private.config.json'))

  assert.equal(privateConfig.setting.useApiHook, false)
  assert.match(read('vite.config.js'), /useApiHook/)
})

test('app global error logging does not print raw error objects', () => {
  const app = read('src/App.vue')

  assert.match(app, /formatErrorMessage/)
  assert.doesNotMatch(app, /console\.warn\('\[appError\]',\s*err\)/)
  assert.doesNotMatch(app, /console\.warn\('\[unhandledRejection\]',\s*res\?\.reason \|\| res\)/)
})

test('login page redirects to index when token exists (cold-start recovery)', () => {
  const login = read('src/pages/login/index.vue')

  // onLoad should check getToken() and reLaunch to index if token exists
  assert.match(
    login,
    /onLoad\(\) \{[\s\S]*?if \(getToken\(\)\) \{[\s\S]*?uni\.reLaunch\(/,
    'login onLoad must check getToken() and redirect to index page'
  )
  // reLaunch must have a fail callback to recover if navigation fails
  assert.match(login, /fail:/, 'reLaunch must have a fail callback for error recovery')
})
