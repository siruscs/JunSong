import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('request adapter delegates authentication expiry detection to auth session', () => {
  const api = read('src/api/index.js')

  assert.match(api, /import \{ createAuthSession, isAuthExpiredResponse \} from '@\/utils\/authSession\.js'/)
  assert.doesNotMatch(api, /function isAuthExpiredResponse\(/)
})

test('request wrapper clears local session and delegates a single recovery on auth expiry', () => {
  const api = read('src/api/index.js')

  assert.match(api, /const authSession = createAuthSession\(/)
  assert.match(api, /authSession\.recoverOnce\(\)/)
  assert.doesNotMatch(api, /let authRedirecting\s*=\s*false/)
  assert.doesNotMatch(api, /function redirectToLogin\(/)
  assert.match(api, /uni\.removeStorageSync\('userInfo'\)/)
  assert.match(api, /uni\.removeStorageSync\('modules'\)/)
  assert.match(api, /uni\.removeStorageSync\('permissions'\)/)
  assert.match(api, /uni\.reLaunch\(\{[\s\S]*?url: '\/pages\/login\/index'/)
})

test('login cold start validates an existing token before entering index page', () => {
  const login = read('src/pages/login/index.vue')

  assert.match(login, /async onLoad\(\)/)
  assert.match(login, /await refreshAuthSession\(\{ noRedirect: true, timeout: 8000 \}\)/)
  assert.match(read('src/api/index.js'), /url: '\/auth\/refresh'/)
  assert.match(read('src/api/index.js'), /header: \{ isToken: true \}/)
  assert.match(login, /this\.clearSession\(\)/)
})

test('app foreground delegates token and context refresh to the shared coordinator', () => {
  const app = read('src/App.vue')

  assert.match(app, /import \{ refreshForegroundSession \} from '@\/utils\/foregroundSession\.js'/)
  assert.match(app, /onShow\(\) \{/)
  assert.match(app, /refreshForegroundSession\(\)/)
})
