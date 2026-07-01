import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const rootDir = path.resolve(import.meta.dirname, '..')

function readRepoFile(relativePath) {
  return fs.readFileSync(path.join(rootDir, relativePath), 'utf8')
}

test('navigation unread-count request opts into silent business errors', () => {
  const source = readRepoFile('junsong-ui-v3/src/api/system/notification.ts')

  assert.match(source, /function\s+getUnreadCount\s*\(\)/)
  assert.match(source, /url:\s*['"]\/system\/notification\/unread-count['"]/)
  assert.match(source, /silentError:\s*true/)
})

test('request interceptor suppresses global toast for silent business errors', () => {
  const source = readRepoFile('junsong-ui-v3/src/api/request.ts')

  assert.match(source, /silentError/)
  assert.match(source, /if\s*\(silentError\)\s*\{\s*return res\.data\s*\}/s)
  assert.match(source, /if\s*\(!silentError\)\s*\{\s*ElNotification\.error\(\{\s*title:\s*msg\s*\}\)/s)
  assert.match(source, /return Promise\.reject\('error'\)/)
})

test('request interceptor prefers backend permission detail over generic 403 text', () => {
  const source = readRepoFile('junsong-ui-v3/src/api/request.ts')

  assert.match(source, /const msg = res\.data\.msg \|\| errorCode\[code\] \|\| errorCode\['default'\]/)
})
