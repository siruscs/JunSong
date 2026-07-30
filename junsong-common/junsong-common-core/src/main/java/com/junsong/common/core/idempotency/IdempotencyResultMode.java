package com.junsong.common.core.idempotency;

/**
 * 幂等结果存储模式。
 *
 * - FULL：存储完整响应（仅小型响应）
 * - REFERENCE：只存储业务资源类型和ID（大响应，默认）
 *
 * @author junsong
 */
public enum IdempotencyResultMode {
    /** 存储完整响应（仅小型响应） */
    FULL,
    /** 只存储业务资源类型和ID（默认，大响应） */
    REFERENCE
}
