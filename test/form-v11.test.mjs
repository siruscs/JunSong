import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const form = fs.readFileSync('src/pages/form/index.vue', 'utf8')
const stateView = fs.readFileSync('src/components/StateView.vue', 'utf8')
const purchaseForm = fs.readFileSync('src/pages/form/form-modules/PurchaseDetailsForm.vue', 'utf8')

test('generic form restores and autosaves non-sensitive drafts', () => {
  assert.match(form, /saveDraft/)
  assert.match(form, /loadDraft/)
  assert.match(form, /clearDraft/)
  assert.match(form, /handler\(value\)/)
  assert.match(form, /检测到未完成的填写/)
})

test('drafts are only restored for new records', () => {
  assert.match(form, /if \(!this\.id\)/)
  assert.match(form, /saveDraft\(this\.moduleKey, deptId/)
})

test('shared state view provides loading empty and error states', () => {
  assert.match(stateView, /loading/)
  assert.match(stateView, /暂无数据/)
  assert.match(stateView, /加载失败/)
  assert.match(stateView, /retry/)
  assert.match(form, /formState/)
  assert.match(form, /@retry="retryInit"/)
})

test('purchase details are isolated in a reusable form module', () => {
  assert.match(form, /PurchaseDetailsForm/)
  assert.match(purchaseForm, /product-change/)
  assert.match(purchaseForm, /quantity-input/)
})
