import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (file) => fs.readFileSync(file, 'utf8')

test('会员购买入口允许拥有购买操作权限的用户进入', () => {
  const permission = read('src/utils/permission.js')
  assert.match(permission, /hasModuleOrActionPermission/)
  assert.match(permission, /Object\.keys\(modules\[moduleKey\]\?\.permissions \|\| \{\}\)/)
  assert.match(permission, /actions\.some\(\(action\) => hasActionPermission\(moduleKey, action/)
})

test('会员购买新建页必须复用会员查找组件和统一表单字段组件', () => {
  const purchase = read('src/pages/member-purchase/index.vue')
  assert.match(purchase, /FormField/)
  assert.match(purchase, /member-search-section/)
  assert.match(purchase, /memberKeyword/)
  assert.match(purchase, /searchMembers/)
  assert.match(purchase, /selectMember/)
})

test('会员购买政策必须按机构、周期、商品加载并带出套餐档位', () => {
  const purchase = read('src/pages/member-purchase/index.vue')
  assert.match(purchase, /\/member\/campaign\/policy\/list/)
  assert.match(purchase, /productId/)
  assert.match(purchase, /periodId/)
  assert.match(purchase, /packages/)
  assert.match(purchase, /policyId.*detail|campaign\/policy\//)
})
