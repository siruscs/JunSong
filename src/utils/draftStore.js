/**
 * 非敏感表单草稿存储。
 *
 * 安全边界：
 * 1. 只保存非敏感、未提交表单数据
 * 2. 不保存凭证、身份证、完整手机号、Token 或服务端响应
 * 3. 提交成功后必须清理草稿
 * 4. 草稿按 formKey + deptId 隔离，部门切换后旧草稿不污染新部门
 *
 * 敏感字段黑名单（永不保存）：
 * - idCard / idCardNo
 * - phone / mobile / phoneNumber（完整手机号）
 * - token / accessToken / refreshToken
 * - password / pwd
 * - attachment / attachments / voucher / vouchers（凭证文件）
 * - bankCard / bankAccount
 */

const SENSITIVE_FIELDS = new Set([
  'idCard', 'idCardNo', 'id_card', 'id_card_no',
  'phone', 'mobile', 'phoneNumber', 'phone_number',
  'token', 'accessToken', 'access_token', 'refreshToken', 'refresh_token',
  'password', 'pwd',
  'attachment', 'attachments', 'voucher', 'vouchers',
  'bankCard', 'bank_card', 'bankAccount', 'bank_account'
])

const STORAGE_PREFIX = 'draft:'

/**
 * 过滤敏感字段，只保留非敏感数据。
 * @param {Object} data 原始表单数据
 * @returns {Object} 过滤后的非敏感数据
 */
export function filterSensitiveFields(data = {}) {
  const safe = {}
  for (const [key, value] of Object.entries(data)) {
    if (!SENSITIVE_FIELDS.has(key)) {
      safe[key] = value
    }
  }
  return safe
}

/**
 * 保存草稿（自动过滤敏感字段）。
 * @param {string} formKey 表单标识（如 'expense' / 'stockTake'）
 * @param {number} deptId 门店ID（隔离不同门店草稿）
 * @param {Object} data 表单数据
 */
export function saveDraft(formKey, deptId, data = {}) {
  if (!formKey || !deptId) return
  const safeData = filterSensitiveFields(data)
  const key = STORAGE_PREFIX + formKey + ':' + deptId
  uni.setStorageSync(key, {
    data: safeData,
    savedAt: Date.now()
  })
}

/**
 * 读取草稿。
 * @param {string} formKey 表单标识
 * @param {number} deptId 门店ID
 * @returns {Object|null} 草稿数据，无草稿返回 null
 */
export function loadDraft(formKey, deptId) {
  if (!formKey || !deptId) return null
  const key = STORAGE_PREFIX + formKey + ':' + deptId
  const draft = uni.getStorageSync(key)
  if (!draft || !draft.data) return null
  return draft.data
}

/**
 * 清除草稿（提交成功后调用）。
 * @param {string} formKey 表单标识
 * @param {number} deptId 门店ID
 */
export function clearDraft(formKey, deptId) {
  if (!formKey || !deptId) return
  const key = STORAGE_PREFIX + formKey + ':' + deptId
  uni.removeStorageSync(key)
}

/**
 * 清除指定门店的所有草稿（部门切换时可选调用）。
 * @param {number} deptId 门店ID
 */
export function clearDraftsByDept(deptId) {
  if (!deptId) return
  const keys = uni.getStorageInfoSync().keys || []
  for (const key of keys) {
    if (key.startsWith(STORAGE_PREFIX) && key.endsWith(':' + deptId)) {
      uni.removeStorageSync(key)
    }
  }
}

/**
 * 判断字段是否敏感（供测试验证）。
 * @param {string} fieldName 字段名
 * @returns {boolean}
 */
export function isSensitiveField(fieldName) {
  return SENSITIVE_FIELDS.has(fieldName)
}
