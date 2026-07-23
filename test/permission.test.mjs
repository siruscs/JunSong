import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const sourceUrl = new URL('../src/utils/permission.js', import.meta.url)
const source = await readFile(sourceUrl, 'utf8')
const testableSource = source.replace(
  "import { modules } from '@/config/modules.js'",
  `const modules = {
    sale: {
      permissions: {
        view: ['finance:sale:list', 'finance:sale:query'],
        add: 'finance:sale:add',
        edit: 'finance:sale:edit',
        remove: 'finance:sale:remove',
        payment: 'finance:sale:edit'
      }
    }
  }`
)

globalThis.uni = {
  getStorageSync(key) {
    if (key === 'modules') return ['sale']
    if (key === 'permissions') return []
    return []
  },
  showToast() {},
  navigateBack() {}
}

const permission = await import(`data:text/javascript;base64,${Buffer.from(testableSource).toString('base64')}`)

test('module grant allows page access but no sale actions', () => {
  assert.equal(permission.hasModulePermission('sale', ['sale']), true)
  assert.equal(permission.hasActionPermission('sale', 'add', ['sale']), false)
  assert.equal(permission.hasActionPermission('sale', 'edit', ['sale']), false)
  assert.equal(permission.hasActionPermission('sale', 'remove', ['sale']), false)
  assert.equal(permission.hasActionPermission('sale', 'payment', ['sale']), false)
})

test('action permission cannot grant access to a module entry', () => {
  assert.equal(permission.hasModulePermission('sale', ['finance:sale:edit']), false)
})

test('sale actions require their exact PC permission codes', () => {
  assert.equal(permission.hasActionPermission('sale', 'add', ['finance:sale:add']), true)
  assert.equal(permission.hasActionPermission('sale', 'add', ['finance:sale:edit']), false)
  assert.equal(permission.hasActionPermission('sale', 'edit', ['finance:sale:edit']), true)
  assert.equal(permission.hasActionPermission('sale', 'payment', ['finance:sale:edit']), true)
  assert.equal(permission.hasActionPermission('sale', 'remove', ['finance:sale:remove']), true)
})

test('administrator wildcard allows every configured action', () => {
  for (const action of ['view', 'add', 'edit', 'remove', 'payment']) {
    assert.equal(permission.hasActionPermission('sale', action, ['*:*:*']), true)
  }
})

test('exact permission checks do not accept adjacent query permission', () => {
  assert.equal(permission.hasExactPermission('finance:expense:list', ['finance:expense:list']), true)
  assert.equal(permission.hasExactPermission('finance:expense:list', ['finance:expense:query']), false)
  assert.equal(permission.hasExactPermission('finance:expense:list', ['*:*:*']), true)
})

test('action checks read permissions storage instead of modules storage by default', () => {
  assert.equal(permission.hasActionPermission('sale', 'edit'), false)
})
