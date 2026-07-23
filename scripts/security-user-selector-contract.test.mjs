import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

const selectorPages = [
  'junsong-ui-v3/src/views/system/delegate/index.vue',
  'junsong-ui-v3/src/views/system/userDept/index.vue',
  'junsong-ui-v3/src/views/workflow/task/index.vue',
]

test('security: user selectors never preload oversized user lists', () => {
  for (const path of selectorPages) {
    const source = read(path)
    assert.doesNotMatch(source, /listUser\s*\(\s*\{[^}]*pageSize\s*:\s*(?:1000|9999)/s, path)
  }
})

test('security: user selectors use remote search with a 20-row request limit', () => {
  for (const path of selectorPages) {
    const source = read(path)
    assert.match(source, /remote-method=/, path)
    assert.match(source, /listUser\s*\(\s*\{[^}]*pageSize\s*:\s*20/s, path)
  }
})

test('security: remote user searches ignore stale responses', () => {
  for (const path of selectorPages) {
    const source = read(path)
    assert.match(source, /userSearchRequestId/, path)
    assert.match(source, /requestId\s*!==\s*userSearchRequestId/, path)
  }
})

test('delegate list returns minimal user labels without user detail requests', () => {
  const page = read('junsong-ui-v3/src/views/system/delegate/index.vue')
  const domain = read('junsong-modules/junsong-system/src/main/java/com/junsong/system/domain/SysUserDelegate.java')
  const mapper = read('junsong-modules/junsong-system/src/main/resources/mapper/system/SysUserDelegateMapper.xml')

  assert.doesNotMatch(page, /\bgetUser\s*\(/)
  assert.match(page, /row\.userName/)
  assert.match(page, /row\.delegateUserName/)
  assert.match(domain, /private String userName;/)
  assert.match(domain, /private String delegateUserName;/)
  assert.match(mapper, /left join sys_user user_account/i)
  assert.match(mapper, /left join sys_user delegate_account/i)
})
