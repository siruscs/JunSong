import test from 'node:test'
import assert from 'node:assert/strict'
import { isUnknownWriteOutcome, resolveListState } from '../src/utils/operationState.js'

test('resolves loading, error, empty, and content list states', () => {
  assert.equal(resolveListState({ loading: true, rows: [] }), 'loading')
  assert.equal(resolveListState({ error: '加载失败', rows: [] }), 'error')
  assert.equal(resolveListState({ rows: [] }), 'empty')
  assert.equal(resolveListState({ rows: [{ id: 1 }] }), 'content')
})

test('only transport failures make a write result unknown', () => {
  assert.equal(isUnknownWriteOutcome({ code: 'REQUEST_TIMEOUT' }), true)
  assert.equal(isUnknownWriteOutcome({ code: 'NETWORK_ERROR' }), true)
  assert.equal(isUnknownWriteOutcome({ code: 403 }), false)
  assert.equal(isUnknownWriteOutcome({ code: 500, msg: '业务处理失败' }), false)
})
