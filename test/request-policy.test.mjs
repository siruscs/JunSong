import test from 'node:test'
import assert from 'node:assert/strict'

import { classifyRequestError, canRetryRequest } from '../src/utils/requestPolicy.js'

test('classifies timeout, network, permission, validation, and server failures', () => {
  assert.equal(classifyRequestError({ errMsg: 'request:fail timeout' }).kind, 'timeout')
  assert.equal(classifyRequestError({ errMsg: 'request:fail network' }).kind, 'network')
  assert.equal(classifyRequestError({ statusCode: 403 }).kind, 'permission')
  assert.equal(classifyRequestError({ statusCode: 400 }).kind, 'validation')
  assert.equal(classifyRequestError({ statusCode: 503 }).kind, 'server')
})

test('only retries safe read operations automatically', () => {
  assert.equal(canRetryRequest({ method: 'GET', kind: 'network' }), true)
  assert.equal(canRetryRequest({ method: 'POST', kind: 'network' }), false)
  assert.equal(canRetryRequest({ method: 'GET', kind: 'permission' }), false)
})
