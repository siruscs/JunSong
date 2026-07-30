package com.junsong.common.core.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等注解。
 *
 * 普通写接口只需添加此注解，由公共 AOP 切面执行原子幂等校验，
 * 业务 Service 不得手写查重逻辑。
 *
 * 协议：
 * - 键来源：默认从请求头 X-Idempotency-Key 读取，特殊接口可自定义 keyResolver
 * - 作用域：tenantId + scene + idempotencyKey
 * - 状态机：PROCESSING → SUCCEEDED / FAILED（FAILED 可通过 retryPolicy 重新占位）
 * - 重复请求：SUCCEEDED 返回原结果；PROCESSING 返回 409；
 *             FAILED 按 retryPolicy 决定（REQUIRE_NEW_KEY 抛出失败原因；
 *             ALLOW_SAME_KEY 尝试 CAS 占位后重新执行业务）
 * - 相同键不同指纹：返回 409 冲突（无论状态如何）
 *
 * 用法示例：
 * <pre>
 * &#64;Idempotent(scene = "sale:create")
 * &#64;PostMapping
 * public AjaxResult add(&#64;RequestBody FinSaleRecord req) { ... }
 * </pre>
 *
 * @author junsong
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等场景标识，全局唯一，必填。
     * 建议格式：模块:动作（如 sale:create, purchase:save, expense:verify）
     */
    String scene();

    /**
     * 是否强制要求键，默认 true。
     * required=true 时缺键 → 400 Bad Request
     * required=false 时缺键 → 跳过幂等，直接执行业务
     */
    boolean required() default true;

    /**
     * 幂等记录保留期（秒），默认 86400（1 天）。
     * 财务高风险场景（过账、冲销、库存变更）建议 ≥ 2592000（30 天）。
     */
    int ttlSeconds() default 86400;

    /**
     * 结果存储模式，默认 REFERENCE。
     * FULL：存储完整响应（仅小型响应）
     * REFERENCE：只存储业务资源类型和ID（大响应）
     */
    IdempotencyResultMode resultMode() default IdempotencyResultMode.REFERENCE;

    /**
     * 键解析器 Bean 名称，默认空字符串（使用默认 HeaderKeyResolver）。
     * 特殊接口（如批量导入、工作流动作）可自定义解析器。
     */
    String keyResolver() default "";

    /**
     * 是否为高风险接口（财务过账、库存变更、付款、冲销）。
     * highRisk=true 时，幂等记录库不可用则失败关闭，不得绕过。
     * highRisk=false 时，幂等记录库不可用按 failOpen 配置处理。
     */
    boolean highRisk() default false;

    /**
     * 普通低风险接口在幂等记录库不可用时是否放行，默认 false。
     * highRisk=true 时此配置被忽略（强制失败关闭）。
     */
    boolean failOpen() default false;

    /**
     * 失败重试策略，默认 REQUIRE_NEW_KEY。
     *
     * - REQUIRE_NEW_KEY：业务失败后要求新键重试（保守策略）
     * - ALLOW_SAME_KEY：允许同键安全重试（适用于网络超时/系统异常等响应丢失场景）
     *
     * 推荐配置：
     * - 普通业务写（CRUD）：REQUIRE_NEW_KEY（默认）
     * - 高风险金融操作（过账、冲销、付款）：REQUIRE_NEW_KEY
     * - 网络不稳定场景下需要安全重试的接口：ALLOW_SAME_KEY
     *
     * 注意：SUCCEEDED 状态永远返回原结果，不受此策略影响。
     */
    IdempotencyRetryPolicy retryPolicy() default IdempotencyRetryPolicy.REQUIRE_NEW_KEY;
}
