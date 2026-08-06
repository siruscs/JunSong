import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(path, 'utf8')

test('member purchase inputs use explicit quantity and money precision with blank entry state', () => {
  const source = read('junsong-ui-v3/src/views/member/purchase/index.vue')
  assert.match(source, /purchaseQuantity[^\n]*precision="3"/)
  assert.match(source, /paymentAmount[^\n]*precision="2"/)
  assert.match(source, /purchaseQuantity:\s*undefined/)
  assert.match(source, /paymentAmount:\s*undefined/)
})

test('finance purchase and sale business inputs declare precision', () => {
  const purchase = read('junsong-ui-v3/src/views/finance/purchase/index.vue')
  const sale = read('junsong-ui-v3/src/views/finance/sale/index.vue')
  assert.match(purchase, /scope\.row\.quantity[^\n]*precision="3"/)
  assert.match(purchase, /scope\.row\.price[^\n]*precision="2"/)
  assert.match(sale, /form\.saleAmount[^\n]*precision="2"/)
  assert.match(sale, /paymentForm\.paymentAmount[^\n]*precision="2"/)
})

test('mini-program purchase detail amount input exposes money precision', () => {
  const source = read('junsong-miniprogram/src/pages/form/form-modules/PurchaseDetailsForm.vue')
  assert.match(source, /detail\.price[^\n]*placeholder="0\.00"/)
  assert.match(source, /detail\.quantity[^\n]*placeholder="0\.000"/)
})

test('remaining finance business entry inputs use blank defaults and explicit precision', () => {
  const sale = read('junsong-ui-v3/src/views/finance/sale/index.vue')
  const purchase = read('junsong-ui-v3/src/views/finance/purchase/index.vue')
  const stockInit = read('junsong-ui-v3/src/views/finance/stockInit/index.vue')
  const pointsGoods = read('junsong-ui-v3/src/views/member/pointsGoods/index.vue')
  assert.match(sale, /form\.saleAmount[^\n]*precision="2"[^\n]*placeholder="0\.00"/)
  assert.match(sale, /form\.saleQuantity[^\n]*precision="3"[^\n]*placeholder="0\.000"/)
  assert.match(sale, /saleAmount: undefined/)
  assert.match(sale, /saleQuantity: undefined/)
  assert.match(purchase, /form\.paidAmount[^\n]*precision="2"[^\n]*placeholder="0\.00"/)
  assert.match(purchase, /scope\.row\.quantity[^\n]*precision="3"[^\n]*placeholder="0\.000"/)
  assert.match(purchase, /quantity: undefined/)
  assert.match(stockInit, /v-model="scope\.row\.quantity"[\s\S]*?:precision="3"[\s\S]*?placeholder="0\.000"/)
  assert.match(stockInit, /v-model="scope\.row\.unitCost"[\s\S]*?:precision="2"[\s\S]*?placeholder="0\.00"/)
  assert.match(pointsGoods, /form\.pointsPrice[^\n]*precision="0"[^\n]*placeholder="请输入积分"/)
  assert.match(pointsGoods, /form\.stock[^\n]*precision="3"[^\n]*placeholder="0\.000"/)
})

test('mini-program stock and claim quantity inputs use digit mode with quantity placeholder', () => {
  const stockAdjustment = read('junsong-miniprogram/src/pages/stock-adjustment/index.vue')
  const fieldWork = read('junsong-miniprogram/src/pages/field-work/index.vue')
  const claim = read('junsong-miniprogram/src/pages/detail/index.vue')
  assert.match(stockAdjustment, /type="digit"[\s\S]*?v-model="item\.quantity"[\s\S]*?placeholder="0\.000"/)
  assert.match(fieldWork, /type="digit"[\s\S]*?v-model="takeForm\.actualQuantity"[\s\S]*?placeholder="0\.000"/)
  assert.match(claim, /type="digit"[\s\S]*?v-model="claimForm\.claimShares"[\s\S]*?placeholder="0\.000"/)
})

test('expense create amount is blank with money precision', () => {
  const source = read('junsong-ui-v3/src/views/finance/expense/index.vue')
  assert.match(source, /v-model="form\.expenseAmount"[^>]*precision="2"[^>]*placeholder="0\.00"/)
  assert.match(source, /expenseAmount: undefined/)
})
