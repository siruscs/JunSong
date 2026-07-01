/**
 * 开放平台 API 错误码映射
 *
 * 网关返回的 OPEN_* 错误码 → 用户友好中文提示。
 * 当响应 msg 字段包含这些错误码时，优先使用此处的提示文案。
 */
export const openApiErrorCodes: Record<string, string> = {
  OPEN_AUTH_HEADERS_MISSING: '请求缺少必要的认证信息，请检查 AppKey 签名配置。',
  OPEN_AUTH_TIMESTAMP_INVALID: '请求时间戳已过期，请检查客户端时间是否准确。',
  OPEN_AUTH_NONCE_REPLAY: '请求不可重复提交，请稍后重试。',
  OPEN_AUTH_SIGNATURE_INVALID: '签名校验失败，请检查 AppSecret 和签名算法。',
  OPEN_AUTH_KEY_DISABLED: '应用密钥无效或已停用，请联系管理员。',
  OPEN_RATE_LIMIT_EXCEEDED: '今日调用配额已用尽，请升级配额方案或明日再试。',
}

/**
 * 从网关响应 msg 中提取 OPEN_* 错误码并返回友好提示。
 * 网关响应格式示例："OPEN_AUTH_HEADERS_MISSING: 缺少API认证请求头"
 *
 * @param msg 网关原始错误消息
 * @returns 用户友好提示，未匹配时返回 null
 */
export function resolveOpenApiError(msg: string): string | null {
  if (!msg || typeof msg !== 'string') return null
  for (const [code, hint] of Object.entries(openApiErrorCodes)) {
    if (msg.startsWith(code)) return hint
  }
  return null
}
