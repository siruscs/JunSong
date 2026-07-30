import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const modules = fs.readFileSync(new URL('../src/config/modules.js', import.meta.url), 'utf8')
const form = fs.readFileSync(new URL('../src/pages/form/index.vue', import.meta.url), 'utf8')
const detail = fs.readFileSync(new URL('../src/pages/detail/index.vue', import.meta.url), 'utf8')

test('核算周期仅允许调整起始时间', () => {
  assert.match(modules, /accountingPeriod:[\s\S]*readonlyFields:\s*\[[^\]]*'endTime'/)
  assert.match(form, /config\?\.readonlyFields\?\.includes\(field\.key\)/)
  assert.match(detail, /canEdit\(\)\s*\{\s*if \(this\.moduleKey === 'accountingPeriod'\) return false/)
})
