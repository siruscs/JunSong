import test from 'node:test'
import assert from 'node:assert/strict'

import { createForegroundSessionCoordinator } from '../src/utils/foregroundSession.js'

const deferred = () => {
  let resolve
  const promise = new Promise((done) => { resolve = done })
  return { promise, resolve }
}

test('concurrent app and page refreshes share one auth and context cycle', async () => {
  const auth = deferred()
  const context = deferred()
  const events = []
  let authCalls = 0
  let contextCalls = 0
  const coordinator = createForegroundSessionCoordinator({
    hasToken: () => true,
    refreshAuth: async () => {
      authCalls += 1
      events.push('auth:start')
      await auth.promise
      events.push('auth:end')
    },
    refreshContext: async () => {
      contextCalls += 1
      events.push('context:start')
      await context.promise
      events.push('context:end')
      return 'ready'
    }
  })

  const appRefresh = coordinator.refresh().then(() => events.push('app:continue'))
  const pageRefresh = coordinator.refresh().then(() => events.push('page:continue'))

  assert.equal(authCalls, 1)
  assert.equal(contextCalls, 0)
  auth.resolve()
  await Promise.resolve()
  await Promise.resolve()
  assert.equal(contextCalls, 1)
  assert.deepEqual(events, ['auth:start', 'auth:end', 'context:start'])

  context.resolve()
  await Promise.all([appRefresh, pageRefresh])
  assert.equal(authCalls, 1)
  assert.equal(contextCalls, 1)
  assert.deepEqual(events, [
    'auth:start', 'auth:end', 'context:start', 'context:end',
    'app:continue', 'page:continue'
  ])
})

test('a completed foreground refresh allows a new cycle', async () => {
  let authCalls = 0
  let contextCalls = 0
  const coordinator = createForegroundSessionCoordinator({
    hasToken: () => true,
    refreshAuth: async () => { authCalls += 1 },
    refreshContext: async () => { contextCalls += 1 }
  })

  await coordinator.refresh()
  await coordinator.refresh()

  assert.equal(authCalls, 2)
  assert.equal(contextCalls, 2)
})

test('foreground refresh without a token returns null without running work', async () => {
  let calls = 0
  const coordinator = createForegroundSessionCoordinator({
    hasToken: () => false,
    refreshAuth: async () => { calls += 1 },
    refreshContext: async () => { calls += 1 }
  })

  assert.equal(await coordinator.refresh(), null)
  assert.equal(calls, 0)
})
