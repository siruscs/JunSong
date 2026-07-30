package com.junsong.common.core.idempotency;

/**
 * 幂等记录状态机。
 *
 * 状态转移：PROCESSING → SUCCEEDED / FAILED
 * - PROCESSING：请求处理中，重复请求返回 409
 * - SUCCEEDED：请求成功完成，重复请求返回原结果
 * - FAILED：请求失败，允许安全重试
 *
 * @author junsong
 */
public enum IdempotencyStatus {
    /** 请求处理中 */
    PROCESSING,
    /** 请求成功完成 */
    SUCCEEDED,
    /** 请求失败，允许重试 */
    FAILED;

    public static IdempotencyStatus fromString(String status) {
        if (status == null || status.isEmpty()) {
            return null;
        }
        try {
            return IdempotencyStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
