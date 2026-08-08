import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(new URL('..', import.meta.url).pathname)
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

test('会员购买和销售政策入口必须是独立真实页面并纳入会员服务', () => {
  const modules = read('src/config/modules.js')
  assert.match(modules, /memberPurchase: \{[\s\S]*?customPage: '\/pages\/member-purchase\/index'/)
  assert.match(modules, /campaignPolicy: \{[\s\S]*?customPage: '\/pages\/campaign-policy\/index'/)
  assert.match(modules, /memberPurchaseReturn: \{[\s\S]*?customPage: '\/pages\/member-purchase-return\/index'/)
  assert.match(modules, /\['member', 'memberPurchase', 'memberPurchaseReturn', 'memberLevel', 'campaignPolicy'/)
  assert.match(modules, /member:purchase:payment/)
  assert.match(modules, /member:purchase:delivery/)
})

test('小程序会员购买和销售政策页面必须有真实表单字段与精度约束', () => {
  const purchase = read('src/pages/member-purchase/index.vue')
  const policy = read('src/pages/campaign-policy/form.vue')
  assert.match(purchase, /购买日期/)
  assert.match(purchase, /核算周期/)
  assert.match(purchase, /收款/)
  assert.match(purchase, /领取/)
  assert.match(purchase, /openReturn/)
  assert.match(purchase, /purchaseQuantity[\s\S]*limit\('purchaseQuantity',[\s\S]*, 3\)/)
  assert.match(purchase, /unitPrice[\s\S]*limit\('unitPrice',[\s\S]*, 2\)/)
  assert.match(policy, /套餐档位/)
  assert.match(policy, /purchaseQuantity/)
  assert.match(policy, /giftQuantity/)
  assert.match(policy, /packagePrice/)
  const returnPage = read('src/pages/member-purchase-return/index.vue')
  assert.match(returnPage, /退货办理核算周期/)
  assert.match(returnPage, /退正品/)
  assert.match(returnPage, /退赠品/)
})

test('跨机构同步显示标准中文日期时间，不能直接输出 ISO T 格式', () => {
  const sync = read('src/pages/config-sync/index.vue')
  assert.match(sync, /formatDateTime\(period\.startTime\)/)
  assert.doesNotMatch(sync, /period\.startTime \|\| '-'/)
})
