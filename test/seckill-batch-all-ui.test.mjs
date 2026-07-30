import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const page = fs.readFileSync(new URL('../src/pages/list/index.vue', import.meta.url), 'utf8')

test('all-member seckill quantity input matches the shared form control style', () => {
  assert.match(page, /class="batch-all-input"[^>]*v-model="batchAllForm\.shares"/)
  const style = page.match(/\.batch-all-input\s*\{([\s\S]*?)\n\}/)?.[1] || ''
  assert.match(style, /display:\s*block/)
  assert.match(style, /width:\s*100%/)
  assert.match(style, /height:\s*84rpx/)
  assert.match(style, /padding:\s*0 24rpx/)
  assert.match(style, /background:\s*#F5F8FA/)
  assert.match(style, /border:\s*2rpx solid #E2E8F0/)
  assert.match(style, /border-radius:\s*14rpx/)
  assert.match(style, /font-size:\s*28rpx/)
})
