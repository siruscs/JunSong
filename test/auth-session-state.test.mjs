import test from 'node:test'
import assert from 'node:assert/strict'

import {
  createAuthSession,
  isAuthExpiredResponse,
  shouldRecoverAuth
} from '../src/utils/authSession.js'

test('recognizes HTTP, business-code, and message authentication expiry', () => {
  assert.equal(isAuthExpiredResponse(401, {}), true)
  assert.equal(isAuthExpiredResponse(200, { code: 401 }), true)
  assert.equal(isAuthExpiredResponse(200, { msg: '登录已超时，请重新登录' }), true)
  assert.equal(isAuthExpiredResponse(500, { msg: '服务异常' }), false)
})

test('coalesces concurrent recovery attempts and allows a later recovery', async () => {
  let recoveries = 0
  let release
  const blocked = new Promise((resolve) => { release = resolve })
  const session = createAuthSession({
    recover: async () => {
      recoveries += 1
      await blocked
    }
  })

  const first = session.recoverOnce()
  const second = session.recoverOnce()
  assert.equal(first, second)
  assert.equal(recoveries, 1)

  release()
  await first
  await session.recoverOnce()
  assert.equal(recoveries, 2)
})

test('recovers only when the expired response belongs to the current token', () => {
  assert.equal(shouldRecoverAuth('old-token', 'old-token'), true)
  assert.equal(shouldRecoverAuth('old-token', ''), false)
  assert.equal(shouldRecoverAuth('old-token', 'new-token'), false)
})
