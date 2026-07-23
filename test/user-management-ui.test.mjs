import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, it } from 'node:test'

const root = new URL('..', import.meta.url).pathname
const page = path => readFileSync(join(root, path), 'utf8')

describe('user management mini-program UI', () => {
  it('shows real avatars and WeChat binding state in the user list', () => {
    const source = page('src/pages/user/index.vue')

    assert.match(source, /import\s+\{\s*request\s*,\s*getBaseUrl\s*\}\s+from\s+'@\/api\/index\.js'/)
    assert.match(source, /<image\s+v-if="avatarUrl\(item\.avatar\)"/)
    assert.match(source, /v-else[^>]*avatar-fallback/)
    assert.match(source, /wechatBindingText\(item\)/)
    assert.match(source, /loadMpBindingStatus\(list\)/)
    assert.match(source, /isActiveMpBinding\(item\)/)
    assert.match(source, /String\(binding\.status[^)]*\)\.toUpperCase\(\)/)
    assert.match(source, /绑定|已绑定|BOUND|ACTIVE/)
    assert.match(source, /\/system\/user\/\$\{user\.userId\}\/mp-binding/)
    assert.match(source, /silent:\s*true/)
    assert.match(source, /timeout:\s*8000/)
    assert.match(source, /avatar\.startsWith\('\/statics\/'\)/)
    assert.match(source, /replace\(\/\\\/prod-api\$\/,\s*''\)\.replace\(\/\\\/dev-api\$\/,\s*''\)/)
  })

  it('provides a tenant-wide WeChat session revoke action', () => {
    const source = page('src/pages/user/index.vue')

    assert.match(source, /一键清除微信会话/)
    assert.match(source, /session-action-card/)
    assert.match(source, /session-button-icon/)
    assert.match(source, /revokeWechatSessions\(\)/)
    assert.match(source, /\/system\/wechat-session\/revoke-all\?reason=/)
    assert.match(source, /uni\.showModal\(/)
  })

  it('supports selecting multiple user departments and submits deptIds', () => {
    const source = page('src/pages/user/form.vue')

    assert.doesNotMatch(source, /<picker\s+:range="deptLabels"\s+@change="onDeptChange"/)
    assert.match(source, /deptIds:\s*\[\]/)
    assert.match(source, /toggleDept\(deptId\)/)
    assert.match(source, /deptSummary\(\)/)
    assert.match(source, /deptIds:\s*this\.form\.deptIds/)
    assert.match(source, /firstNonEmptyArray\(/)
    assert.match(source, /firstNonEmptyArray\(data\.deptIds,\s*res\.deptIds/)
    assert.match(source, /res\.deptIds/)
  })

  it('shows real avatar, assigned departments and WeChat binding state in user detail', () => {
    const source = page('src/pages/user/detail.vue')

    assert.match(source, /import\s+\{\s*request\s*,\s*deleteData\s*,\s*getBaseUrl\s*\}\s+from\s+'@\/api\/index\.js'/)
    assert.match(source, /<image\s+v-if="avatarUrl\(user\.avatar\)"/)
    assert.match(source, /deptNames\(user\)/)
    assert.match(source, /firstNonEmptyArray\(/)
    assert.match(source, /firstNonEmptyArray\(data\.depts,\s*res\.depts/)
    assert.match(source, /user\.deptName/)
    assert.match(source, /user\.deptId/)
    assert.match(source, /deptName:\s*data\.deptName/)
    assert.match(source, /微信绑定/)
    assert.match(source, /loadMpBindingStatus\(\)/)
    assert.match(source, /\/system\/user\/\$\{this\.userId\}\/mp-binding/)
    assert.match(source, /silent:\s*true/)
    assert.match(source, /timeout:\s*8000/)
    assert.match(source, /avatar\.startsWith\('\/statics\/'\)/)
  })
})
