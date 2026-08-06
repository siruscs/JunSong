import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (file) => fs.readFileSync(file, 'utf8')

test('小程序必须提供会员等级配置真实页面和独立权限', () => {
  const modules = read('src/config/modules.js')
  const pages = read('src/pages.json')
  assert.match(modules, /memberLevel:\s*\{[\s\S]*?customPage:\s*['"]\/pages\/member-level\/index['"]/)
  assert.match(modules, /member:level:list/)
  assert.match(modules, /member:level:add/)
  assert.match(modules, /member:level:edit/)
  assert.match(pages, /pages\/member-level\/index/)
  assert.match(read('src/pages/member-level/index.vue'), /等级名称/)
  assert.match(read('src/pages/member-level/index.vue'), /等级编码/)
})

test('会员配置 API 必须覆盖等级、销售政策和配置同步', () => {
  const levelApi = read('src/api/memberLevel.js')
  const policyApi = read('src/api/campaignPolicy.js')
  assert.match(levelApi, /\/member\/level\/list/)
  assert.match(levelApi, /\/member\/level/)
  assert.match(policyApi, /\/member\/campaign\/policy\/list/)
  assert.match(policyApi, /purchaseQuantity/)
  assert.match(policyApi, /giftQuantity/)
  assert.match(policyApi, /packagePrice/)
})
