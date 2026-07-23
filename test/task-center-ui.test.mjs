import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const page = fs.readFileSync(new URL('../src/pages/workflow/todo.vue', import.meta.url), 'utf8')
const modules = fs.readFileSync(new URL('../src/config/modules.js', import.meta.url), 'utf8')

test('todo page presents a filterable unified task center', () => {
  const todoModule = modules.match(/wfTodo: \{([\s\S]*?)(?=\n  wfDone: \{)/)?.[1] || ''
  assert.match(todoModule, /title: '任务中心'/)
  assert.match(page, /任务中心/)
  assert.match(page, /filterTabs/)
  assert.match(page, /全部任务/)
  assert.match(page, /审批任务/)
  assert.match(page, /待核销/)
  assert.match(page, /filteredRows/)
  assert.match(page, /class="load-error"/)
  assert.match(page, /class="partial-notice"/)
  assert.match(page, /@tap="refresh"/)
})

test('pending expense loading is capability gated and merged through the task model', () => {
  assert.match(page, /hasExactPermission\('finance:expense:list'\)/)
  assert.match(page, /hasActionPermission\('expense', 'verify'\)/)
  assert.match(page, /async loadAllExpenseTasks\(\)/)
  assert.match(page, /while \(pageNum <= totalPages\)/)
  assert.match(page, /listData\('\/finance\/expense',[\s\S]*?pageNum[\s\S]*?status: '0'/)
  assert.match(page, /Promise\.allSettled/)
  assert.match(page, /buildTaskCenterItems\(\{ approvals, expenses, preserveOrder: this\.isDone \}\)/)
})

test('latest refresh wins and completed history keeps backend order', () => {
  assert.match(page, /requestVersion:/)
  const fetchList = page.match(/async fetchList\(\) \{([\s\S]*?)(?=\n    openTask)/)?.[1] || ''
  assert.match(fetchList, /const requestVersion = \+\+this\.requestVersion/)
  assert.match(fetchList, /if \(requestVersion !== this\.requestVersion\) return/)
  assert.match(fetchList, /preserveOrder: this\.isDone/)
  const removeTask = page.match(/removeTask\(taskId\) \{([\s\S]*?)(?=\n    \})/)?.[1] || ''
  assert.match(removeTask, /this\.requestVersion \+= 1/)
})

test('task types route to their authoritative handling pages', () => {
  const openTask = page.match(/openTask\(item\) \{([\s\S]*?)(?=\n    quickApprove)/)?.[1] || ''
  assert.match(openTask, /item\.type === 'verification'/)
  assert.match(openTask, /\/pages\/expense-verify\/index\?expenseIds=/)
  assert.match(openTask, /\/pages\/workflow\/detail\?taskId=/)
  assert.match(page, /v-if="!isDone && item\.type === 'approval'"/)
})
