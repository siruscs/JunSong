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
  assert.match(source, /sectionCard/)
  assert.match(source, /highlightGrid/)
})

test('sale detail export cards keep the amount column inside the page width', () => {
  const style = source.match(/exportStyle\(\)\s*\{[\s\S]*?\n    \},\n    \/\/ 加载数据/)
  assert.ok(style, '销售详情必须存在完整导出样式')
  const exportStyle = style[0]
  assert.match(exportStyle, /sectionCard: \{ width: 347/)
  assert.match(exportStyle, /highlightItem: \{ width: 143, height: 54/)
  assert.match(exportStyle, /fieldRow: \{ width: 299, height: 48/)
  assert.match(exportStyle, /paymentHistoryItem: \{ width: 299, height: 112/)
  assert.match(exportStyle, /heroValue: \{ width: 315, height: 22/)
  assert.match(exportStyle, /paymentHistoryMetaText: \{ height: 18/)
  assert.match(exportStyle, /paymentHistoryDate: \{ width: 104/)
  assert.match(exportStyle, /paymentHistoryNo: \{ width: 299/)
  assert.match(exportStyle, /paymentHistoryAmountLine: \{ width: 299/)
})

test('sale detail export mirrors the page section and card hierarchy', () => {
  assert.match(source, /sectionCard/)
  assert.match(source, /highlightGrid/)
  assert.match(source, /highlightItem/)
  assert.match(source, /fieldRow/)
  assert.match(source, /paymentHistoryItem/)
})

test('sale detail export includes the total amount in the hero and explicit payment fields', () => {
  assert.match(source, /heroValue/)
  assert.match(source, /paymentHistoryAmount/)
  assert.match(source, /paymentHistoryMetaText/)
  assert.match(source, /paymentDateText\(payment\.createTime\)/)
  assert.match(source, /paymentHistoryAmountLine/)
})
