import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

// =========================================================================
// 后端：SysUserController PC 管理接口
// =========================================================================

test('SysUserController: 包含 GET /user/{userId}/mp-binding 端点', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysUserController.java')
  assert.match(src, /GetMapping\("\/\{userId\}\/mp-binding"\)|GetMapping\("\/\{userId\}\/mp-binding"\)/)
})

test('SysUserController: mp-binding 查询使用独立权限 system:user:unbindMp', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysUserController.java')
  // 查询和解绑都应使用 system:user:unbindMp 权限
  assert.match(src, /RequiresPermissions\("system:user:unbindMp"\)/)
})

test('SysUserController: 解绑端点使用 @Log 审计日志', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysUserController.java')
  // DELETE 端点必须有 @Log 注解
  assert.match(src, /@Log\(/)
})

test('SysUserController: 注入 ISysUserMpBindingService', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysUserController.java')
  assert.match(src, /ISysUserMpBindingService/)
})

test('SysUserController: 响应中不返回 openid/unionid 明文', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysUserController.java')
  // 必须在返回前将 openid/unionid 设为 null 或使用脱敏
  // 检查是否存在 setOpenid(null) 或 setUnionid(null) 调用
  assert.match(src, /setOpenid\(null\)/)
  assert.match(src, /setUnionid\(null\)/)
})

test('SysUserController: 包含 DELETE /user/{userId}/mp-binding 解绑端点', () => {
  const src = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/controller/SysUserController.java')
  assert.match(src, /DeleteMapping\("\/\{userId\}\/mp-binding"\)/)
})

// =========================================================================
// PC API：wechatBinding.ts
// =========================================================================

test('wechatBinding.ts: 文件存在', () => {
  assert.doesNotThrow(() => read('junsong-ui-v3/src/api/system/wechatBinding.ts'))
})

test('wechatBinding.ts: 包含 getUserBindings 方法', () => {
  const src = read('junsong-ui-v3/src/api/system/wechatBinding.ts')
  assert.match(src, /getUserBindings/)
  assert.match(src, /mp-binding/)
})

test('wechatBinding.ts: 包含 adminUnbind 方法', () => {
  const src = read('junsong-ui-v3/src/api/system/wechatBinding.ts')
  assert.match(src, /adminUnbind/)
  assert.match(src, /delete|DELETE/i)
})

// =========================================================================
// PC UI：用户管理页绑定管理
// =========================================================================

test('user/index.vue: 引入 wechatBinding API', () => {
  const src = read('junsong-ui-v3/src/views/system/user/index.vue')
  assert.match(src, /wechatBinding|getUserBindings|adminUnbind/)
})

test('user/index.vue: 使用 system:user:unbindMp 权限控制解绑按钮', () => {
  const src = read('junsong-ui-v3/src/views/system/user/index.vue')
  assert.match(src, /system:user:unbindMp/)
})

test('user/index.vue: 解绑操作有二次确认', () => {
  const src = read('junsong-ui-v3/src/views/system/user/index.vue')
  // ElMessageBox.confirm 或 $modal.confirm 用于二次确认
  assert.match(src, /confirm|ElMessageBox|modal/)
})

test('user/index.vue: 不展示 openid/unionid 字段', () => {
  const src = read('junsong-ui-v3/src/views/system/user/index.vue')
  // 模板中不应出现 openid 或 unionid 的显示
  assert.doesNotMatch(src, /openid|unionid/i)
})
