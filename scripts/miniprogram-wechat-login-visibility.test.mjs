import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

test('mini-program login page renders the WeChat login entry only when capability enables it', () => {
  const source = fs.readFileSync('junsong-miniprogram/src/pages/login/index.vue', 'utf8')
  assert.match(source, /<view v-if="wechatLoginEnabled" class="wechat-login-section">/)
  assert.match(source, /url:\s*'\/member\/mp\/capabilities'/)
  assert.match(source, /this\.wechatLoginEnabled = data && data\.wechatLoginEnabled === true/)
})
