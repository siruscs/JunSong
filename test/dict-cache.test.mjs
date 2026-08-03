import assert from 'node:assert/strict'
import test from 'node:test'
import { createDictCache } from '../src/utils/dictCache.js'

test('loads a dictionary once within its ttl', async () => {
  let calls = 0
  const cache = createDictCache({ ttl: 600000, now: () => 1000 })
  const fetcher = async () => { calls += 1; return [{ value: 'OPENING_STOCK', label: '期初库存' }] }

  assert.deepEqual(await cache.get('finance_stock_adjustment_type', fetcher), [{ value: 'OPENING_STOCK', label: '期初库存' }])
  assert.deepEqual(await cache.get('finance_stock_adjustment_type', fetcher), [{ value: 'OPENING_STOCK', label: '期初库存' }])
  assert.equal(calls, 1)
})

test('uses stale cached data when a refresh fails', async () => {
  let now = 1000
  const cache = createDictCache({ ttl: 1, now: () => now })
  const fetcher = async () => [{ value: 'OTHER', label: '其他' }]
  await cache.get('finance_stock_adjustment_type', fetcher)
  now = 2000
  const result = await cache.get('finance_stock_adjustment_type', async () => { throw new Error('offline') })
  assert.deepEqual(result, [{ value: 'OTHER', label: '其他' }])
})

test('coalesces concurrent dictionary requests', async () => {
  let calls = 0
  let resolveFetch
  const cache = createDictCache()
  const fetcher = () => {
    calls += 1
    return new Promise(resolve => { resolveFetch = resolve })
  }
  const first = cache.get('finance_stock_adjustment_type', fetcher)
  const second = cache.get('finance_stock_adjustment_type', fetcher)
  await Promise.resolve()
  resolveFetch([{ value: 'OTHER', label: '其他' }])
  assert.deepEqual(await Promise.all([first, second]), [[{ value: 'OTHER', label: '其他' }], [{ value: 'OTHER', label: '其他' }]])
  assert.equal(calls, 1)
})
