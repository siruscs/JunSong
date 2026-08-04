import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const form = fs.readFileSync('src/pages/form/index.vue', 'utf8')
const modules = fs.readFileSync('src/config/modules.js', 'utf8')
const stateView = fs.readFileSync('src/components/StateView.vue', 'utf8')
const purchaseForm = fs.readFileSync('src/pages/form/form-modules/PurchaseDetailsForm.vue', 'utf8')
const fieldForm = fs.existsSync('src/pages/form/form-modules/FormField.vue') ? fs.readFileSync('src/pages/form/form-modules/FormField.vue', 'utf8') : ''
const expenseForm = fs.existsSync('src/pages/form/form-modules/ExpenseForm.vue') ? fs.readFileSync('src/pages/form/form-modules/ExpenseForm.vue', 'utf8') : ''

test('generic form restores and autosaves non-sensitive drafts', () => {
  assert.match(form, /saveDraft/)
  assert.match(form, /loadDraft/)
  assert.match(form, /clearDraft/)
  assert.match(form, /handler\(value\)/)
  assert.match(form, /检测到未完成的填写/)
})

test('drafts are only restored for new records', () => {
  assert.match(form, /if \(!this\.id\)/)
  assert.match(form, /getCurrentUserId\(\)/)
  assert.match(form, /saveDraft\(this\.moduleKey, deptId, value, userId\)/)
  assert.match(form, /loadDraft\(this\.moduleKey, deptId, userId\)/)
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

test('required and optional fields share one reusable field renderer', () => {
  assert.ok(fieldForm, 'generic form field renderer must exist')
  assert.match(form, /FormField/)
  assert.match(form, /@set-value="setValueFromField\(field\.key, \$event\)"/)
  assert.match(form, /@select-value="selectValueFromField\(field, \$event\)"/)
  assert.match(form, /@input-value="inputValueFromField\(field\.key, \$event\)"/)
})

test('expense OCR controls are isolated in a reusable form module', () => {
  assert.ok(expenseForm, 'expense form module must exist')
  assert.match(form, /ExpenseForm/)
  assert.match(expenseForm, /choose-ocr-image/)
  assert.match(expenseForm, /ocr-loading/)
})

test('expense and purchase business validation remains guarded by the form shell', () => {
  assert.match(modules, /expense:\s*\{[\s\S]*?expenseContent[^\n]*required: true[\s\S]*?expenseAmount[^\n]*required: true/)
  assert.match(modules, /purchase:\s*\{[\s\S]*?supplierId[^\n]*required: true[\s\S]*?purchaseDate[^\n]*required: true/)
  assert.match(form, /this\.moduleKey === 'purchase'/)
  assert.match(form, /请添加商品明细/)
  assert.match(form, /请选择所有商品/)
  assert.match(form, /data\.details = data\.details\.map/)
  assert.match(form, /quantity: this\.toNum3\(d\.quantity, true\)/)
  assert.match(form, /amount: d\.amount === ''/)
})
