import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const home = fs.readFileSync(new URL('../src/pages/index/index.vue', import.meta.url), 'utf8')

test('home derives a display-only work view from shared departments and authorized modules', () => {
  assert.match(home, /import \{ deriveWorkView \} from '@\/utils\/workView\.js'/)
  assert.match(home, /import \{ recordRecent \} from '@\/utils\/workbenchPersonalization\.js'/)

  const block = home.match(/workView\(\) \{([\s\S]*?)(?=\n    [a-zA-Z])/i)?.[1] || ''
  assert.match(block, /workContext\.snapshot\(\)/)
  assert.match(block, /deriveWorkView\(\{[\s\S]*?depts:\s*context\.depts,[\s\S]*?modules:\s*authorizedModules/)
  assert.match(home, /\{\{ workView\.label \}\}/)
  assert.match(home, /class="work-view-label"/)
})

test('home does not render bottom quick actions', () => {
  assert.doesNotMatch(home, />常用操作</)
  assert.doesNotMatch(home, /class="quick-section/)
  assert.doesNotMatch(home, /filteredQuickActions\(\)/)
})

test('opening a module records recent use only after permission succeeds', () => {
  const block = home.match(/openModule\(key\) \{([\s\S]*?)(?=\n    \/\/ 跳转会员运营看板)/)?.[1] || ''
  const permissionEnd = block.indexOf("return\n      }")
  const recordIndex = block.indexOf('recordRecent(')

  assert.ok(permissionEnd >= 0, 'permission guard should remain before navigation')
  assert.ok(recordIndex > permissionEnd, 'recent use should be recorded after permission passes')
  assert.match(block, /uni\.getStorageSync\('miniProgramRecent'\)/)
  assert.match(block, /recordRecent\([^\n]+key,\s*this\.modules\)/)
  assert.match(block, /uni\.setStorageSync\('miniProgramRecent'/)
})
