import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const page = fs.readFileSync(new URL('../src/pages/list/index.vue', import.meta.url), 'utf8')

test('list distinguishes load failure from a successful empty result', () => {
  assert.match(page, /loadError:/)
  assert.match(page, /class="load-error"/)
  assert.match(page, /@tap="refresh"/)
  assert.match(page, /v-if="listState === 'empty'"/)
  assert.match(page, /v-if="listState === 'error'"/)
})

test('refresh queues the latest intent while a request is active', () => {
  const block = page.match(/async refresh\(\) \{([\s\S]*?)(?=\n    async loadMore)/)?.[1] || ''
  assert.match(block, /if \(this\.loading\) \{[\s\S]*?this\.refreshPending = true[\s\S]*?return/)
  assert.match(block, /try \{[\s\S]*?await this\.fetchList\(true\)[\s\S]*?\} finally \{[\s\S]*?this\.refreshing = false/)
})

test('failed pagination restores the previous page only in the same work context', () => {
  const block = page.match(/async loadMore\(\) \{([\s\S]*?)(?=\n    async fetchList)/)?.[1] || ''
  assert.match(block, /const previousPage = this\.pageNum/)
  assert.match(block, /const loaded = await this\.fetchList\(false\)/)
  assert.match(block, /shouldRestoreListPage\(loaded, workContext\.isCurrent\(contextVersion\)\)/)
  assert.match(block, /this\.pageNum = previousPage/)
})

test('first-page failure records an error without replacing existing pagination rows', () => {
  const block = page.match(/async fetchList\(reset\) \{([\s\S]*?)(?=\n    async loadClaimRows)/)?.[1] || ''
  assert.match(block, /catch \(error\)/)
  assert.match(block, /if \(this\.refreshPending\) return true/)
  assert.match(block, /if \(reset\) this\.loadError =/)
  assert.match(block, /return false/)
  assert.match(block, /finally \{[\s\S]*?this\.loading = false[\s\S]*?if \(this\.refreshPending\) \{[\s\S]*?this\.refreshPending = false[\s\S]*?await this\.refresh\(\)/)
  assert.doesNotMatch(block, /catch \(error\)[\s\S]*?this\.rows = \[\]/)
})
