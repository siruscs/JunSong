import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const api = fs.readFileSync(new URL('../src/api/index.js', import.meta.url), 'utf8')

test('request captures work-context version and exposes stale response marker', () => {
  assert.match(api, /import \{ workContext \} from '@\/utils\/workContext\.js'/)
  assert.match(api, /const contextSnapshot = workContext\.snapshot\(\)/)
  assert.match(api, /const contextVersion = contextSnapshot\.version/)
  assert.match(api, /staleContext: !workContext\.isCurrent\(contextVersion\)/)
})

test('context metadata is opt-in and records the captured department id', () => {
  assert.match(api, /options\.withContextMeta\s*\?\s*\{/)
  assert.match(api, /currentDeptId: contextSnapshot\.currentDeptId/)
  assert.match(api, /:\s*data/)
})

test('request failures use the shared policy without automatic retries', () => {
  assert.match(api, /import \{ classifyRequestError \} from '@\/utils\/requestPolicy\.js'/)
  assert.match(api, /const classified = classifyRequestError\(/)
  assert.doesNotMatch(api, /canRetryRequest/)
  assert.doesNotMatch(api, /retry/i)
})

test('authentication recovery ignores staggered expiry responses from an old token', () => {
  assert.match(api, /import \{[^}]*shouldRecoverAuth[^}]*\} from '@\/utils\/authSession\.js'/s)
  assert.match(api, /const requestToken = getToken\(\)/)
  assert.match(api, /shouldRecoverAuth\(requestToken, getToken\(\)\)/)
})
