import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

// =========================================================================
// 登录页：微信快捷登录按钮与跳转
// =========================================================================

test('login page: 微信登录按钮按 wechatLoginEnabled 条件渲染', () => {
  const login = read('src/pages/login/index.vue')
  assert.match(login, /<view v-if="wechatLoginEnabled" class="wechat-login-section">/)
})

test('login page: wechatLoginEnabled 默认为 false（fail-closed）', () => {
  const login = read('src/pages/login/index.vue')
  assert.match(login, /wechatLoginEnabled:\s*false/)
})

test('login page: handleWechatLogin 防止重复提交', () => {
  const login = read('src/pages/login/index.vue')
  // 方法开头必须有 loading 守卫
  assert.match(login, /handleWechatLogin\(\)\s*\{[\s\S]*?if\s*\(this\.loading\)\s*return/)
})

test('login page: 未绑定时跳转到绑定页面', () => {
  const login = read('src/pages/login/index.vue')
  assert.match(login, /未绑定/)
  assert.match(login, /\/pages\/wechat-bind\/index/)
})

test('login page: 微信登录请求使用 POST /auth/mp/wechat/login', () => {
  const login = read('src/pages/login/index.vue')
  assert.match(login, /url:\s*'\/auth\/mp\/wechat\/login'/)
  assert.match(login, /method:\s*'POST'/)
})

test('login page: capabilities 接口失败时 fail-closed', () => {
  const login = read('src/pages/login/index.vue')
  // loadCapabilities 的 catch 块中必须设置 wechatLoginEnabled = false
  assert.match(login, /catch[\s\S]*?wechatLoginEnabled\s*=\s*false/)
})

// =========================================================================
// 绑定页：首次绑定流程
// =========================================================================

test('wechat-bind page: 文件存在', () => {
  assert.doesNotThrow(() => read('src/pages/wechat-bind/index.vue'))
})

test('wechat-bind page: 包含用户名输入框', () => {
  const page = read('src/pages/wechat-bind/index.vue')
  assert.match(page, /v-model="form\.username"/)
})

test('wechat-bind page: 包含密码输入框', () => {
  const page = read('src/pages/wechat-bind/index.vue')
  assert.match(page, /v-model="form\.password"/)
})

test('wechat-bind page: 调用 wx.login 获取 code', () => {
  const page = read('src/pages/wechat-bind/index.vue')
  assert.match(page, /wx\.login/)
})

test('wechat-bind page: 调用 POST /auth/mp/wechat/bind', () => {
  const page = read('src/pages/wechat-bind/index.vue')
  assert.match(page, /url:\s*'\/auth\/mp\/wechat\/bind'/)
  assert.match(page, /method:\s*'POST'/)
})

test('wechat-bind page: 请求包含 code、username、password', () => {
  const page = read('src/pages/wechat-bind/index.vue')
  // data 对象中包含 code, username, password
  assert.match(page, /code/)
  assert.match(page, /username/)
  assert.match(page, /password/)
})

test('wechat-bind page: 防止重复提交', () => {
  const page = read('src/pages/wechat-bind/index.vue')
  assert.match(page, /if\s*\(this\.loading\)\s*return/)
})

test('wechat-bind page: 绑定成功后设置 token', () => {
  const page = read('src/pages/wechat-bind/index.vue')
  assert.match(page, /setToken/)
})

test('wechat-bind page: 绑定成功后跳转首页', () => {
  const page = read('src/pages/wechat-bind/index.vue')
  assert.match(page, /reLaunch/)
  assert.match(page, /\/pages\/index\/index/)
})

test('wechat-bind page: 保留返回密码登录入口', () => {
  const page = read('src/pages/wechat-bind/index.vue')
  // 有返回登录页的链接或按钮
  assert.match(page, /\/pages\/login\/index|返回|navigateBack/)
})

test('wechat-bind page: 请求使用 silent 和 timeout', () => {
  const page = read('src/pages/wechat-bind/index.vue')
  assert.match(page, /silent:\s*true/)
  assert.match(page, /timeout/)
})

test('wechat-bind page: 不打印原始错误对象', () => {
  const page = read('src/pages/wechat-bind/index.vue')
  // 不应出现 console.log/error/warn 直接打印 error 对象
  assert.doesNotMatch(page, /console\.(log|error|warn)\([^)\n]*,\s*(e|err)\)/)
})

// =========================================================================
// 我的页：解绑功能
// =========================================================================

test('mine page: 包含微信账号管理菜单项', () => {
  const page = read('src/pages/mine/index.vue')
  assert.match(page, /微信|wechat/i)
})

test('mine page: 查询绑定状态 GET /auth/mp/wechat/binding', () => {
  const page = read('src/pages/mine/index.vue')
  assert.match(page, /\/auth\/mp\/wechat\/binding/)
})

test('mine page: 解绑调用 POST /auth/mp/wechat/unbind', () => {
  const page = read('src/pages/mine/index.vue')
  assert.match(page, /\/auth\/mp\/wechat\/unbind/)
  assert.match(page, /method:\s*'POST'/)
})

test('mine page: 解绑前二次确认', () => {
  const page = read('src/pages/mine/index.vue')
  // showModal 用于二次确认
  assert.match(page, /showModal/)
})

test('mine page: 解绑成功后清理本地登录态', () => {
  const page = read('src/pages/mine/index.vue')
  // 解绑成功后应清理 token 和 storage
  assert.match(page, /setToken\(''\)|removeStorageSync\('token'\)/)
})

test('mine page: 请求使用 silent 和 timeout', () => {
  const page = read('src/pages/mine/index.vue')
  assert.match(page, /silent:\s*true/)
  assert.match(page, /timeout/)
})

// =========================================================================
// pages.json: 注册绑定页路由
// =========================================================================

test('pages.json: 包含 wechat-bind 页面路由', () => {
  const json = read('src/pages.json')
  assert.match(json, /pages\/wechat-bind\/index/)
})
