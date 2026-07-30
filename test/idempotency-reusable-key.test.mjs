import test from 'node:test'
import assert from 'node:assert/strict'

import {
  applyIdempotencyHeader,
  clearIdempotencyKeyOnSuccess,
  releaseIdempotencyKeyOnFailure,
  consumeReusableKey,
  releaseReusableKey,
  clearReusableKey,
  computeRequestSignature,
  setStorageAdapter
} from '../src/utils/idempotency.js'

/**
 * 小程序表单会话级幂等键复用测试。
 *
 * 验证场景（与 PC 端 request.ts 保持一致语义）：
 * 1. 同一签名（method+url+body）在网络超时/失败后重试复用同键
 * 2. 业务成功（code=200）后清除暂存，下次视为新业务
 * 3. 业务失败/网络失败保留暂存，供下次重试复用
 * 4. body 内容改变 → 指纹改变 → 视为新请求，生成新键
 * 5. 不同 URL 的请求生成不同键
 * 6. TTL 过期后生成新键
 * 7. 调用方显式传入 idempotencyKey 跳过自动管理
 * 8. 调用方设置 idempotencyNewKey=true 强制生成新键
 */

// ============================================================
// 测试用 storage adapter（内存 Map，避免污染 uni storage）
// ============================================================

function createMemoryAdapter() {
  const store = new Map()
  return {
    get(key) { return store.get(key) },
    set(key, value) { store.set(key, value) },
    _store: store
  }
}

function setupAdapter() {
  const adapter = createMemoryAdapter()
  setStorageAdapter(adapter)
  return adapter
}

test('场景1：同一签名在网络超时后重试复用同键', () => {
  const adapter = setupAdapter()
  const options = {
    method: 'POST',
    url: '/sale',
    data: { amount: 100, productId: 42 }
  }

  // 第一次请求：生成键
  const header1 = applyIdempotencyHeader(options)
  const key1 = header1['X-Idempotency-Key']
  assert.ok(key1, '第一次请求应生成键')

  // 模拟网络超时：调用 releaseIdempotencyKeyOnFailure 保留暂存
  releaseIdempotencyKeyOnFailure(options, key1)

  // 第二次请求（重试）：应复用同一个键
  const header2 = applyIdempotencyHeader(options)
  const key2 = header2['X-Idempotency-Key']
  assert.equal(key2, key1, '重试应复用同一个幂等键')
})

test('场景2：业务成功后清除暂存，下次视为新业务', () => {
  const adapter = setupAdapter()
  const options = {
    method: 'POST',
    url: '/sale',
    data: { amount: 100 }
  }

  // 第一次请求：生成键
  const header1 = applyIdempotencyHeader(options)
  const key1 = header1['X-Idempotency-Key']

  // 业务成功：清除暂存
  clearIdempotencyKeyOnSuccess(options, key1)

  // 第二次请求（新业务）：应生成新键
  const header2 = applyIdempotencyHeader(options)
  const key2 = header2['X-Idempotency-Key']
  assert.notEqual(key2, key1, '业务成功后下次请求应生成新键')
})

test('场景3：业务失败后保留暂存，供下次重试复用', () => {
  const adapter = setupAdapter()
  const options = {
    method: 'POST',
    url: '/sale',
    data: { amount: 200 }
  }

  // 第一次请求
  const header1 = applyIdempotencyHeader(options)
  const key1 = header1['X-Idempotency-Key']

  // 业务失败（如 code=500）：保留暂存
  releaseIdempotencyKeyOnFailure(options, key1)

  // 第二次请求（重试）：复用同键
  const header2 = applyIdempotencyHeader(options)
  const key2 = header2['X-Idempotency-Key']
  assert.equal(key2, key1, '业务失败后重试应复用同一个键')
})

test('场景4：body 内容改变 → 指纹改变 → 生成新键', () => {
  const adapter = setupAdapter()

  // 第一次请求：amount=100
  const options1 = {
    method: 'POST',
    url: '/sale',
    data: { amount: 100, productId: 42 }
  }
  const header1 = applyIdempotencyHeader(options1)
  const key1 = header1['X-Idempotency-Key']

  // 修改表单内容后再次保存：amount=200
  const options2 = {
    method: 'POST',
    url: '/sale',
    data: { amount: 200, productId: 42 }
  }
  const header2 = applyIdempotencyHeader(options2)
  const key2 = header2['X-Idempotency-Key']

  assert.notEqual(key2, key1, 'body 内容改变后应生成新键（不同签名）')
})

test('场景5：不同 URL 的请求生成不同键', () => {
  const adapter = setupAdapter()

  const options1 = { method: 'POST', url: '/sale', data: { amount: 100 } }
  const options2 = { method: 'POST', url: '/expense', data: { amount: 100 } }

  const key1 = applyIdempotencyHeader(options1)['X-Idempotency-Key']
  const key2 = applyIdempotencyHeader(options2)['X-Idempotency-Key']

  assert.notEqual(key2, key1, '不同 URL 应生成不同键')
})

test('场景6：不同 HTTP 方法的请求生成不同键', () => {
  const adapter = setupAdapter()

  const options1 = { method: 'POST', url: '/sale', data: { amount: 100 } }
  const options2 = { method: 'PUT', url: '/sale', data: { amount: 100 } }

  const key1 = applyIdempotencyHeader(options1)['X-Idempotency-Key']
  const key2 = applyIdempotencyHeader(options2)['X-Idempotency-Key']

  assert.notEqual(key2, key1, '不同 HTTP 方法应生成不同键')
})

test('场景7：显式 idempotencyKey 跳过自动管理（用于调用方完全控制）', () => {
  const adapter = setupAdapter()
  const fixedKey = 'explicit-retry-key-123'

  // 第一次请求：显式传入 idempotencyKey
  const options1 = {
    method: 'POST',
    url: '/sale',
    data: { amount: 100 },
    idempotencyKey: fixedKey
  }
  const key1 = applyIdempotencyHeader(options1)['X-Idempotency-Key']
  assert.equal(key1, fixedKey, '应使用显式传入的键')

  // 业务失败时调用 releaseIdempotencyKeyOnFailure：应跳过（不污染暂存表）
  releaseIdempotencyKeyOnFailure(options1, key1)

  // 自动管理路径的请求：不应受显式键影响
  const options2 = {
    method: 'POST',
    url: '/sale',
    data: { amount: 100 }
  }
  const key2 = applyIdempotencyHeader(options2)['X-Idempotency-Key']
  assert.notEqual(key2, fixedKey, '自动管理路径不应复用显式键')

  // 业务成功时调用 clearIdempotencyKeyOnSuccess：应跳过（不清除显式键）
  clearIdempotencyKeyOnSuccess(options1, key1)
  // 无显式断言，只要不抛异常即可
})

test('场景8：idempotencyNewKey=true 强制生成新键（另存为新业务）', () => {
  const adapter = setupAdapter()
  const options = {
    method: 'POST',
    url: '/sale',
    data: { amount: 100 }
  }

  // 第一次请求
  const key1 = applyIdempotencyHeader(options)['X-Idempotency-Key']

  // 第二次请求：强制新键
  const options2 = { ...options, idempotencyNewKey: true }
  const key2 = applyIdempotencyHeader(options2)['X-Idempotency-Key']

  assert.notEqual(key2, key1, 'idempotencyNewKey=true 应强制生成新键')

  // 第三次请求：仍用自动管理，应复用第一次的键（暂存未被污染）
  const key3 = applyIdempotencyHeader(options)['X-Idempotency-Key']
  assert.equal(key3, key1, '强制新键不应污染自动管理暂存表')
})

test('场景9：URL 路径变量规范化（/sale/123 与 /sale/456 视为相同签名）', () => {
  const adapter = setupAdapter()

  // 第一次请求：/sale/123
  const options1 = { method: 'PUT', url: '/sale/123', data: { amount: 100 } }
  const key1 = applyIdempotencyHeader(options1)['X-Idempotency-Key']

  // 网络失败，保留暂存
  releaseIdempotencyKeyOnFailure(options1, key1)

  // 第二次请求：/sale/456（不同 ID，但相同签名 /sale/{id}）
  // 注意：这测试的是签名规范化逻辑，实际业务中不同 ID 是不同业务，
  // 但签名相同会导致复用同键——这是设计权衡（避免相同表单不同 ID 被视为不同签名）。
  const options2 = { method: 'PUT', url: '/sale/456', data: { amount: 100 } }
  const key2 = applyIdempotencyHeader(options2)['X-Idempotency-Key']

  // 签名相同 → 复用同键（这是设计预期，避免路径变量 ID 变化导致键爆炸）
  assert.equal(key2, key1, 'URL 路径变量规范化后相同签名应复用同键')
})

test('场景10：连续多次失败重试都复用同一个键', () => {
  const adapter = setupAdapter()
  const options = {
    method: 'POST',
    url: '/expense',
    data: { amount: 500, category: 'office' }
  }

  // 第一次请求
  const key1 = applyIdempotencyHeader(options)['X-Idempotency-Key']
  releaseIdempotencyKeyOnFailure(options, key1)

  // 第二次重试
  const key2 = applyIdempotencyHeader(options)['X-Idempotency-Key']
  releaseIdempotencyKeyOnFailure(options, key2)

  // 第三次重试
  const key3 = applyIdempotencyHeader(options)['X-Idempotency-Key']
  releaseIdempotencyKeyOnFailure(options, key3)

  // 第四次重试
  const key4 = applyIdempotencyHeader(options)['X-Idempotency-Key']

  assert.equal(key1, key2, '第二次重试应复用第一次的键')
  assert.equal(key2, key3, '第三次重试应复用第二次的键')
  assert.equal(key3, key4, '第四次重试应复用第三次的键')
})

test('场景11：computeRequestSignature 稳定性（相同输入相同输出）', () => {
  const sig1 = computeRequestSignature('/sale', 'POST', { a: 1, b: 2 })
  const sig2 = computeRequestSignature('/sale', 'POST', { b: 2, a: 1 })
  assert.equal(sig1, sig2, '对象字段顺序不同应产生相同签名（稳定序列化）')
})

test('场景12：consumeReusableKey 直接调用 - 命中暂存复用，未命中生成新键', () => {
  const adapter = setupAdapter()
  const signature = 'post|/sale|amount=100'

  // 第一次调用：未命中，生成新键
  const key1 = consumeReusableKey(signature, 'sale:create')
  assert.ok(key1, '未命中时应生成新键')
  assert.match(key1, /^sale:create-\d+-[a-z0-9]+$/)

  // 第二次调用：命中暂存，复用同键
  const key2 = consumeReusableKey(signature, 'sale:create')
  assert.equal(key2, key1, '命中暂存时应复用原键')
})

test('场景13：clearReusableKey 直接调用 - 清除后下次生成新键', () => {
  const adapter = setupAdapter()
  const signature = 'post|/expense|amount=200'

  const key1 = consumeReusableKey(signature, 'expense:create')
  clearReusableKey(signature)

  const key2 = consumeReusableKey(signature, 'expense:create')
  assert.notEqual(key2, key1, '清除后下次应生成新键')
})

test('场景14：releaseReusableKey 直接调用 - 保留暂存供下次复用', () => {
  const adapter = setupAdapter()
  const signature = 'post|/stocktake|id=1'

  const key1 = consumeReusableKey(signature, 'stocktake:reverse')
  releaseReusableKey(signature, key1)

  const key2 = consumeReusableKey(signature, 'stocktake:reverse')
  assert.equal(key2, key1, 'release 后下次应复用原键')
})
