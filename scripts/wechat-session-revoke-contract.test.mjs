import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

// =========================================================================
// 后端：SysWechatSessionController
// =========================================================================

test('SysWechatSessionController: 包含 POST /wechat-session/revoke-all 端点', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysWechatSessionController.java')
  assert.match(src, /PostMapping\("\/revoke-all"\)/)
  assert.match(src, /RequestMapping\("\/wechat-session"\)/)
})

test('SysWechatSessionController: 使用独立权限 system:user:wechatSession:revokeAll', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysWechatSessionController.java')
  assert.match(src, /RequiresPermissions\("system:user:wechatSession:revokeAll"\)/)
})

test('SysWechatSessionController: 一键失效使用 @Log 审计日志', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysWechatSessionController.java')
  assert.match(src, /@Log\(/)
  assert.match(src, /微信会话一键失效/)
})

test('SysWechatSessionController: 不返回 openid/unionid/token 明细', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysWechatSessionController.java')
  // 控制器不应出现 openid/unionid 返回逻辑
  assert.doesNotMatch(src, /getOpenid|getUnionid|openid.*result|unionid.*result/i)
})

test('SysWechatSessionController: 操作使用当前租户上下文', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysWechatSessionController.java')
  assert.match(src, /TenantContext\.getTenantId\(\)/)
})

// =========================================================================
// 后端：SysWechatSessionService
// =========================================================================

test('SysWechatSessionService: 包含 revokeAllWechatSessions 方法', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/SysWechatSessionService.java')
  assert.match(src, /revokeAllWechatSessions/)
})

test('SysWechatSessionService: 使用 Redis increment 原子递增', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/SysWechatSessionService.java')
  assert.match(src, /redisService\.increment/)
})

test('SysWechatSessionService: 返回结果包含 tenantId/previousEpoch/currentEpoch', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/service/SysWechatSessionService.java')
  assert.match(src, /"tenantId"/)
  assert.match(src, /"previousEpoch"/)
  assert.match(src, /"currentEpoch"/)
})

// =========================================================================
// 后端：LoginUser 增加 authSource 和 wechatSessionEpoch
// =========================================================================

test('LoginUser: 包含 authSource 字段', () => {
  const src = read('junsong-api/junsong-api-system/src/main/java/com/junsong/system/api/model/LoginUser.java')
  assert.match(src, /authSource/)
  assert.match(src, /getAuthSource/)
  assert.match(src, /setAuthSource/)
})

test('LoginUser: 包含 wechatSessionEpoch 字段', () => {
  const src = read('junsong-api/junsong-api-system/src/main/java/com/junsong/system/api/model/LoginUser.java')
  assert.match(src, /wechatSessionEpoch/)
  assert.match(src, /getWechatSessionEpoch/)
  assert.match(src, /setWechatSessionEpoch/)
})

// =========================================================================
// 后端：TokenService 微信会话版本管理
// =========================================================================

test('TokenService: createToken 设置 authSource=PASSWORD', () => {
  const src = read('junsong-common/junsong-common-security/src/main/java/com/junsong/common/security/service/TokenService.java')
  assert.match(src, /setAuthSource\("PASSWORD"\)/)
})

test('TokenService: createTokenMp 重载方法接受 authSource 参数', () => {
  const src = read('junsong-common/junsong-common-security/src/main/java/com/junsong/common/security/service/TokenService.java')
  assert.match(src, /createTokenMp\(LoginUser\s+\w+,\s*String\s+\w+\)/)
})

test('TokenService: createTokenMp 仅 WECHAT_MP 设置 epoch', () => {
  const src = read('junsong-common/junsong-common-security/src/main/java/com/junsong/common/security/service/TokenService.java')
  assert.match(src, /WECHAT_MP/)
  assert.match(src, /setWechatSessionEpoch\(null\)/)
})

test('TokenController: mp/login 传 PASSWORD 来源（小程序密码登录不受微信会话失效影响）', () => {
  const src = read('junsong-auth/src/main/java/com/junsong/auth/controller/TokenController.java')
  assert.match(src, /createTokenMp\(\w+,\s*"PASSWORD"\)/)
})

test('WechatMpBindingController: 微信登录传 WECHAT_MP 来源', () => {
  const src = read('junsong-auth/src/main/java/com/junsong/auth/controller/WechatMpBindingController.java')
  const matches = src.match(/createTokenMp\(\w+,\s*"WECHAT_MP"\)/g) || []
  assert.ok(matches.length >= 2, '微信登录和绑定都应传 WECHAT_MP，实际匹配数: ' + matches.length)
})

test('TokenService: 包含 incrementWechatSessionEpoch 方法', () => {
  const src = read('junsong-common/junsong-common-security/src/main/java/com/junsong/common/security/service/TokenService.java')
  assert.match(src, /incrementWechatSessionEpoch/)
})

test('TokenService: 包含 verifyWechatSessionEpoch 方法', () => {
  const src = read('junsong-common/junsong-common-security/src/main/java/com/junsong/common/security/service/TokenService.java')
  assert.match(src, /verifyWechatSessionEpoch/)
  // 非 WECHAT_MP 来源直接返回 true（PASSWORD 会话不参与校验）
  assert.match(src, /WECHAT_MP/)
  assert.match(src, /return true/)
})

// =========================================================================
// 后端：HeaderInterceptor 每次请求校验 epoch
// =========================================================================

test('HeaderInterceptor: 调用 verifyWechatSessionEpoch 校验', () => {
  const src = read('junsong-common/junsong-common-security/src/main/java/com/junsong/common/security/interceptor/HeaderInterceptor.java')
  assert.match(src, /verifyWechatSessionEpoch/)
})

test('HeaderInterceptor: 版本不匹配时返回 false 拒绝请求', () => {
  const src = read('junsong-common/junsong-common-security/src/main/java/com/junsong/common/security/interceptor/HeaderInterceptor.java')
  assert.match(src, /return false/)
})

// =========================================================================
// 后端：CacheConstants 新增 WECHAT_SESSION_EPOCH_KEY
// =========================================================================

test('CacheConstants: 包含 WECHAT_SESSION_EPOCH_KEY 常量', () => {
  const src = read('junsong-common/junsong-common-core/src/main/java/com/junsong/common/core/constant/CacheConstants.java')
  assert.match(src, /WECHAT_SESSION_EPOCH_KEY/)
})

// =========================================================================
// PC API：wechatBinding.ts 新增会话失效方法
// =========================================================================

test('wechatBinding.ts: 包含 revokeAllWechatSessions 方法', () => {
  const src = read('junsong-ui-v3/src/api/system/wechatBinding.ts')
  assert.match(src, /revokeAllWechatSessions|revokeAll/)
  assert.match(src, /wechat-session\/revoke-all/)
})

test('wechatBinding.ts: 包含 getWechatSessionEpoch 方法', () => {
  const src = read('junsong-ui-v3/src/api/system/wechatBinding.ts')
  assert.match(src, /getWechatSessionEpoch|getEpoch/)
  assert.match(src, /wechat-session\/epoch/)
})

// =========================================================================
// PC UI：用户管理页会话失效按钮
// =========================================================================

test('user/index.vue: 包含 system:user:wechatSession:revokeAll 权限控制', () => {
  const src = read('junsong-ui-v3/src/views/system/user/index.vue')
  assert.match(src, /system:user:wechatSession:revokeAll/)
})

test('user/index.vue: 使用"微信会话失效"文字而非"解除绑定"', () => {
  const src = read('junsong-ui-v3/src/views/system/user/index.vue')
  // 不应使用"解除绑定"文字
  assert.doesNotMatch(src, /解除绑定/)
  // 应使用"微信会话失效"或"会话失效"文字
  assert.match(src, /微信会话失效|会话失效/)
})

test('user/index.vue: 会话失效操作有二次确认', () => {
  const src = read('junsong-ui-v3/src/views/system/user/index.vue')
  // 检查 confirm 或 ElMessageBox
  const confirmCount = (src.match(/ElMessageBox\.confirm/g) || []).length
  assert.ok(confirmCount >= 2, '应至少有 2 个 ElMessageBox.confirm（删除 + 会话失效）')
})

test('user/index.vue: 引入 revokeAllWechatSessions API', () => {
  const src = read('junsong-ui-v3/src/views/system/user/index.vue')
  assert.match(src, /revokeAllWechatSessions|revokeAll/)
})

test('user/index.vue: 不展示 openid/unionid 字段', () => {
  const src = read('junsong-ui-v3/src/views/system/user/index.vue')
  assert.doesNotMatch(src, /openid|unionid/i)
})
