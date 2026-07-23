import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync('junsong-ui-v3/src/views/system/overview/index.vue', 'utf8')

test('system overview renders service health as tiles without an inner scrolling table', () => {
  assert.match(source, /class="service-tiles"/, 'service health must use a tiled container')
  assert.match(source, /class="service-tile"/, 'each service must render as a service tile')
  assert.doesNotMatch(
    source,
    /<el-table[^>]*:data="health\.services \|\| \[\]"[^>]*max-height=/,
    'service health must not use a max-height table that hides services behind inner scrolling'
  )
})

test('system overview service tiles keep status, service code, and operator message visible', () => {
  assert.match(source, /serviceStatusText/, 'tiles must render a normalized status label')
  assert.match(source, /service\.code/, 'tiles must keep the service code visible for diagnosis')
  assert.match(source, /service\.message/, 'tiles must keep the health message visible')
})
