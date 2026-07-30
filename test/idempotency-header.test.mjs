import test from 'node:test'
import assert from 'node:assert/strict'

import {
  generateIdempotencyKey,
  inferIdempotencyScene,
  applyIdempotencyHeader
} from '../src/utils/idempotency.js'

/**
 * 小程序幂等键自动注入测试。
 *
 * 验证：
 * 1. 写请求（POST/PUT/DELETE/PATCH）自动注入 X-Idempotency-Key
 * 2. GET 请求不注入
 * 3. 显式 idempotencyKey 复用（用于失败重试）
 * 4. 已存在头部不覆盖
 * 5. scene 推断正确
 * 6. 自动生成的键唯一
 */

test('POST 请求自动注入 X-Idempotency-Key', () => {
  const header = applyIdempotencyHeader({
    method: 'POST',
    url: '/sale',
    data: { amount: 100 }
  })
  assert.ok(header['X-Idempotency-Key'], 'POST 请求必须有 X-Idempotency-Key')
  assert.ok(header['X-Idempotency-Key'].length > 0)
  assert.ok(header['X-Idempotency-Key'].length <= 128)
})

test('PUT 请求自动注入 X-Idempotency-Key', () => {
  const header = applyIdempotencyHeader({
    method: 'PUT',
    url: '/sale/1',
    data: { amount: 200 }
  })
  assert.ok(header['X-Idempotency-Key'])
})

test('DELETE 请求自动注入 X-Idempotency-Key', () => {
  const header = applyIdempotencyHeader({
    method: 'DELETE',
    url: '/sale/1'
  })
  assert.ok(header['X-Idempotency-Key'])
})

test('PATCH 请求自动注入 X-Idempotency-Key', () => {
  const header = applyIdempotencyHeader({
    method: 'PATCH',
    url: '/sale/1',
    data: { status: 'paid' }
  })
  assert.ok(header['X-Idempotency-Key'])
})

test('GET 请求不注入 X-Idempotency-Key', () => {
  const header = applyIdempotencyHeader({
    method: 'GET',
    url: '/sale/list',
    data: { page: 1 }
  })
  assert.equal(header['X-Idempotency-Key'], undefined)
})

test('显式 idempotencyKey 时复用该键（用于失败重试）', () => {
  const fixedKey = 'sale:create-1730000000000-abc12345'
  const header = applyIdempotencyHeader({
    method: 'POST',
    url: '/sale',
    data: {},
    idempotencyKey: fixedKey
  })
  assert.equal(header['X-Idempotency-Key'], fixedKey)
})

test('已存在 X-Idempotency-Key 时不覆盖（调用方显式控制）', () => {
  const existingKey = 'explicit-key-123'
  const header = applyIdempotencyHeader({
    method: 'POST',
    url: '/sale',
    data: {},
    header: { 'X-Idempotency-Key': existingKey }
  })
  assert.equal(header['X-Idempotency-Key'], existingKey, '不应覆盖已存在的头部')
})

test('小写 x-idempotency-key 也被视为已存在', () => {
  const existingKey = 'explicit-key-lower'
  const header = applyIdempotencyHeader({
    method: 'POST',
    url: '/sale',
    data: {},
    header: { 'x-idempotency-key': existingKey }
  })
  // 已存在时不覆盖，返回原 header
  assert.equal(header['x-idempotency-key'], existingKey)
  assert.equal(header['X-Idempotency-Key'], undefined)
})

test('保留原有 header 字段（Authorization 等）', () => {
  const header = applyIdempotencyHeader({
    method: 'POST',
    url: '/sale',
    data: {},
    header: {
      'Authorization': 'Bearer token-abc',
      'Content-Type': 'application/json'
    }
  })
  assert.equal(header['Authorization'], 'Bearer token-abc')
  assert.equal(header['Content-Type'], 'application/json')
  assert.ok(header['X-Idempotency-Key'])
})

test('header 不存在时自动创建对象', () => {
  const header = applyIdempotencyHeader({
    method: 'POST',
    url: '/sale',
    data: {}
    // 不传 header
  })
  assert.ok(header['X-Idempotency-Key'])
})

test('不同请求生成不同的幂等键', () => {
  const keys = new Set()
  for (let i = 0; i < 100; i++) {
    const header = applyIdempotencyHeader({
      method: 'POST',
      url: '/sale',
      data: { i }
    })
    keys.add(header['X-Idempotency-Key'])
  }
  assert.equal(keys.size, 100, '100 个请求应生成 100 个不同的键')
})

test('scene 推断：POST /sale → sale:create', () => {
  const scene = inferIdempotencyScene('/sale', 'POST')
  assert.equal(scene, 'sale:create')
})

test('scene 推断：PUT /sale/1 → sale:update', () => {
  const scene = inferIdempotencyScene('/sale/1', 'PUT')
  assert.equal(scene, 'sale:update')
})

test('scene 推断：DELETE /sale/1 → sale:delete', () => {
  const scene = inferIdempotencyScene('/sale/1', 'DELETE')
  assert.equal(scene, 'sale:delete')
})

test('scene 推断：带 /api 前缀也能正确提取', () => {
  const scene = inferIdempotencyScene('/api/expense', 'POST')
  assert.equal(scene, 'expense:create')
})

test('scene 推断：带查询参数也能正确提取', () => {
  const scene = inferIdempotencyScene('/sale?page=1', 'GET')
  assert.equal(scene, 'sale:write') // GET 不在 actionMap 中，回退为 write
})

test('scene 推断：空 URL 返回 biz', () => {
  assert.equal(inferIdempotencyScene('', 'POST'), 'biz')
  assert.equal(inferIdempotencyScene(undefined, 'POST'), 'biz')
})

test('generateIdempotencyKey 生成的键包含 scene 前缀', () => {
  const key = generateIdempotencyKey('expense:verify')
  assert.match(key, /^expense:verify-\d+-[a-z0-9]+$/)
})

test('generateIdempotencyKey 生成的键不超过 128 字符', () => {
  const longScene = 'a'.repeat(200)
  const key = generateIdempotencyKey(longScene)
  assert.ok(key.length <= 128, '键长度应不超过 128')
})

test('模拟超时重试 —— 重试时复用原键', () => {
  const originalKey = 'sale:create-1730000000000-abc12345'
  const header1 = applyIdempotencyHeader({
    method: 'POST',
    url: '/sale',
    data: { amount: 100 },
    idempotencyKey: originalKey
  })
  const header2 = applyIdempotencyHeader({
    method: 'POST',
    url: '/sale',
    data: { amount: 100 },
    idempotencyKey: originalKey
  })
  assert.equal(header1['X-Idempotency-Key'], originalKey)
  assert.equal(header2['X-Idempotency-Key'], originalKey)
  assert.equal(header1['X-Idempotency-Key'], header2['X-Idempotency-Key'])
})

test('自定义 idempotencyScene 覆盖自动推断', () => {
  const header = applyIdempotencyHeader({
    method: 'POST',
    url: '/sale',
    data: {},
    idempotencyScene: 'custom:scene'
  })
  assert.match(header['X-Idempotency-Key'], /^custom:scene-\d+-[a-z0-9]+$/)
})
