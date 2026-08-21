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
  assert.match(source, /summary-meta/)
  assert.match(source, /核销时间：\{\{ formatTime\(batch\.verifyTime\) \|\| '-' \}\}/)
  assert.match(source, /核销状态：\{\{ statusText\(batch\.status\) \|\| '-' \}\}/)
  assert.doesNotMatch(source, /核销单号/)
  assert.doesNotMatch(source, /批次ID|费用合计|借支合计/)
})

test('verification detail uses the batch total for advances when generated rows exist', () => {
  const source = read('src/pages/verification-record/detail.vue')
  assert.match(source, /batch\.totalAdvanceAmount/)
  assert.match(source, /relationType.*RELATION_SOURCE|relationType.*SOURCE|generatedFlag/)
})

test('verification export template preserves the page card hierarchy', () => {
  const source = read('src/pages/verification-record/detail.vue')
  assert.doesNotMatch(source, /<view class="info-card">/)
  assert.doesNotMatch(source, /const infoCard =/)
  assert.match(source, /核销时间/)
  assert.match(source, /核销状态|状态/)
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

test('verification export explanation uses a gradient background and centered text', () => {
  const source = read('src/pages/verification-record/detail.vue')
  assert.match(source, /explanation: \{[\s\S]*?backgroundGradient:/)
  assert.match(source, /explanation: \{[\s\S]*?textAlign: 'center'/)
})
