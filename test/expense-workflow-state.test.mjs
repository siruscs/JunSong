import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const verifyPage = fs.readFileSync(new URL('../src/pages/expense-verify/index.vue', import.meta.url), 'utf8')
const detailPage = fs.readFileSync(new URL('../src/pages/detail/index.vue', import.meta.url), 'utf8')

test('verification loading failure is visible and retryable', () => {
  assert.match(verifyPage, /loadError:/)
  assert.match(verifyPage, /class="load-error"/)
  assert.match(verifyPage, /@tap="loadData"/)
  assert.match(verifyPage, /this\.loadError = ''/)
  assert.match(verifyPage, /this\.loadError = error\?\.(?:message|msg)/)
})

test('successful reload keeps only selectable advances and enables verification last', () => {
  const block = verifyPage.match(/async loadData\(\) \{([\s\S]*?)(?=\n    isAdvanceSelected)/)?.[1] || ''
  assert.match(block, /const availableAdvanceIds = new Set/)
  assert.match(block, /this\.selectedAdvanceIds = this\.selectedAdvanceIds\.filter/)
  const sanitizeIndex = block.indexOf('this.selectedAdvanceIds =')
  const readyIndex = block.indexOf('this.verificationReady = true')
  assert.ok(sanitizeIndex >= 0 && readyIndex > sanitizeIndex)
})

test('ambiguous verification replays the same frozen payload only after confirmation', () => {
  assert.match(verifyPage, /import \{ isUnknownWriteOutcome \}/)
  assert.match(verifyPage, /pendingVerifyPayload:/)
  assert.match(verifyPage, /this\.pendingVerifyPayload \|\| \{/)
  assert.match(verifyPage, /data: this\.pendingVerifyPayload/)
  assert.match(verifyPage, /isUnknownWriteOutcome\(error\)[\s\S]*?确认核销结果/)
  assert.match(verifyPage, /if \(modal\.confirm\)[\s\S]*?await this\.submitVerify\(\)[\s\S]*?return/)
})

test('ambiguous reversal preserves and replays pending request after confirmation', () => {
  const block = detailPage.match(/async openExpenseUnverify\(\) \{([\s\S]*?)(?=\n    async loadExpenseCapability)/)?.[1] || ''
  assert.match(detailPage, /import \{ isUnknownWriteOutcome \}/)
  assert.match(block, /isUnknownWriteOutcome\(error\)/)
  assert.match(block, /确认反核销结果/)
  assert.match(block, /if \(modal\.confirm\)[\s\S]*?await this\.openExpenseUnverify\(\)[\s\S]*?return/)
  assert.ok(block.indexOf('isUnknownWriteOutcome(error)') < block.indexOf('this.pendingUnverify = null', block.indexOf('catch (error)')))
})
