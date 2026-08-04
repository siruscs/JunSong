import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const deptForm = fs.readFileSync('src/pages/dept/form.vue', 'utf8')
const userForm = fs.readFileSync('src/pages/user/form.vue', 'utf8')
const adjustment = fs.readFileSync('src/pages/stock-adjustment/index.vue', 'utf8')
const detail = fs.readFileSync('src/pages/detail/index.vue', 'utf8')
const stocktakeDetail = fs.readFileSync('src/pages/stocktake/detail.vue', 'utf8')

test('department edit has an explicit initialization gate and empty-detail guard', () => {
  assert.match(deptForm, /initializing: false/)
  assert.match(deptForm, /this\.initializing = true/)
  assert.match(deptForm, /finally \{ this\.initializing = false \}/)
  assert.match(deptForm, /部门详情为空，无法编辑/)
})

test('user edit waits for dependencies and detail before becoming interactive', () => {
  assert.match(userForm, /initializing: false/)
  assert.match(userForm, /Promise\.all\(\[this\.loadRoles\(\), this\.loadDepts\(\), this\.id \? this\.loadUser\(\) : Promise\.resolve\(\)\]\)/)
  assert.match(userForm, /finally \{ this\.initializing = false \}/)
  assert.match(userForm, /用户详情为空，无法编辑/)
})

test('stock adjustment editor and detail expose independent loading states', () => {
  assert.match(adjustment, /editorLoading: false/)
  assert.match(adjustment, /detailLoading: false/)
  assert.match(adjustment, /this\.editorLoading = false/)
  assert.match(adjustment, /this\.detailLoading = false/)
  assert.match(adjustment, /调整单详情为空，无法编辑/)
})

test('shared detail pages always release their loading state', () => {
  assert.match(detail, /finally \{\s*this\.loading = false\s*\}/)
  assert.match(stocktakeDetail, /finally \{\s*this\.loading = false\s*\}/)
})
