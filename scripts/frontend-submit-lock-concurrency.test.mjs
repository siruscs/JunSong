import { test } from 'node:test';
import assert from 'node:assert/strict';

/**
 * 前端提交锁（useSubmitLock）并发行为测试。
 *
 * useSubmitLock.ts 使用 Vue ref 实现，本测试通过最小化 mock ref 的响应式行为
 * 来验证 execute 函数的防重入语义，不依赖 Vue 运行时。
 *
 * 场景：
 * 1. 连续点击 2 次：第二次被忽略
 * 2. 连续点击 10 次：只有第一次执行
 * 3. 网络延迟场景：长时间请求期间，其他调用被忽略
 * 4. 双标签页（不同 key）：可以并行执行
 * 5. 幂等键生成唯一性
 */

// 最小化 ref mock —— 模拟 Vue ref 的 .value 读写语义
function ref(initial) {
  const obj = { value: initial };
  return obj;
}

// 从 useSubmitLock.ts 提取的核心逻辑（与源码一致）
function useSubmitLock() {
  const loading = ref(false);
  const lockKey = ref('');

  async function execute(key, fn) {
    if (loading.value && lockKey.value === key) {
      return undefined;
    }
    loading.value = true;
    lockKey.value = key;
    try {
      return await fn();
    } finally {
      loading.value = false;
      lockKey.value = '';
    }
  }

  function buildIdempotencyKey(prefix, ...parts) {
    return `${prefix}-${parts.join('-')}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  }

  return { loading, execute, buildIdempotencyKey };
}

// ===== 测试场景 =====

test('场景1: 连续点击 2 次，第二次被忽略（返回 undefined）', async () => {
  const { execute } = useSubmitLock();
  let callCount = 0;

  // 模拟异步请求
  const mockFn = () => new Promise((resolve) => {
    callCount++;
    setTimeout(() => resolve('result'), 50);
  });

  // 同时发起 2 个相同 key 的请求
  const p1 = execute('submit-key', mockFn);
  const p2 = execute('submit-key', mockFn);

  const [r1, r2] = await Promise.all([p1, p2]);

  assert.equal(r1, 'result', '第一次调用应返回结果');
  assert.equal(r2, undefined, '第二次调用应被忽略（返回 undefined）');
  assert.equal(callCount, 1, 'mockFn 应只被调用 1 次');
});

test('场景2: 连续点击 10 次，只有第一次执行', async () => {
  const { execute } = useSubmitLock();
  let callCount = 0;

  const mockFn = () => new Promise((resolve) => {
    callCount++;
    setTimeout(() => resolve('ok'), 50);
  });

  // 同时发起 10 个相同 key 的请求
  const key = 'rapid-click-10x';
  const promises = [];
  for (let i = 0; i < 10; i++) {
    promises.push(execute(key, mockFn));
  }

  const results = await Promise.all(promises);

  const successCount = results.filter((r) => r === 'ok').length;
  const ignoredCount = results.filter((r) => r === undefined).length;

  assert.equal(successCount, 1, '只有 1 次请求成功执行');
  assert.equal(ignoredCount, 9, '9 次请求被忽略');
  assert.equal(callCount, 1, 'mockFn 应只被调用 1 次');
});

test('场景3: 网络延迟 —— 长时间请求期间，其他调用被忽略', async () => {
  const { execute } = useSubmitLock();
  let callCount = 0;

  const slowFn = () => new Promise((resolve) => {
    callCount++;
    // 模拟 200ms 网络延迟
    setTimeout(() => resolve('slow-result'), 200);
  });

  // 发起慢请求
  const p1 = execute('network-delay', slowFn);

  // 在慢请求期间，50ms 后尝试再次提交
  await new Promise((r) => setTimeout(r, 50));
  const p2 = execute('network-delay', slowFn);

  // 100ms 后再尝试
  await new Promise((r) => setTimeout(r, 50));
  const p3 = execute('network-delay', slowFn);

  const [r1, r2, r3] = await Promise.all([p1, p2, p3]);

  assert.equal(r1, 'slow-result', '第一次请求应成功');
  assert.equal(r2, undefined, '网络延迟期间第二次请求应被忽略');
  assert.equal(r3, undefined, '网络延迟期间第三次请求应被忽略');
  assert.equal(callCount, 1, '慢请求应只被调用 1 次');
});

test('场景4: 双标签页 —— 不同 key 的请求可以并行执行', async () => {
  const { execute } = useSubmitLock();
  let callCount = 0;

  const mockFn = (result) => new Promise((resolve) => {
    callCount++;
    setTimeout(() => resolve(result), 50);
  });

  // 标签页1 用 key-A
  const p1 = execute('key-A', () => mockFn('tab1-result'));
  // 标签页2 用 key-B（不同 key）
  const p2 = execute('key-B', () => mockFn('tab2-result'));

  const [r1, r2] = await Promise.all([p1, p2]);

  // useSubmitLock 的 execute 条件：loading.value && lockKey.value === key
  // 不同 key 时 lockKey.value !== key，所以第二次调用不被忽略
  // 这是设计预期：相同 key 防重入，不同 key 允许并行
  assert.equal(callCount, 2, '不同 key 的请求应都执行（双标签页隔离由独立实例保证）');
  assert.equal(r1, 'tab1-result', '标签页1 请求应成功');
  assert.equal(r2, 'tab2-result', '标签页2 请求应成功');
});

test('场景5: 请求完成后可以再次提交（锁释放）', async () => {
  const { execute } = useSubmitLock();
  let callCount = 0;

  const mockFn = () => new Promise((resolve) => {
    callCount++;
    setTimeout(() => resolve('done'), 30);
  });

  // 第一次请求
  const r1 = await execute('retry-key', mockFn);
  assert.equal(r1, 'done');
  assert.equal(callCount, 1);

  // 第二次请求（锁已释放）
  const r2 = await execute('retry-key', mockFn);
  assert.equal(r2, 'done');
  assert.equal(callCount, 2, '锁释放后第二次请求应成功');
});

test('场景6: buildIdempotencyKey 生成唯一且格式正确的幂等键', () => {
  const { buildIdempotencyKey } = useSubmitLock();
  const keys = new Set();

  // 生成 1000 个幂等键，验证唯一性
  for (let i = 0; i < 1000; i++) {
    const key = buildIdempotencyKey('POST', 'batch-1', 'v3');
    keys.add(key);

    // 格式验证：前缀-业务标识-时间戳-随机串
    assert.match(key, /^POST-batch-1-v3-\d+-[a-z0-9]+$/,
      `幂等键格式不正确: ${key}`);
  }

  assert.equal(keys.size, 1000, '1000 个幂等键必须全部唯一');
});

test('场景7: 连续 10 次相同 key 并发 + 不同 key 串行，验证锁状态正确', async () => {
  const { execute, loading } = useSubmitLock();
  let countA = 0;
  let countB = 0;

  const fnA = () => new Promise((resolve) => {
    countA++;
    setTimeout(() => resolve('A'), 50);
  });

  const fnB = () => new Promise((resolve) => {
    countB++;
    setTimeout(() => resolve('B'), 30);
  });

  // 10 次相同 key 并发
  const keyA = 'concurrent-A';
  const promisesA = [];
  for (let i = 0; i < 10; i++) {
    promisesA.push(execute(keyA, fnA));
  }

  const resultsA = await Promise.all(promisesA);

  assert.equal(countA, 1, 'key-A 应只执行 1 次');
  assert.equal(resultsA.filter((r) => r === 'A').length, 1, '只有 1 次返回 A');
  assert.equal(loading.value, false, '完成后 loading 应为 false');

  // 然后执行 key-B（锁已释放）
  const rB = await execute('key-B', fnB);
  assert.equal(rB, 'B');
  assert.equal(countB, 1, 'key-B 应执行 1 次');
});
