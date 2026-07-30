package com.junsong.common.core.idempotency;

/**
 * 幂等失败重试策略。
 *
 * 区分业务失败与响应丢失/系统异常两类场景：
 * - 业务明确失败（参数校验、业务规则违反、余额不足等）：
 *     业务一定未成功执行，重试安全，但是否复用键由策略决定。
 * - 响应丢失但业务可能已成功（网络超时、5xx 系统异常、服务重启等）：
 *     业务可能已成功执行，必须使用同键让后端按 SUCCEEDED/PROCESSING 状态判定，
 *     切面会尝试 CAS 占位（FAILED → PROCESSING），成功后重新执行业务。
 *
 * @author junsong
 */
public enum IdempotencyRetryPolicy {

    /**
     * 业务明确失败后要求新键重试（保守策略，默认）。
     *
     * 适用场景：参数校验失败、业务规则违反等明确业务失败。
     * 行为：切面遇到 FAILED 状态时直接抛出原失败原因，要求客户端使用新键重试。
     * 优势：失败原因明确，新键重试语义清晰。
     */
    REQUIRE_NEW_KEY,

    /**
     * 允许同键安全重试。
     *
     * 适用场景：网络超时、5xx 系统异常、服务重启等响应丢失场景，
     *          以及业务失败后允许同键重试的低风险接口。
     * 行为：切面遇到 FAILED 状态时尝试 CAS 占位（FAILED → PROCESSING），
     *       占位成功后重新执行业务，占位失败则返回"请求处理中"。
     *       业务执行结果会覆盖原 FAILED 记录。
     * 注意：SUCCEEDED 状态永远返回原结果，不会重新执行业务。
     */
    ALLOW_SAME_KEY
}
