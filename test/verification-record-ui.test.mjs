import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

for (const path of ['src/pages/verification-record/index.vue', 'src/pages/verification-record/detail.vue']) {
  test(`${path} uses the verification summary and detail export rules`, () => {
    const source = read(path)
    if (path.endsWith('/index.vue')) {
      assert.match(source, /核销记录/)
      assert.doesNotMatch(source, /导出图片/)
      assert.doesNotMatch(source, /exportLongImage/)
    } else {
      assert.match(source, /summary-card|核销汇总/)
      assert.match(source, /费用/)
      assert.match(source, /借支/)
      assert.match(source, /导出图片/)
      assert.match(source, /exportLongImage/)
      assert.match(source, /exportWxml\(\)/)
    }
  })
}

test('verification detail batch info keeps only the requested fields', () => {
  const source = read('src/pages/verification-record/detail.vue')
  assert.match(source, /费用（\{\{ expenseDetails\.length \}\}笔）/)
  assert.match(source, /借支（\{\{ advanceDetails\.length \}\}笔）/)
  assert.match(source, /差额/)
  assert.match(source, /核销单号/)
  const infoCard = source.match(/<!-- 批次信息卡片 -->([\s\S]*?)<!-- 费用明细 -->/)?.[1] || ''
  assert.doesNotMatch(infoCard, /批次ID/)
  assert.doesNotMatch(infoCard, /费用合计/)
  assert.doesNotMatch(infoCard, /借支合计/)
  assert.doesNotMatch(infoCard, /差额</)
})

test('verification export template preserves the page card hierarchy', () => {
  const source = read('src/pages/verification-record/detail.vue')
  assert.match(source, /infoCard/)
  assert.match(source, /infoHeader/)
  assert.match(source, /statusTag/)
  assert.match(source, /sectionHeader/)
  assert.match(source, /detailList/)
  assert.match(source, /detailCard/)
})

test('verification export layout gives dynamic sections and lists explicit heights', () => {
  const source = read('src/pages/verification-record/detail.vue')
  assert.match(source, /expenseSection/)
  assert.match(source, /advanceSection/)
  assert.match(source, /expenseList/)
  assert.match(source, /advanceList/)
  assert.match(source, /expenseSection: \{[^}]*height:/s)
  assert.match(source, /advanceSection: \{[^}]*height:/s)
})
