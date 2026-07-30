import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const form = fs.readFileSync(new URL('../src/pages/form/index.vue', import.meta.url), 'utf8')
const userForm = fs.readFileSync(new URL('../src/pages/user/form.vue', import.meta.url), 'utf8')
const deptForm = fs.readFileSync(new URL('../src/pages/dept/form.vue', import.meta.url), 'utf8')
const deptDetail = fs.readFileSync(new URL('../src/pages/dept/detail.vue', import.meta.url), 'utf8')
const workflow = fs.readFileSync(new URL('../src/pages/workflow/detail.vue', import.meta.url), 'utf8')
const profile = fs.readFileSync(new URL('../src/pages/profile/index.vue', import.meta.url), 'utf8')
const list = fs.readFileSync(new URL('../src/pages/list/index.vue', import.meta.url), 'utf8')
const detail = fs.readFileSync(new URL('../src/pages/detail/index.vue', import.meta.url), 'utf8')

test('generic add/edit form keeps synchronous submit guard for products and other modules', () => {
  const block = form.match(/async submit\(\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(block, /if \(this\.submitting \|\| this\.submitted\) return/)
  assert.match(form, /submitted: false/)
  assert.match(block, /this\.submitting \|\| this\.submitted/)
  assert.match(block, /this\.submitted = true/)
  assert.match(block, /this\.submitting = true/)
  assert.match(block, /finally \{[\s\S]*?this\.submitting = false/)
  assert.match(form, /:disabled="submitting"/)
})

test('independent user and dept add pages lock duplicate saves', () => {
  const userBlock = userForm.match(/async submit\(\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(userForm, /saving: false/)
  assert.match(userForm, /:disabled="saving"/)
  assert.match(userBlock, /if \(this\.saving \|\| this\.saved\) return/)
  assert.match(userBlock, /this\.saving = true/)
  assert.match(userBlock, /finally \{[\s\S]*?this\.saving = false/)

  const deptBlock = deptForm.match(/async submitForm\(\) \{([\s\S]*?)\n    \}/)?.[1] || ''
  assert.match(deptForm, /saving: false/)
  assert.match(deptForm, /:disabled="saving"/)
  assert.match(deptBlock, /if \(this\.saving \|\| this\.saved\) return/)
  assert.match(deptBlock, /this\.saving = true/)
  assert.match(deptBlock, /finally \{[\s\S]*?this\.saving = false/)
})

test('department add, edit, and detail views hide the leader field', () => {
  assert.doesNotMatch(deptForm, /<text class="form-label">负责人<\/text>/)
  assert.doesNotMatch(deptDetail, /<text class="form-label">负责人<\/text>/)
})

test('seckill claim and batch creation actions lock repeated taps', () => {
  assert.match(list, /claimSubmitting: false/)
  assert.match(list, /:disabled="claimSubmitting"/)
  assert.match(list, /if \(this\.claimSubmitting\) return/)
  assert.match(list, /this\.claimSubmitting = true/)
  assert.match(list, /finally \{[\s\S]*?this\.claimSubmitting = false/)
  assert.match(list, /if \(this\.batchAllLoading\) return/)

  assert.match(detail, /claimSubmitting: false/)
  assert.match(detail, /:disabled="claimSubmitting"/)
  assert.match(detail, /if \(this\.claimSubmitting\) return/)
  assert.match(detail, /this\.claimSubmitting = true/)
  assert.match(detail, /finally \{[\s\S]*?this\.claimSubmitting = false/)
})

test('delayed-success forms keep their write lock until navigation', () => {
  for (const source of [workflow, profile, userForm, deptForm]) {
    assert.match(source, /submitted: false|saved: false/)
    assert.match(source, /this\.(submitted|saved) = true/)
    assert.match(source, /this\.(submitting|saving) \|\| this\.(submitted|saved)/)
  }
})
