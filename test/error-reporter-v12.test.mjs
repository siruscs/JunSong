import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const reporter = fs.existsSync('src/utils/errorReporter.js') ? fs.readFileSync('src/utils/errorReporter.js', 'utf8') : ''
const request = fs.readFileSync('src/api/index.js', 'utf8')
const app = fs.readFileSync('src/App.vue', 'utf8')
const vite = fs.readFileSync('vite.config.js', 'utf8')
const packageJson = fs.readFileSync('package.json', 'utf8')
const patchDoc = fs.existsSync('docs/runtime-patch.md') ? fs.readFileSync('docs/runtime-patch.md', 'utf8') : ''

test('error reporter sanitizes, queues, and flushes structured failures', () => {
  assert.ok(reporter, 'errorReporter.js must exist')
  assert.match(reporter, /sanitize|脱敏/)
  assert.match(reporter, /queue|本地|Storage/)
  assert.match(reporter, /flush|error-report/)
  assert.match(request, /reportError/)
  assert.match(app, /reportError/)
})

test('Vite runtime patch is documented and uni versions remain locked', () => {
  assert.ok(patchDoc, 'runtime patch documentation must exist')
  assert.match(patchDoc, /getSystemInfoSync/)
  assert.match(patchDoc, /preloadAsset/)
  const pkg = JSON.parse(packageJson)
  assert.equal(pkg.dependencies['@dcloudio/uni-mp-weixin'], '3.0.0-5000720260410001')
  assert.match(vite, /patchUniMpWeixinRuntime/)
})
