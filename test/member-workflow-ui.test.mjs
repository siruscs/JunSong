import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const listPage = fs.readFileSync(new URL('../src/pages/list/index.vue', import.meta.url), 'utf8')
const formPage = fs.readFileSync(new URL('../src/pages/form/index.vue', import.meta.url), 'utf8')
const modules = fs.readFileSync(new URL('../src/config/modules.js', import.meta.url), 'utf8')

test('member list routes names, numbers, and mobile searches through member workflow rules', () => {
  assert.match(listPage, /import \{ resolveMemberSearchField \} from '@\/utils\/memberWorkflow\.js'/)
  assert.match(listPage, /if \(this\.moduleKey === 'member'\) return '输入姓名、编号或手机号'/)
  const buildQuery = listPage.match(/buildQuery\(\) \{([\s\S]*?)(?=\n    async refresh)/)?.[1] || ''
  assert.match(buildQuery, /if \(this\.moduleKey === 'member'\) \{[\s\S]*?query\[resolveMemberSearchField\(val\)\] = val/)
  const fetchList = listPage.match(/async fetchList\(reset\) \{([\s\S]*?)(?=\n    async loadClaimRows)/)?.[1] || ''
  assert.match(fetchList, /if \(this\.moduleKey === 'member' && query\.phone && list\.length === 0\)/)
  assert.match(fetchList, /delete fallbackQuery\.phone[\s\S]*?fallbackQuery\.memberNo = query\.phone[\s\S]*?listData\(this\.config\.path, fallbackQuery\)/)
})

test('new member number is optional, read-only, and described as server generated', () => {
  const memberConfig = modules.match(/member: \{([\s\S]*?)(?=\n  pointsGoods: \{)/)?.[1] || ''
  assert.match(memberConfig, /key: 'memberNo',[^\n]*serverGenerated: true/)
  assert.doesNotMatch(memberConfig, /key: 'memberNo',[^\n]*required: true/)
  assert.match(formPage, /this\.moduleKey === 'member' && !this\.id && field\.serverGenerated/)
  assert.match(formPage, /fieldPlaceholder\(field\)/)
  assert.match(formPage, /return '系统自动生成'/)
})

test('member number is generated only when the backend saves and member contacts are validated', () => {
  assert.doesNotMatch(formPage, /loadNextMemberNo/)
  const submitData = formPage.match(/buildSubmitData\(\) \{([\s\S]*?)(?=\n    async submit)/)?.[1] || ''
  assert.match(submitData, /if \(this\.moduleKey === 'member' && !this\.id\) delete data\.memberNo/)
  assert.match(formPage, /import \{ validateMemberContact \} from '@\/utils\/memberWorkflow\.js'/)
  const validate = formPage.match(/validate\(\) \{([\s\S]*?)(?=\n    async preview)/)?.[1] || ''
  assert.match(validate, /if \(this\.moduleKey === 'member'\) \{[\s\S]*?validateMemberContact\(this\.form\)[\s\S]*?showToast/)
})
