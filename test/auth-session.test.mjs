import assert from 'node:assert/strict'
import test from 'node:test'
import { createSessionRestorer, extractAccessToken } from '../src/utils/authSession.js'

test('extracts a refreshed access token from common response shapes', () => {
  assert.equal(extractAccessToken({ data: { access_token: 'next-token' } }), 'next-token')
  assert.equal(extractAccessToken({ accessToken: 'next-access-token' }), 'next-access-token')
})

test('coalesces concurrent foreground session restores', async () => {
  let calls = 0
  let release
  const gate = new Promise((resolve) => { release = resolve })
  const restorer = createSessionRestorer({
    getToken: () => 'token',
    refresh: async () => { calls += 1; await gate; return 'context' }
  })

  const first = restorer.restoreSession()
  const second = restorer.restoreSession()
  assert.equal(first, second)
  release()
  assert.equal(await first, 'context')
  assert.equal(calls, 1)
})
