import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const formPage = fs.readFileSync(new URL('../src/pages/form/index.vue', import.meta.url), 'utf8')
const modules = fs.readFileSync(new URL('../src/config/modules.js', import.meta.url), 'utf8')

test('member card type selector requests enabled levels only', () => {
  const memberConfig = modules.match(/member: \{([\s\S]*?)(?=\n  pointsGoods: \{)/)?.[1] || ''
  assert.match(memberConfig, /remoteFilterStatus: '0'/)
  assert.match(formPage, /if \(field\.remoteFilterStatus\) params\.status = field\.remoteFilterStatus/)
  assert.match(formPage, /remoteFilterStatus \|\| ''/)
})

test('member creation result highlights member name and number in a custom success dialog', () => {
  assert.match(formPage, /v-if="memberCreateSuccess"/)
  assert.match(formPage, /class="member-success-name"/)
  assert.match(formPage, /class="member-success-no"/)
  assert.match(formPage, /font-size: 42rpx/)
  assert.match(formPage, /color: #DC2626/)
  assert.match(formPage, /\.member-success-confirm\s*\{[\s\S]*?width:\s*100%[\s\S]*?height:\s*78rpx[\s\S]*?line-height:\s*78rpx/)
  assert.doesNotMatch(formPage, /uni\.showModal\(\{[\s\S]*?title: '会员信息'[\s\S]*?content: `会员姓名：/)
})
