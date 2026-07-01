package com.junsong.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 开放平台 API 可观测指标记录器
 *
 * Prometheus 指标：
 *   openapi_requests_total    — 请求总数（按 appKey / method / path / status 分组）
 *   openapi_auth_errors_total — 认证失败总数（按 errorCode 分组）
 *   openapi_request_duration  — 请求延迟分布（按 method / path 分组）
 *
 * @author junsong
 */
@Component
public class OpenApiMetricsRecorder
{
    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * 记录一次 API 请求
     */
    public void recordRequest(String appKey, String method, String path, int statusCode)
    {
        Counter.builder("openapi_requests_total")
                .tag("app_key", appKey != null ? appKey : "unknown")
                .tag("method", method)
                .tag("path", path)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录认证失败
     */
    public void recordAuthError(String errorCode)
    {
        Counter.builder("openapi_auth_errors_total")
                .tag("error_code", errorCode)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录请求耗时
     */
    public void recordDuration(String method, String path, long durationMs)
    {
        Timer.builder("openapi_request_duration_seconds")
                .tag("method", method)
                .tag("path", path)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
    }
}
