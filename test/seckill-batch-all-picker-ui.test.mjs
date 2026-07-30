import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const page = fs.readFileSync(new URL('../src/pages/list/index.vue', import.meta.url), 'utf8')

test('all-member seckill pickers fill the available form width', () => {
  assert.match(page, /\.batch-all-row\s*>\s*picker\s*\{[\s\S]*?flex:\s*1[\s\S]*?width:\s*0/)
  const style = page.match(/\.batch-all-picker\s*\{([\s\S]*?)\n\}/)?.[1] || ''
  assert.match(style, /display:\s*block/)
  assert.match(style, /width:\s*100%/)
  assert.match(style, /height:\s*84rpx/)
  assert.match(style, /padding:\s*0 24rpx/)
  assert.match(style, /background:\s*#F5F8FA/)
  assert.match(style, /border:\s*2rpx solid #E2E8F0/)
})
