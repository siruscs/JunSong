import assert from 'node:assert/strict'
import test from 'node:test'
import { normalizeListResponse, normalizeObjectResponse } from '../src/utils/responseNormalize.js'

test('normalizes common paged response shapes', () => {
  assert.deepEqual(normalizeListResponse({ data: { rows: [{ id: 1 }], total: 1 } }), { rows: [{ id: 1 }], total: 1 })
  assert.deepEqual(normalizeListResponse({ rows: [{ id: 2 }], total: 2 }), { rows: [{ id: 2 }], total: 2 })
  assert.deepEqual(normalizeListResponse(null), { rows: [], total: 0 })
})

test('normalizes nullable object response without losing fields', () => {
  assert.deepEqual(normalizeObjectResponse({ data: { id: 1 } }), { id: 1 })
  assert.deepEqual(normalizeObjectResponse({ id: 2 }), { id: 2 })
  assert.deepEqual(normalizeObjectResponse(null), {})
})
