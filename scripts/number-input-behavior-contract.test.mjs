import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

test('global number input behavior clears zero defaults and exposes a placeholder', () => {
  const source = fs.readFileSync('junsong-ui-v3/src/directives/numberInput.ts', 'utf8')
  assert.match(source, /placeholder.*0\.00/)
  assert.match(source, /\.el-input-number input/)
  assert.match(source, /dispatchEvent\(new Event\('input'/)
})

test('campaign policy package quantity uses blank initial values and three decimals', () => {
  const source = fs.readFileSync('junsong-ui-v3/src/views/member/campaignPolicy/index.vue', 'utf8')
  assert.match(source, /v-model="item\.purchaseQuantity"[^>]*:precision="3"/)
  assert.match(source, /v-model="item\.giftQuantity"[^>]*:precision="3"/)
  assert.match(source, /purchaseQuantity: undefined/)
  assert.match(source, /giftQuantity: undefined/)
  assert.match(source, /totalQuantity: undefined/)
})
