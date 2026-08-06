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

test('会员购买新建页必须复用 MemberSearch 组件和统一表单字段组件', () => {
  const purchase = read('src/pages/member-purchase/index.vue')
  assert.match(purchase, /FormField/)
  assert.match(purchase, /import MemberSearch from '@\/components\/MemberSearch\/index\.vue'/)
  assert.match(purchase, /<MemberSearch/)
  assert.match(purchase, /@select/)
  assert.match(purchase, /@clear/)
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

test('会员购买页筛选区域支持顾客类型、收款状态、日期范围与重置', () => {
  const purchase = read('src/pages/member-purchase/index.vue')
  assert.match(purchase, /customerTypeFilters/)
  assert.match(purchase, /paymentStatusFilters/)
  assert.match(purchase, /beginTime/)
  assert.match(purchase, /endTime/)
  assert.match(purchase, /resetFilters/)
})

test('会员购买页调用统计接口并展示汇总卡片', () => {
  const purchase = read('src/pages/member-purchase/index.vue')
  assert.match(purchase, /\/member\/purchase\/statistics/)
  assert.match(purchase, /loadStatistics/)
  assert.match(purchase, /purchaseOrderCount/)
  assert.match(purchase, /giftQuantity/)
})

test('会员购买页使用分页参数 pageSize=20 并基于 total 计算总页数', () => {
  const purchase = read('src/pages/member-purchase/index.vue')
  assert.match(purchase, /pageSize:\s*20/)
  assert.match(purchase, /pageNum/)
  assert.match(purchase, /totalPages/)
  assert.doesNotMatch(purchase, /pageSize:\s*50/)
})

test('会员购买页散客单支持绑定会员并调用 bind-member 接口', () => {
  const purchase = read('src/pages/member-purchase/index.vue')
  assert.match(purchase, /bind-member/)
  assert.match(purchase, /openBind/)
  assert.match(purchase, /confirmBind/)
  assert.match(purchase, /WALK_IN/)
  assert.match(purchase, /can\('bind'\)/)
})

test('会员购买页有退货记录时显示退货详情入口', () => {
  const purchase = read('src/pages/member-purchase/index.vue')
  assert.match(purchase, /退货详情/)
  assert.match(purchase, /member-purchase-return/)
  assert.match(purchase, /hasReturn/)
})

test('会员购买页数量与金额输入占位符符合规范', () => {
  const purchase = read('src/pages/member-purchase/index.vue')
  assert.match(purchase, /placeholder="0\.000"/)
  assert.match(purchase, /placeholder="0\.00"/)
})

test('会员购买页创建购买单时 purchaseDate 发送日期字符串而非 ISO 时间', () => {
  const purchase = read('src/pages/member-purchase/index.vue')
  assert.doesNotMatch(purchase, /purchaseDate:this\.isoDateTime\(f\.purchaseDate\)/)
})
