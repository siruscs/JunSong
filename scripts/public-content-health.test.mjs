import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const showcaseHtml = fs.readFileSync('junsong-ui-v3/public/showcase.html', 'utf8')
const openPortalVue = fs.readFileSync('junsong-ui-v3/src/views/open/portal/index.vue', 'utf8')

const forbiddenShowcaseTerms = [
  'JunSong-Cloud',
  'Release Candidate',
  'RC',
  'R25',
  'R1-R25',
  '开发完成',
  'health gate',
  '封版',
]

test('SHOWCASE uses public product positioning, not internal release language', () => {
  for (const term of forbiddenShowcaseTerms) {
    assert.equal(
      showcaseHtml.includes(term),
      false,
      `SHOWCASE must not expose internal release term: ${term}`,
    )
  }

  for (const term of ['连锁门店经营管理平台', '经营总览', '财务闭环', '会员增长', '开放连接']) {
    assert.equal(showcaseHtml.includes(term), true, `SHOWCASE should include public product term: ${term}`)
  }
})

test('OPEN PLATFORM homepage explains trusted external integration capabilities', () => {
  for (const term of ['可信开放平台', 'X-Open-*', '调用日志', 'Webhook', 'X-Inner-Token']) {
    assert.equal(openPortalVue.includes(term), true, `OPEN PLATFORM should explain: ${term}`)
  }
})
