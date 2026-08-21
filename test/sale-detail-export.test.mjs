import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const source = fs.readFileSync(new URL('../src/pages/detail/index.vue', import.meta.url), 'utf8')

test('sale detail exposes long image export without changing payment flow', () => {
  assert.match(source, /导出图片/)
  assert.match(source, /exportLongImage/)
  assert.match(source, /exportWxml\(\)/)
  assert.match(source, /wxml-to-canvas/)
  assert.match(source, /class="export-canvas"/)
  assert.match(source, /moduleKey === 'sale'/)
  assert.match(source, /payments/)
  assert.match(source, /sectionHeader/)
  assert.match(source, /detailList/)
  assert.match(source, /detailCard/)
})
