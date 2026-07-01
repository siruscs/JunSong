package com.junsong.gateway.constant;

/**
 * 开放平台 API 统一错误码常量
 *
 * 所有 OPEN_* 前缀的机器可读错误码集中管理，供网关过滤器和测试使用。
 *
 * @author junsong
 */
public final class OpenApiErrorCodes
{
    private OpenApiErrorCodes() {}

    // ── 认证类（401） ──────────────────────────────────

    /** 缺少认证请求头（X-App-Key / X-App-Timestamp / X-App-Nonce / X-App-Signature） */
    public static final String AUTH_HEADERS_MISSING = "OPEN_AUTH_HEADERS_MISSING";

    /** 时间戳格式错误或超出 5 分钟有效窗口 */
    public static final String AUTH_TIMESTAMP_INVALID = "OPEN_AUTH_TIMESTAMP_INVALID";

    /** Nonce 已被使用（防重放，10 分钟窗口） */
    public static final String AUTH_NONCE_REPLAY = "OPEN_AUTH_NONCE_REPLAY";

    /** HMAC-SHA256 签名校验失败 */
    public static final String AUTH_SIGNATURE_INVALID = "OPEN_AUTH_SIGNATURE_INVALID";

    /** AppKey 不存在或已停用 */
    public static final String AUTH_KEY_DISABLED = "OPEN_AUTH_KEY_DISABLED";

    // ── 限流类（429） ──────────────────────────────────

    /** 日调用配额已用尽 */
    public static final String RATE_LIMIT_EXCEEDED = "OPEN_RATE_LIMIT_EXCEEDED";
}
