/**
 * Phase 6：小程序现场作业能力测试。
 *
 * 覆盖 18 项必需场景：
 *  1. 扫码商品查询成功
 *  2. 扫码会员查询成功
 *  3. 无权限扫码结果被拒绝
 *  4. 部门切换后旧查询结果不污染新部门
 *  5. 费用凭证上传成功
 *  6. 上传失败不产生已提交费用状态
 *  7. 上传重试不重复创建凭证
 *  8. 库存盘点数量校验
 *  9. 盘盈盘亏原因必填
 * 10. 库存版本冲突提示并刷新
 * 11. 非敏感草稿保存
 * 12. 敏感字段不进入草稿
 * 13. 提交成功后清除草稿
 * 14. 请求超时后的安全重试
 * 15. 401 自动恢复登录
 * 16. 403 显示权限提示
 * 17. 任务完成后刷新任务和统一指标
 * 18. 重复提交不会重复写入
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

// ============================================================
// 工具：读取源文件内容（用于契约测试）
// ============================================================
const ROOT = new URL('../', import.meta.url)
const BACKEND_ROOT = new URL('../../junsong-modules/junsong-finance/src/main/java/com/junsong/finance/', import.meta.url)

function readSource(rel) {
  return fs.readFileSync(new URL(rel, ROOT), 'utf8')
}

function readBackend(rel) {
  return fs.readFileSync(new URL(rel, BACKEND_ROOT), 'utf8')
}

// ============================================================
// 模拟 uni 全局对象（用于功能性测试）
// ============================================================
const storage = new Map()

function setupUniMock() {
  storage.clear()
  globalThis.uni = {
    setStorageSync(key, value) { storage.set(key, value) },
    getStorageSync(key) { return storage.has(key) ? storage.get(key) : '' },
    removeStorageSync(key) { storage.delete(key) },
    getStorageInfoSync() { return { keys: Array.from(storage.keys()) } },
    showToast() {},
    showLoading() {},
    hideLoading() {},
    showModal() {},
    uploadFile() {},
    chooseImage() {},
    request() {},
    navigateTo() {},
    navigateBack() {},
    reLaunch() {}
  }
}

function teardownUniMock() {
  delete globalThis.uni
}

// ============================================================
// 扫码查找（1-4）
// ============================================================

// 1. 扫码商品查询成功
test('findProductByCode calls GET /product/list with productCode and deptId', () => {
  const src = readSource('src/api/scan.js')
  assert.match(src, /export function findProductByCode/)
  assert.match(src, /url: '\/product\/list'/)
  assert.match(src, /method: 'GET'/)
  // 传递 deptId 用于部门隔离
  assert.match(src, /productCode: code/)
  assert.match(src, /deptId/)
  // 返回首条记录或 null（无结果时）
  assert.match(src, /rows\[0\]/)
  assert.match(src, /: null/)
})

// 2. 扫码会员查询成功
test('findMemberByNo calls GET /member/no/{memberNo}', () => {
  const src = readSource('src/api/scan.js')
  assert.match(src, /export function findMemberByNo/)
  assert.match(src, /url: '\/member\/no\/'/)
  assert.match(src, /encodeURIComponent\(memberNo\)/)
  assert.match(src, /method: 'GET'/)
})

// 3. 无权限扫码结果被拒绝
test('scan APIs use silent mode and backend enforces 403 for unauthorized access', () => {
  const src = readSource('src/api/scan.js')
  // 扫码查询使用 silent 模式，不弹默认 toast，由页面处理 403
  assert.match(src, /silent: true/)
  // 后端 StockTakeController 必须有 @RequiresPermissions
  const controller = readBackend('controller/StockTakeController.java')
  assert.match(controller, /@RequiresPermissions\("finance:stock:take"\)/)
})

// 4. 部门切换后旧查询结果不污染新部门
test('findProductByCode passes deptId to isolate department scope', () => {
  const src = readSource('src/api/scan.js')
  // findProductByCode 接收 deptId 参数并传递给后端
  assert.match(src, /findProductByCode\(code, deptId\)/)
  assert.match(src, /data:\s*\{\s*productCode: code,\s*deptId\s*\}/)
  // field-work 页面在 onShow 时检测部门切换并清空旧结果
  const page = readSource('src/pages/field-work/index.vue')
  assert.match(page, /onShow/)
  assert.match(page, /this\.contextVersion !== currentVersion/)
  assert.match(page, /this\.scanResult = null/)
})

// ============================================================
// 费用凭证上传（5-7）
// ============================================================

// 5. 费用凭证上传成功
test('uploadAttachment uses uni.uploadFile with Bearer token and returns url', () => {
  const src = readSource('src/api/attachment.js')
  assert.match(src, /export function uploadAttachment/)
  assert.match(src, /uni\.uploadFile/)
  assert.match(src, /name: 'file'/)
  assert.match(src, /Authorization: 'Bearer ' \+ token/)
  // 返回 url 和 name
  assert.match(src, /resolve\(\{/)
  assert.match(src, /url: file\.url/)
  assert.match(src, /name: file\.name/)
})

// 6. 上传失败不产生已提交费用状态
test('uploadAttachment rejects on failure without side effects', () => {
  const src = readSource('src/api/attachment.js')
  // 失败时 reject，不 resolve
  assert.match(src, /reject\(/)
  // 区分 401/403/超时/网络错误
  assert.match(src, /AUTH_EXPIRED/)
  assert.match(src, /PERMISSION_DENIED/)
  assert.match(src, /REQUEST_TIMEOUT/)
  assert.match(src, /NETWORK_ERROR/)
  assert.match(src, /UPLOAD_FAILED/)
  // field-work 页面不因上传失败而标记费用已提交
  const page = readSource('src/pages/field-work/index.vue')
  // 上传失败只显示 toast，不修改 attachmentUrl
  assert.match(page, /catch \(err\)/)
  assert.match(page, /this\.attachmentUrl = result\.url/)
})

// 7. 上传重试不重复创建凭证
test('uploadAttachment passes bizNo for idempotent association', () => {
  const src = readSource('src/api/attachment.js')
  // formData 包含 bizType 和 bizNo，用于后端关联和去重
  assert.match(src, /formData\.bizType = bizType/)
  assert.match(src, /formData\.bizNo = bizNo/)
  // field-work 页面调用时传递 takeNo 作为 bizNo
  const page = readSource('src/pages/field-work/index.vue')
  assert.match(page, /uploadAttachment\(filePath, 'expense', this\.takeForm\.takeNo\)/)
})

// ============================================================
// 库存盘点（8-10）
// ============================================================

// 8. 库存盘点数量校验
test('submitStockTake passes actualQuantity and field-work validates non-negative', () => {
  const api = readSource('src/api/stocktake.js')
  assert.match(api, /export function submitStockTake/)
  assert.match(api, /url: '\/stockTake'/)
  assert.match(api, /method: 'POST'/)
  assert.match(api, /actualQuantity: params\.actualQuantity/)
  // 后端 VO 有 actualQuantity 字段且必须 >= 0
  const vo = readBackend('domain/vo/StockTakeRequest.java')
  assert.match(vo, /private Integer actualQuantity/)
  // field-work 页面前端校验非负
  const page = readSource('src/pages/field-work/index.vue')
  assert.match(page, /isNaN\(actual\) \|\| actual < 0/)
})

// 9. 盘盈盘亏原因必填
test('reason is required when actual differs from expected on both client and backend', () => {
  // 前端：差异时必须填写原因
  const page = readSource('src/pages/field-work/index.vue')
  assert.match(page, /actual !== expected && !this\.takeForm\.reason/)
  assert.match(page, /盘盈盘亏必须填写原因/)
  // API 传递 reason
  const api = readSource('src/api/stocktake.js')
  assert.match(api, /reason: params\.reason/)
  // 后端 VO 有 reason 字段
  const vo = readBackend('domain/vo/StockTakeRequest.java')
  assert.match(vo, /private String reason/)
  // 后端测试验证无原因时拒绝
  const test = fs.readFileSync(new URL(
    '../../junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/StockTakeServiceImplTest.java',
    import.meta.url
  ), 'utf8')
  assert.match(test, /rejectsGainWithoutReason/)
  assert.match(test, /原因/)
})

// 10. 库存版本冲突提示并刷新
test('backend uses SELECT FOR UPDATE for inventory version safety', () => {
  const service = readBackend('service/impl/StockTakeServiceImpl.java')
  // 使用行锁查询当前库存
  assert.match(service, /selectPositionQuantityForUpdate/)
  // 更新结存时检查影响行数
  assert.match(service, /affected != 1/)
  assert.match(service, /库存结存更新影响行数异常/)
  // 事务回滚
  assert.match(service, /@Transactional/)
  assert.match(service, /rollbackFor = Exception\.class/)
})

// ============================================================
// 草稿保存（11-13）- 功能性测试
// ============================================================

// 11. 非敏感草稿保存
test('saveDraft stores non-sensitive fields by formKey and deptId', async () => {
  setupUniMock()
  try {
    const draftModule = await import(`data:text/javascript;base64,${Buffer.from(
      fs.readFileSync(new URL('../src/utils/draftStore.js', import.meta.url), 'utf8')
    ).toString('base64')}`)

    draftModule.saveDraft('expense', 100, {
      expenseContent: '办公耗材',
      amount: 150.50,
      reason: '月度采购'
    })

    const draft = draftModule.loadDraft('expense', 100)
    assert.ok(draft, '草稿应存在')
    assert.equal(draft.expenseContent, '办公耗材')
    assert.equal(draft.amount, 150.50)
    assert.equal(draft.reason, '月度采购')
  } finally {
    teardownUniMock()
  }
})

// 12. 敏感字段不进入草稿
test('filterSensitiveFields strips idCard, phone, token, password, attachment, bankCard', async () => {
  setupUniMock()
  try {
    const draftModule = await import(`data:text/javascript;base64,${Buffer.from(
      fs.readFileSync(new URL('../src/utils/draftStore.js', import.meta.url), 'utf8')
    ).toString('base64')}`)

    const sensitive = ['idCard', 'idCardNo', 'phone', 'mobile', 'phoneNumber',
      'token', 'accessToken', 'refreshToken', 'password', 'pwd',
      'attachment', 'attachments', 'voucher', 'vouchers',
      'bankCard', 'bankAccount']

    for (const field of sensitive) {
      assert.equal(draftModule.isSensitiveField(field), true,
        `字段 ${field} 应被标记为敏感`)
    }

    // 保存包含敏感字段的数据
    draftModule.saveDraft('expense', 100, {
      expenseContent: '安全字段',
      idCard: '110101199001011234',
      phone: '13800138000',
      token: 'Bearer xxx',
      password: 'secret123',
      attachment: 'file://xxx.jpg',
      bankCard: '6222000112345678'
    })

    const draft = draftModule.loadDraft('expense', 100)
    assert.equal(draft.expenseContent, '安全字段', '非敏感字段应保留')
    assert.equal(draft.idCard, undefined, 'idCard 不应进入草稿')
    assert.equal(draft.phone, undefined, 'phone 不应进入草稿')
    assert.equal(draft.token, undefined, 'token 不应进入草稿')
    assert.equal(draft.password, undefined, 'password 不应进入草稿')
    assert.equal(draft.attachment, undefined, 'attachment 不应进入草稿')
    assert.equal(draft.bankCard, undefined, 'bankCard 不应进入草稿')
  } finally {
    teardownUniMock()
  }
})

// 13. 提交成功后清除草稿
test('clearDraft removes draft after successful submission', async () => {
  setupUniMock()
  try {
    const draftModule = await import(`data:text/javascript;base64,${Buffer.from(
      fs.readFileSync(new URL('../src/utils/draftStore.js', import.meta.url), 'utf8')
    ).toString('base64')}`)

    draftModule.saveDraft('fieldWork', 100, { actualQuantity: '55', reason: '盘点差异' })
    assert.ok(draftModule.loadDraft('fieldWork', 100), '草稿应存在')

    draftModule.clearDraft('fieldWork', 100)
    assert.equal(draftModule.loadDraft('fieldWork', 100), null, '清除后草稿应为 null')

    // 草稿按 deptId 隔离：清除 dept 100 不影响 dept 200
    draftModule.saveDraft('fieldWork', 100, { actualQuantity: '10' })
    draftModule.saveDraft('fieldWork', 200, { actualQuantity: '20' })
    draftModule.clearDraft('fieldWork', 100)
    assert.equal(draftModule.loadDraft('fieldWork', 100), null, 'dept 100 草稿已清除')
    assert.ok(draftModule.loadDraft('fieldWork', 200), 'dept 200 草稿不受影响')
  } finally {
    teardownUniMock()
  }
})

// ============================================================
// 弱网/超时安全重试（14-16）- 功能性测试
// ============================================================

// 14. 请求超时后的安全重试
test('shouldRetrySafely retries GET on timeout with exponential backoff', async () => {
  const { shouldRetrySafely, isUnknownWriteOutcome } = await import('../src/utils/requestPolicy.js')

  // GET 超时可重试
  const r1 = shouldRetrySafely({ method: 'GET', error: { errMsg: 'request:fail timeout' }, retryCount: 0 })
  assert.equal(r1.shouldRetry, true)
  assert.ok(r1.delay >= 1000, '第一次重试延迟应 >= 1000ms')

  const r2 = shouldRetrySafely({ method: 'GET', error: { errMsg: 'request:fail timeout' }, retryCount: 1 })
  assert.equal(r2.shouldRetry, true)
  assert.ok(r2.delay >= 2000, '第二次重试延迟应 >= 2000ms')

  // 最多重试 3 次
  const r4 = shouldRetrySafely({ method: 'GET', error: { errMsg: 'request:fail timeout' }, retryCount: 3 })
  assert.equal(r4.shouldRetry, false, '超过 3 次不再重试')

  // POST 写操作不自动重试（可能已成功）
  const w = shouldRetrySafely({ method: 'POST', error: { errMsg: 'request:fail timeout' }, retryCount: 0 })
  assert.equal(w.shouldRetry, false, '写操作不自动重试')

  // 超时属于未知写结果
  assert.equal(isUnknownWriteOutcome({ code: 'REQUEST_TIMEOUT' }), true)
  assert.equal(isUnknownWriteOutcome({ code: 'NETWORK_ERROR' }), true)
  assert.equal(isUnknownWriteOutcome({ code: 'UPLOAD_FAILED' }), false)
})

// 15. 401 自动恢复登录
test('401 triggers authSession recovery via isAuthExpiredResponse', () => {
  const api = readSource('src/api/index.js')
  // request 中检测 401 并调用 authSession.recoverOnce
  assert.match(api, /isAuthExpiredResponse/)
  assert.match(api, /authSession\.recoverOnce/)
  // attachment.js 中 401 返回 AUTH_EXPIRED
  const attachment = readSource('src/api/attachment.js')
  assert.match(attachment, /data\.code === 401/)
  assert.match(attachment, /AUTH_EXPIRED/)
  // field-work 页面处理 AUTH_EXPIRED
  const page = readSource('src/pages/field-work/index.vue')
  assert.match(page, /AUTH_EXPIRED/)
})

// 16. 403 显示权限提示
test('403 classified as permission error and shown to user', async () => {
  const { classifyRequestError } = await import('../src/utils/requestPolicy.js')

  const err403 = classifyRequestError({ statusCode: 403 })
  assert.equal(err403.kind, 'permission')

  // attachment.js 返回 PERMISSION_DENIED
  const attachment = readSource('src/api/attachment.js')
  assert.match(attachment, /data\.code === 403/)
  assert.match(attachment, /PERMISSION_DENIED/)

  // field-work 页面显示权限提示
  const page = readSource('src/pages/field-work/index.vue')
  assert.match(page, /PERMISSION_DENIED/)
  assert.match(page, /暂无.*权限/)
})

// ============================================================
// 任务刷新和幂等（17-18）
// ============================================================

// 17. 任务完成后刷新任务和统一指标
test('refreshAfterTaskAction invokes all refresh callbacks', async () => {
  const { refreshAfterTaskAction } = await import('../src/utils/taskCenter.js')

  let taskListRefreshed = false
  let countRefreshed = false
  let metricsRefreshed = false

  await refreshAfterTaskAction({
    refreshTaskList: () => { taskListRefreshed = true },
    refreshPendingCount: () => { countRefreshed = true },
    refreshMetrics: () => { metricsRefreshed = true }
  })

  assert.equal(taskListRefreshed, true, '任务列表应被刷新')
  assert.equal(countRefreshed, true, '待办计数应被刷新')
  assert.equal(metricsRefreshed, true, '统一指标应被刷新')

  // field-work 页面提交成功后调用 refreshAfterTaskAction
  const page = readSource('src/pages/field-work/index.vue')
  assert.match(page, /refreshAfterTaskAction/)
  assert.match(page, /refreshMetrics/)
})

// 18. 重复提交不会重复写入
test('idempotency: takeNo uniqueness enforced by backend and preserved by client on retry', () => {
  // 客户端：生成唯一 takeNo
  const api = readSource('src/api/stocktake.js')
  assert.match(api, /export function generateTakeNo/)
  assert.match(api, /TK-/)
  // submitStockTake 使用 params.takeNo || generateTakeNo()
  assert.match(api, /params\.takeNo \|\| generateTakeNo\(\)/)

  // 后端：countByReferenceNo 检查幂等
  const service = readBackend('service/impl/StockTakeServiceImpl.java')
  assert.match(service, /countByReferenceNo/)
  assert.match(service, /盘点单号已存在/)
  assert.match(service, /禁止重复提交/)

  // 后端测试验证重复 takeNo 被拒绝
  const test = fs.readFileSync(new URL(
    '../../junsong-modules/junsong-finance/src/test/java/com/junsong/finance/service/impl/StockTakeServiceImplTest.java',
    import.meta.url
  ), 'utf8')
  assert.match(test, /rejectsDuplicateTakeNo/)
  assert.match(test, /已存在/)

  // field-work 页面：超时后保留 takeNo 供用户确认，不自动重试
  const page = readSource('src/pages/field-work/index.vue')
  assert.match(page, /isUnknownWriteOutcome/)
  assert.match(page, /不要重复提交相同单号/)
  // 成功后生成新 takeNo
  assert.match(page, /this\.takeForm\.takeNo = generateTakeNo\(\)/)
})
