package com.junsong.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.utils.ServletUtils;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.redis.service.RedisService;
import com.junsong.gateway.constant.OpenApiErrorCodes;
import com.junsong.gateway.metrics.OpenApiMetricsRecorder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 开放平台 API Key 鉴权过滤器
 *
 * 拦截 /openapi/v1/** 请求，校验 HMAC-SHA256 签名。
 *
 * 签名算法：HMAC-SHA256(AppSecret, method + path + timestamp + nonce + body)
 *
 * 请求头：
 *   X-App-Key        AppKey（公开标识）
 *   X-App-Timestamp  时间戳（毫秒）
 *   X-App-Nonce      随机串（防重放）
 *   X-App-Signature  HMAC-SHA256 签名
 *
 * @author junsong
 */
@Component
public class ApiKeyAuthFilter implements GlobalFilter, Ordered
{
    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);

    // ── 路径 ───────────────────────────────────────────
    private static final String OPENAPI_PREFIX = "/openapi/";

    // ── 请求头 ─────────────────────────────────────────
    private static final String HEADER_APP_KEY = "X-App-Key";
    private static final String HEADER_TIMESTAMP = "X-App-Timestamp";
    private static final String HEADER_NONCE = "X-App-Nonce";
    private static final String HEADER_SIGNATURE = "X-App-Signature";
    /** 服务间内部密钥头（不可由外部伪造，与 OPEN_INTERNAL_SECRET 配对） */
    private static final String HEADER_INNER_TOKEN = "X-Inner-Token";

    // ── 统一错误码（见 OpenApiErrorCodes）─────────────────

    // ── 时间窗口 ───────────────────────────────────────
    /** 时间戳有效期（5 分钟） */
    private static final long TIMESTAMP_EXPIRE_MS = 5 * 60 * 1000L;
    /** Nonce 缓存时间（10 分钟） */
    private static final long NONCE_CACHE_SECONDS = 600L;
    /** 认证上下文缓存 TTL（5 分钟） */
    private static final long AUTH_CONTEXT_TTL_MINUTES = 5L;

    // ── JSON ───────────────────────────────────────────
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ── 依赖 ───────────────────────────────────────────
    @Autowired
    private RedisService redisService;

    @Autowired
    private OpenApiMetricsRecorder metricsRecorder;

    /**
     * 内部服务间密钥，由 OPEN_INTERNAL_SECRET 环境变量注入。
     * 网关调用开放服务内部接口时携带 X-Inner-Token 头，开放服务校验一致性。
     * 为空时仅记录警告（不阻断网关启动），开放服务侧 fail closed。
     */
    @Value("${open.internal.secret:}")
    private String innerToken;

    private WebClient webClient;

    public ApiKeyAuthFilter()
    {
        this.webClient = WebClient.builder().build();
    }

    /**
     * 供测试注入 mock WebClient
     */
    public void setWebClient(WebClient webClient)
    {
        this.webClient = webClient;
    }

    /**
     * 供测试注入 mock RedisService
     */
    public void setRedisService(RedisService redisService)
    {
        this.redisService = redisService;
    }

    // ── 认证上下文 ─────────────────────────────────────

    /**
     * 缓存的认证上下文，包含签名校验和后续转发所需的全部字段
     */
    public static class AuthContext
    {
        private String appSecret;
        private Long appId;
        private Long tenantId;
        private Integer dailyQuota;
        private String status;
        private String keyType;

        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }

        public Long getAppId() { return appId; }
        public void setAppId(Long appId) { this.appId = appId; }

        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

        public Integer getDailyQuota() { return dailyQuota; }
        public void setDailyQuota(Integer dailyQuota) { this.dailyQuota = dailyQuota; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getKeyType() { return keyType; }
        public void setKeyType(String keyType) { this.keyType = keyType; }
    }

    // ── 过滤器入口 ─────────────────────────────────────

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (!path.contains(OPENAPI_PREFIX))
        {
            return chain.filter(exchange);
        }

        HttpHeaders headers = request.getHeaders();
        String appKey = headers.getFirst(HEADER_APP_KEY);
        String timestamp = headers.getFirst(HEADER_TIMESTAMP);
        String nonce = headers.getFirst(HEADER_NONCE);
        String signature = headers.getFirst(HEADER_SIGNATURE);

        if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(timestamp)
                || StringUtils.isEmpty(nonce) || StringUtils.isEmpty(signature))
        {
            return unauthorizedResponse(exchange, "缺少API认证请求头", OpenApiErrorCodes.AUTH_HEADERS_MISSING);
        }

        long ts;
        try
        {
            ts = Long.parseLong(timestamp);
        }
        catch (NumberFormatException e)
        {
            return unauthorizedResponse(exchange, "时间戳格式错误", OpenApiErrorCodes.AUTH_TIMESTAMP_INVALID);
        }

        long now = System.currentTimeMillis();
        if (Math.abs(now - ts) > TIMESTAMP_EXPIRE_MS)
        {
            return unauthorizedResponse(exchange, "请求已过期", OpenApiErrorCodes.AUTH_TIMESTAMP_INVALID);
        }

        String method = request.getMethod().name();

        if ("GET".equals(method) || "DELETE".equals(method))
        {
            return doVerify(exchange, chain, appKey, method, path, timestamp, nonce, "", signature);
        }

        return DataBufferUtils.join(request.getBody())
                .defaultIfEmpty(new org.springframework.core.io.buffer.DefaultDataBufferFactory().wrap(new byte[0]))
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    String body = new String(bytes, StandardCharsets.UTF_8);
                    ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(request)
                    {
                        @Override
                        public Flux<DataBuffer> getBody()
                        {
                            return Flux.just(new org.springframework.core.io.buffer.DefaultDataBufferFactory().wrap(bytes));
                        }
                    };
                    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
                    return doVerify(mutatedExchange, chain, appKey, method, path, timestamp, nonce, body, signature);
                });
    }

    // ── 签名校验核心逻辑 ───────────────────────────────

    private Mono<Void> doVerify(ServerWebExchange exchange, GatewayFilterChain chain,
            String appKey, String method, String path, String timestamp, String nonce,
            String body, String signature)
    {
        return getAuthContext(appKey)
                .flatMap(authCtx -> {
                    // Nonce 防重放
                    String nonceKey = "openapi:nonce:" + appKey + ":" + nonce;
                    if (redisService.hasKey(nonceKey))
                    {
                        return unauthorizedResponse(exchange, "请求不可重放", OpenApiErrorCodes.AUTH_NONCE_REPLAY);
                    }

                    // HMAC-SHA256 签名比对
                    String signStr = method + path + timestamp + nonce + body;
                    String computedSignature = hmacSha256(authCtx.getAppSecret(), signStr);
                    if (!computedSignature.equals(signature))
                    {
                        return unauthorizedResponse(exchange, "签名校验失败", OpenApiErrorCodes.AUTH_SIGNATURE_INVALID);
                    }

                    // 记录 Nonce
                    redisService.setCacheObject(nonceKey, "1", Long.valueOf(NONCE_CACHE_SECONDS), TimeUnit.SECONDS);

                    // 日额度检查（原子计数，超限返回 429）
                    if (authCtx.getDailyQuota() != null && authCtx.getDailyQuota() > 0)
                    {
                        String quotaKey = "openapi:quota:" + appKey + ":" + java.time.LocalDate.now();
                        Long used = redisService.increment(quotaKey);
                        if (used != null && used == 1L)
                        {
                            redisService.expire(quotaKey, 2, TimeUnit.DAYS);
                        }
                        if (used != null && used > authCtx.getDailyQuota())
                        {
                            return quotaExceededResponse(exchange, "开放API日额度已用尽", OpenApiErrorCodes.QUOTA_EXCEEDED);
                        }
                    }

                    // 记录请求指标
                    if (metricsRecorder != null)
                    {
                        metricsRecorder.recordRequest(appKey, method, path, 200);
                    }

                    // 注入可信开放上下文头后转发到下游
                    // 同时剥离外部请求可能携带的内部头，防止伪造 from-source / user_id 绕过下游权限
                    String requestId = java.util.UUID.randomUUID().toString().replace("-", "");
                    ServerHttpRequest decorated = exchange.getRequest().mutate()
                            .headers(h -> {
                                h.remove("from-source");
                                h.remove("user_id");
                                h.remove("username");
                                h.remove("user_key");
                            })
                            .header("X-Open-App-Id", String.valueOf(authCtx.getAppId()))
                            .header("X-Open-App-Key", appKey)
                            .header("X-Open-Tenant-Id", String.valueOf(authCtx.getTenantId()))
                            .header("X-Open-Key-Type", authCtx.getKeyType())
                            .header("X-Open-Request-Id", requestId)
                            .header("X-Open-Auth-Version", "R23")
                            .build();
                    return chain.filter(exchange.mutate().request(decorated).build());
                })
                .onErrorResume(e -> {
                    log.error("[ApiKeyAuth] 查询认证上下文失败, appKey={}, error={}", appKey, e.getMessage());
                    return unauthorizedResponse(exchange, "AppKey不存在或已禁用", OpenApiErrorCodes.AUTH_KEY_DISABLED);
                });
    }

    // ── 认证上下文获取（Redis 缓存 + 远程查询） ────────

    private Mono<AuthContext> getAuthContext(String appKey)
    {
        String cacheKey = "openapi:auth_context:" + appKey;

        // 优先从 Redis 读取缓存的认证上下文
        try
        {
            Object cached = redisService.getCacheObject(cacheKey);
            if (cached instanceof AuthContext ctx)
            {
                return Mono.just(ctx);
            }
        }
        catch (Exception e)
        {
            log.warn("[ApiKeyAuth] Redis 缓存读取异常, appKey={}, 降级到远程查询", appKey);
        }

        // 远程查询并缓存
        return fetchAuthContext(appKey)
                .doOnNext(ctx -> {
                    try
                    {
                        redisService.setCacheObject(cacheKey, ctx, Long.valueOf(AUTH_CONTEXT_TTL_MINUTES), TimeUnit.MINUTES);
                    }
                    catch (Exception e)
                    {
                        log.warn("[ApiKeyAuth] Redis 缓存写入异常, appKey={}", appKey);
                    }
                });
    }

    /**
     * 调用开放服务内部接口查询认证上下文，使用结构化 JSON 解析。
     * 响应结构无效时 fail closed（抛出异常）。
     */
    private Mono<AuthContext> fetchAuthContext(String appKey)
    {
        return webClient.get()
                .uri("http://junsong-modules-open:9208/internal/secret/byKey/{appKey}", appKey)
                .header(SecurityConstants.FROM_SOURCE, SecurityConstants.INNER)
                .header(HEADER_INNER_TOKEN, innerToken)
                .retrieve()
                .bodyToMono(String.class)
                .map(resp -> {
                    if (StringUtils.isEmpty(resp))
                    {
                        throw new RuntimeException("Empty response from open service");
                    }

                    try
                    {
                        JsonNode root = OBJECT_MAPPER.readTree(resp);

                        // 检查业务状态码（AjaxResult 成功为 200，兼容 0）
                        JsonNode codeNode = root.get("code");
                        if (codeNode == null || (codeNode.asInt() != 0 && codeNode.asInt() != 200))
                        {
                            String msg = root.has("msg") ? root.get("msg").asText() : "unknown";
                            throw new RuntimeException("Open service error: " + msg);
                        }

                        // 结构化解析 data 节点
                        JsonNode dataNode = root.get("data");
                        if (dataNode == null || dataNode.isNull())
                        {
                            throw new RuntimeException("Missing data node in response");
                        }

                        // AppKey 停用检查（fail closed）
                        String status = getFieldText(dataNode, "status");
                        if (StringUtils.isNotEmpty(status) && !"0".equals(status))
                        {
                            throw new RuntimeException("AppKey is disabled, status=" + status);
                        }

                        // 必须包含 appSecret
                        String appSecret = getFieldText(dataNode, "appSecret");
                        if (StringUtils.isEmpty(appSecret))
                        {
                            throw new RuntimeException("Missing appSecret in response data");
                        }

                        AuthContext ctx = new AuthContext();
                        ctx.setAppSecret(appSecret);
                        ctx.setAppId(getFieldLong(dataNode, "appId"));
                        ctx.setTenantId(getFieldLong(dataNode, "tenantId"));
                        ctx.setDailyQuota(getFieldInt(dataNode, "dailyQuota"));
                        ctx.setStatus(status);
                        ctx.setKeyType(getFieldText(dataNode, "keyType"));

                        if (ctx.getAppId() == null || ctx.getTenantId() == null
                                || StringUtils.isEmpty(ctx.getKeyType()))
                        {
                            throw new RuntimeException("Missing required auth context");
                        }

                        return ctx;
                    }
                    catch (RuntimeException re)
                    {
                        throw re;
                    }
                    catch (Exception e)
                    {
                        log.error("[ApiKeyAuth] JSON 解析失败, appKey={}", appKey, e);
                        throw new RuntimeException("Failed to parse open service response", e);
                    }
                });
    }

    // ── JSON 辅助方法 ──────────────────────────────────

    private static String getFieldText(JsonNode node, String field)
    {
        JsonNode child = node.get(field);
        return (child != null && !child.isNull()) ? child.asText() : null;
    }

    private static Long getFieldLong(JsonNode node, String field)
    {
        JsonNode child = node.get(field);
        return (child != null && !child.isNull() && child.isNumber()) ? child.asLong() : null;
    }

    private static Integer getFieldInt(JsonNode node, String field)
    {
        JsonNode child = node.get(field);
        return (child != null && !child.isNull() && child.isNumber()) ? child.asInt() : null;
    }

    // ── HMAC-SHA256 ────────────────────────────────────

    private String hmacSha256(String secret, String data)
    {
        try
        {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        }
        catch (Exception e)
        {
            log.error("[ApiKeyAuth] HMAC 计算失败", e);
            return "";
        }
    }

    private String bytesToHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
        {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ── 错误响应 ───────────────────────────────────────

    /**
     * 返回 401 Unauthorized 响应，携带统一错误码。
     * 使用 overload 3 确保 HTTP 状态码正确设置为 401。
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String msg, String errorCode)
    {
        log.warn("[ApiKeyAuth] {} path={}, errorCode={}", msg, exchange.getRequest().getPath(), errorCode);
        if (metricsRecorder != null)
        {
            metricsRecorder.recordAuthError(errorCode);
        }
        logGatewayRejection(exchange, 401, errorCode, msg);
        String responseBody = errorCode + ": " + msg;
        return ServletUtils.webFluxResponseWriter(
                exchange.getResponse(), HttpStatus.UNAUTHORIZED, responseBody, HttpStatus.UNAUTHORIZED.value());
    }

    /**
     * 返回 429 Too Many Requests 响应，携带统一错误码。
     */
    private Mono<Void> quotaExceededResponse(ServerWebExchange exchange, String msg, String errorCode)
    {
        log.warn("[ApiKeyAuth] {} path={}, errorCode={}", msg, exchange.getRequest().getPath(), errorCode);
        if (metricsRecorder != null)
        {
            metricsRecorder.recordAuthError(errorCode);
        }
        logGatewayRejection(exchange, 429, errorCode, msg);
        String responseBody = errorCode + ": " + msg;
        return ServletUtils.webFluxResponseWriter(
                exchange.getResponse(), HttpStatus.TOO_MANY_REQUESTS, responseBody, HttpStatus.TOO_MANY_REQUESTS.value());
    }

    /**
     * 异步记录网关层拒绝日志到 open_api_log（fire-and-forget，不阻塞响应）
     */
    private void logGatewayRejection(ServerWebExchange exchange, int responseCode, String errorCode, String msg)
    {
        try
        {
            String appKey = exchange.getRequest().getHeaders().getFirst("X-App-Key");
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "GET";
            String requestIp = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "";

            java.util.Map<String, Object> logBody = new java.util.LinkedHashMap<>();
            logBody.put("appKey", appKey);
            logBody.put("requestMethod", method);
            logBody.put("requestPath", path);
            logBody.put("requestIp", requestIp);
            logBody.put("responseCode", responseCode);
            logBody.put("status", "fail");
            logBody.put("errorCode", errorCode);
            if (msg != null && msg.length() > 500)
            {
                logBody.put("responseMessage", msg.substring(0, 500));
            }
            else
            {
                logBody.put("responseMessage", msg);
            }

            webClient.post()
                    .uri("http://junsong-modules-open:9208/internal/log/access")
                    .header(SecurityConstants.FROM_SOURCE, SecurityConstants.INNER)
                    .header(HEADER_INNER_TOKEN, innerToken)
                    .bodyValue(logBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            r -> {},
                            e -> log.debug("[ApiKeyAuth] 网关拒绝日志写入失败: {}", e.getMessage())
                    );
        }
        catch (Exception e)
        {
            log.debug("[ApiKeyAuth] 网关拒绝日志构造失败: {}", e.getMessage());
        }
    }

    @Override
    public int getOrder()
    {
        return -150;
    }
}
