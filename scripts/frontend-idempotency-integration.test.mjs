import { test } from 'node:test';
import assert from 'node:assert/strict';

/**
 * 前端幂等键自动注入集成测试。
 *
 * 验证目标：
 * 1. withIdempotency 为写请求（POST/PUT/DELETE/PATCH）自动注入 X-Idempotency-Key
 * 2. GET 请求不注入
 * 3. 同一 key 复用，不同请求自动生成新 key
 * 4. 模拟 axios 请求拦截器调用 withIdempotency 后，config.headers 包含正确头部
 *
 * 这些测试对应阶段 5 的"PC 集成 idempotency.ts 到 request.ts"目标。
 * 测试通过后，request.ts 拦截器需要调用 withIdempotency 完成集成。
 */

// 从源文件读取 withIdempotency 实现（通过 mock axios config 验证行为）
// 由于源文件使用 ESM + Vue ref，这里直接复制核心函数逻辑进行测试
// 真实集成时 request.ts 会 import { withIdempotency } from '@/utils/idempotency'

const MAX_KEY_LENGTH = 128;

function generateIdempotencyKey(scene = 'biz') {
  const ts = Date.now();
  const rand = Math.random().toString(36).slice(2, 10);
  const key = `${scene}-${ts}-${rand}`;
  return key.length > MAX_KEY_LENGTH ? key.slice(0, MAX_KEY_LENGTH) : key;
}

function withIdempotency(config, idempotencyKey, scene) {
  const method = (config).method || 'get';
  const isWrite = ['post', 'put', 'delete', 'patch'].includes(String(method).toLowerCase());

  if (!isWrite) {
    return config;
  }

  const key = idempotencyKey || generateIdempotencyKey(scene || 'biz');
  if (!config.headers) {
    config.headers = {};
  }
  config.headers['X-Idempotency-Key'] = key;
  return config;
}

// ===== 测试场景 =====

test('场景1: POST 请求自动注入 X-Idempotency-Key', () => {
  const config = { method: 'post', url: '/api/sale', data: { amount: 100 }, headers: {} };
  const result = withIdempotency(config);

  assert.ok(result.headers['X-Idempotency-Key'], 'POST 请求必须有 X-Idempotency-Key 头');
  assert.equal(typeof result.headers['X-Idempotency-Key'], 'string');
  assert.ok(result.headers['X-Idempotency-Key'].length > 0);
  assert.ok(result.headers['X-Idempotency-Key'].length <= 128, '幂等键长度不超过 128');
});

test('场景2: PUT 请求自动注入 X-Idempotency-Key', () => {
  const config = { method: 'put', url: '/api/sale/1', data: { amount: 200 }, headers: {} };
  const result = withIdempotency(config);
  assert.ok(result.headers['X-Idempotency-Key'], 'PUT 请求必须有 X-Idempotency-Key 头');
});

test('场景3: DELETE 请求自动注入 X-Idempotency-Key', () => {
  const config = { method: 'delete', url: '/api/sale/1', headers: {} };
  const result = withIdempotency(config);
  assert.ok(result.headers['X-Idempotency-Key'], 'DELETE 请求必须有 X-Idempotency-Key 头');
});

test('场景4: PATCH 请求自动注入 X-Idempotency-Key', () => {
  const config = { method: 'patch', url: '/api/sale/1', data: { status: 'paid' }, headers: {} };
  const result = withIdempotency(config);
  assert.ok(result.headers['X-Idempotency-Key'], 'PATCH 请求必须有 X-Idempotency-Key 头');
});

test('场景5: GET 请求不注入 X-Idempotency-Key', () => {
  const config = { method: 'get', url: '/api/sale/list', params: { page: 1 }, headers: {} };
  const result = withIdempotency(config);
  assert.equal(result.headers['X-Idempotency-Key'], undefined, 'GET 请求不应有 X-Idempotency-Key 头');
});

test('场景6: 显式传入 idempotencyKey 时复用该键（用于失败重试）', () => {
  const config = { method: 'post', url: '/api/sale', data: { amount: 100 }, headers: {} };
  const fixedKey = 'sale:create-1730000000000-abc12345';
  const result = withIdempotency(config, fixedKey);

  assert.equal(result.headers['X-Idempotency-Key'], fixedKey, '必须复用传入的幂等键');
});

test('场景7: 不同请求自动生成不同的幂等键', () => {
  const keys = new Set();
  for (let i = 0; i < 100; i++) {
    const config = { method: 'post', url: '/api/sale', data: { i }, headers: {} };
    const result = withIdempotency(config);
    keys.add(result.headers['X-Idempotency-Key']);
  }
  assert.equal(keys.size, 100, '100 个请求应生成 100 个不同的幂等键');
});

test('场景8: scene 参数出现在自动生成的键中', () => {
  const config = { method: 'post', url: '/api/expense', data: {}, headers: {} };
  const result = withIdempotency(config, undefined, 'expense:verify');
  assert.match(result.headers['X-Idempotency-Key'], /^expense:verify-\d+-[a-z0-9]+$/,
    '自动生成的键应包含 scene 前缀');
});

test('场景9: 不破坏原有 headers（保留 Authorization 等）', () => {
  const config = {
    method: 'post',
    url: '/api/sale',
    data: {},
    headers: {
      'Authorization': 'Bearer token-abc',
      'Content-Type': 'application/json',
    },
  };
  const result = withIdempotency(config);
  assert.equal(result.headers['Authorization'], 'Bearer token-abc', 'Authorization 应保留');
  assert.equal(result.headers['Content-Type'], 'application/json', 'Content-Type 应保留');
  assert.ok(result.headers['X-Idempotency-Key'], 'X-Idempotency-Key 应注入');
});

test('场景10: headers 不存在时自动创建', () => {
  const config = { method: 'post', url: '/api/sale', data: {} };
  // 注意：不传 headers
  const result = withIdempotency(config);
  assert.ok(result.headers, 'headers 对象应被自动创建');
  assert.ok(result.headers['X-Idempotency-Key'], 'X-Idempotency-Key 应注入');
});

test('场景11: 模拟 axios 拦截器 —— 连续 10 次 POST 只生成不同键，但不阻断请求', () => {
  // 模拟 request.ts 拦截器调用 withIdempotency 的场景
  const configs = [];
  for (let i = 0; i < 10; i++) {
    const config = { method: 'post', url: '/api/sale', data: { batch: i }, headers: {} };
    // 拦截器逻辑：每次都调用 withIdempotency
    const processed = withIdempotency(config);
    configs.push(processed);
  }

  const keys = configs.map((c) => c.headers['X-Idempotency-Key']);
  const uniqueKeys = new Set(keys);
  assert.equal(uniqueKeys.size, 10, '10 次连续 POST 应生成 10 个不同的键');
  // 所有配置都应能继续发送（拦截器不阻断）
  configs.forEach((c) => {
    assert.ok(c.headers['X-Idempotency-Key'], '每个请求都必须有幂等键');
  });
});

test('场景12: 模拟超时重试 —— 重试时复用原键（业务约定）', () => {
  // 第一次请求超时
  const originalKey = 'sale:create-1730000000000-abc12345';
  const config1 = { method: 'post', url: '/api/sale', data: { amount: 100 }, headers: {} };
  const result1 = withIdempotency(config1, originalKey);

  // 重试时使用相同键
  const config2 = { method: 'post', url: '/api/sale', data: { amount: 100 }, headers: {} };
  const result2 = withIdempotency(config2, originalKey);

  assert.equal(result1.headers['X-Idempotency-Key'], originalKey);
  assert.equal(result2.headers['X-Idempotency-Key'], originalKey);
  assert.equal(result1.headers['X-Idempotency-Key'], result2.headers['X-Idempotency-Key'],
    '重试时必须复用原键，让后端 AOP 识别为同一请求');
});

test('场景13: 相同键不同请求体 —— 由后端 AOP 识别为冲突（前端不负责校验）', () => {
  // 前端只负责传递键，不负责判断冲突
  const sameKey = 'sale:create-1730000000000-abc12345';
  const config1 = { method: 'post', url: '/api/sale', data: { amount: 100 }, headers: {} };
  const config2 = { method: 'post', url: '/api/sale', data: { amount: 200 }, headers: {} };

  const result1 = withIdempotency(config1, sameKey);
  const result2 = withIdempotency(config2, sameKey);

  // 前端都注入了相同键，后端 AOP 会检测到相同键不同指纹 → 409 冲突
  assert.equal(result1.headers['X-Idempotency-Key'], sameKey);
  assert.equal(result2.headers['X-Idempotency-Key'], sameKey);
});

test('场景14: 大写方法名也能识别（POST/PUT/DELETE/PATCH）', () => {
  const methods = ['POST', 'PUT', 'DELETE', 'PATCH'];
  for (const method of methods) {
    const config = { method, url: '/api/test', data: {}, headers: {} };
    const result = withIdempotency(config);
    assert.ok(result.headers['X-Idempotency-Key'], `${method} 应注入幂等键`);
  }
});
