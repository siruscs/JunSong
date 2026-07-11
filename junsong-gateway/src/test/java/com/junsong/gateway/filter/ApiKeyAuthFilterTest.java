package com.junsong.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import com.junsong.common.redis.service.RedisService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApiKeyAuthFilter 单元测试
 *
 * 覆盖场景：
 *   1. 缺少请求头 → OPEN_AUTH_HEADERS_MISSING
 *   2. 时间戳格式错误 → OPEN_AUTH_TIMESTAMP_INVALID
 *   3. 时间戳过期 → OPEN_AUTH_TIMESTAMP_INVALID
 *   4. Nonce 重放 → OPEN_AUTH_NONCE_REPLAY
 *   5. 签名不匹配 → OPEN_AUTH_SIGNATURE_INVALID
 *   6. AppKey 不存在或已禁用 → OPEN_AUTH_KEY_DISABLED
 *   7. 正确签名 → 请求通过
 */
class ApiKeyAuthFilterTest
{
    private static final String TEST_APP_KEY = "test-app-key";
    private static final String TEST_APP_SECRET = "test-secret-12345";
    private static final String TEST_PATH = "/openapi/v1/members";

    /** 内部服务错误响应 JSON */
    private static final String ERROR_RESPONSE = "{\"code\":500,\"msg\":\"AppKey不存在\",\"data\":null}";

    private ApiKeyAuthFilter filter;
    private FakeRedisService redisService;
    private RecordingGatewayFilterChain chain;

    @BeforeEach
    void setUp()
    {
        redisService = new FakeRedisService();
        chain = new RecordingGatewayFilterChain();
        filter = new ApiKeyAuthFilter();
        filter.setRedisService(redisService);
    }

    // ── 场景 1：缺少请求头 ─────────────────────────────

    @Test
    @DisplayName("缺少认证请求头应返回 OPEN_AUTH_HEADERS_MISSING")
    void missingHeaders_shouldReturnHeadersMissing()
    {
        MockServerWebExchange exchange = buildExchange(
                MockServerHttpRequest.get(TEST_PATH).build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertFalse(chain.wasCalled());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    // ── 场景 2：时间戳格式错误 ─────────────────────────

    @Test
    @DisplayName("时间戳非数字应返回 OPEN_AUTH_TIMESTAMP_INVALID")
    void invalidTimestampFormat_shouldReturnTimestampInvalid()
    {
        MockServerWebExchange exchange = buildExchange(
                MockServerHttpRequest.get(TEST_PATH)
                        .header("X-App-Key", TEST_APP_KEY)
                        .header("X-App-Timestamp", "not-a-number")
                        .header("X-App-Nonce", "nonce-1")
                        .header("X-App-Signature", "sig").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertFalse(chain.wasCalled());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    // ── 场景 3：时间戳过期 ─────────────────────────────

    @Test
    @DisplayName("时间戳超出5分钟窗口应返回 OPEN_AUTH_TIMESTAMP_INVALID")
    void expiredTimestamp_shouldReturnTimestampInvalid()
    {
        // 使用 10 分钟前的时间戳
        long expiredTs = System.currentTimeMillis() - 10 * 60 * 1000L;

        MockServerWebExchange exchange = buildExchange(
                MockServerHttpRequest.get(TEST_PATH)
                        .header("X-App-Key", TEST_APP_KEY)
                        .header("X-App-Timestamp", String.valueOf(expiredTs))
                        .header("X-App-Nonce", "nonce-1")
                        .header("X-App-Signature", "sig").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertFalse(chain.wasCalled());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    // ── 场景 4：Nonce 重放 ─────────────────────────────

    @Test
    @DisplayName("重复 Nonce 应返回 OPEN_AUTH_NONCE_REPLAY")
    void replayNonce_shouldReturnNonceReplay()
    {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "replay-nonce-1";
        String signature = computeSignature(TEST_APP_SECRET, "GET", TEST_PATH, timestamp, nonce, "");

        redisService.authContext = buildAuthContext();
        redisService.replayedNonceKeyPart = "openapi:nonce:" + TEST_APP_KEY + ":" + nonce;

        MockServerWebExchange exchange = buildExchange(
                MockServerHttpRequest.get(TEST_PATH)
                        .header("X-App-Key", TEST_APP_KEY)
                        .header("X-App-Timestamp", timestamp)
                        .header("X-App-Nonce", nonce)
                        .header("X-App-Signature", signature).build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertFalse(chain.wasCalled());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    // ── 场景 5：签名不匹配 ─────────────────────────────

    @Test
    @DisplayName("错误签名应返回 OPEN_AUTH_SIGNATURE_INVALID")
    void wrongSignature_shouldReturnSignatureInvalid()
    {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "nonce-sig-test";

        redisService.authContext = buildAuthContext();

        MockServerWebExchange exchange = buildExchange(
                MockServerHttpRequest.get(TEST_PATH)
                        .header("X-App-Key", TEST_APP_KEY)
                        .header("X-App-Timestamp", timestamp)
                        .header("X-App-Nonce", nonce)
                        .header("X-App-Signature", "wrong-signature-value").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertFalse(chain.wasCalled());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    // ── 场景 6：AppKey 不存在或已禁用 ──────────────────

    @Test
    @DisplayName("内部服务返回错误应返回 OPEN_AUTH_KEY_DISABLED")
    void invalidAppKey_shouldReturnKeyDisabled()
    {
        String timestamp = String.valueOf(System.currentTimeMillis());

        filter.setWebClient(buildWebClient(ERROR_RESPONSE));

        MockServerWebExchange exchange = buildExchange(
                MockServerHttpRequest.get(TEST_PATH)
                        .header("X-App-Key", TEST_APP_KEY)
                        .header("X-App-Timestamp", timestamp)
                        .header("X-App-Nonce", "nonce-1")
                        .header("X-App-Signature", "some-sig").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertFalse(chain.wasCalled());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    // ── 场景 7：正确签名请求通过 ───────────────────────

    @Test
    @DisplayName("正确签名请求应通过过滤器到达下游")
    void validSignature_shouldPassFilter()
    {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "valid-nonce-1";
        String signature = computeSignature(TEST_APP_SECRET, "GET", TEST_PATH, timestamp, nonce, "");

        redisService.authContext = buildAuthContext();

        MockServerWebExchange exchange = buildExchange(
                MockServerHttpRequest.get(TEST_PATH)
                        .header("X-App-Key", TEST_APP_KEY)
                        .header("X-App-Timestamp", timestamp)
                        .header("X-App-Nonce", nonce)
                        .header("X-App-Signature", signature).build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertTrue(chain.wasCalled());
        assertTrue(redisService.setKeys.stream().anyMatch(key -> key.contains("openapi:nonce:")));
    }

    // ── 场景 8：签名通过后注入 X-Open-* 上下文头 ───────

    @Test
    @DisplayName("签名通过后下游请求应包含 X-Open-* 可信上下文头")
    void shouldInjectOpenContextHeadersAfterSignaturePass()
    {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "valid-nonce-headers";
        String signature = computeSignature(TEST_APP_SECRET, "GET", TEST_PATH, timestamp, nonce, "");

        redisService.authContext = buildAuthContext();

        MockServerWebExchange exchange = buildExchange(
                MockServerHttpRequest.get(TEST_PATH)
                        .header("X-App-Key", TEST_APP_KEY)
                        .header("X-App-Timestamp", timestamp)
                        .header("X-App-Nonce", nonce)
                        .header("X-App-Signature", signature).build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertTrue(chain.wasCalled());
        assertNotNull(chain.capturedExchange(), "downstream exchange must be captured");
        HttpHeaders downstreamHeaders = chain.capturedExchange().getRequest().getHeaders();
        assertEquals("1", downstreamHeaders.getFirst("X-Open-App-Id"));
        assertEquals(TEST_APP_KEY, downstreamHeaders.getFirst("X-Open-App-Key"));
        assertEquals("100", downstreamHeaders.getFirst("X-Open-Tenant-Id"));
        assertEquals("production", downstreamHeaders.getFirst("X-Open-Key-Type"));
        assertNotNull(downstreamHeaders.getFirst("X-Open-Request-Id"), "X-Open-Request-Id must be injected");
        assertEquals("R23", downstreamHeaders.getFirst("X-Open-Auth-Version"));
    }

    // ── 场景 8b：外部请求携带的内部头被剥离 ─────────────

    @Test
    @DisplayName("外部请求携带的 from-source / user_id 等内部头必须被剥离")
    void shouldStripSpoofableInternalHeadersFromExternalRequests()
    {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "valid-nonce-strip";
        String signature = computeSignature(TEST_APP_SECRET, "GET", TEST_PATH, timestamp, nonce, "");

        redisService.authContext = buildAuthContext();

        MockServerWebExchange exchange = buildExchange(
                MockServerHttpRequest.get(TEST_PATH)
                        .header("X-App-Key", TEST_APP_KEY)
                        .header("X-App-Timestamp", timestamp)
                        .header("X-App-Nonce", nonce)
                        .header("X-App-Signature", signature)
                        .header("from-source", "inner")
                        .header("user_id", "999")
                        .header("username", "attacker")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertTrue(chain.wasCalled());
        HttpHeaders downstreamHeaders = chain.capturedExchange().getRequest().getHeaders();
        assertNull(downstreamHeaders.getFirst("from-source"), "from-source must be stripped from external requests");
        assertNull(downstreamHeaders.getFirst("user_id"), "user_id must be stripped from external requests");
        assertNull(downstreamHeaders.getFirst("username"), "username must be stripped from external requests");
    }

    // ── 场景 9：日额度超限返回 429 ─────────────────────

    @Test
    @DisplayName("日额度超限应返回 429 和 OPEN_QUOTA_EXCEEDED")
    void shouldReturn429WhenDailyQuotaExceeded()
    {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "valid-nonce-quota";
        String signature = computeSignature(TEST_APP_SECRET, "GET", TEST_PATH, timestamp, nonce, "");

        ApiKeyAuthFilter.AuthContext ctx = buildAuthContext();
        ctx.setDailyQuota(1);
        redisService.authContext = ctx;
        redisService.incrementReturn = 2L;

        MockServerWebExchange exchange = buildExchange(
                MockServerHttpRequest.get(TEST_PATH)
                        .header("X-App-Key", TEST_APP_KEY)
                        .header("X-App-Timestamp", timestamp)
                        .header("X-App-Nonce", nonce)
                        .header("X-App-Signature", signature).build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertFalse(chain.wasCalled(), "downstream must not be called when quota exceeded");
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
    }

    // ── 辅助方法 ───────────────────────────────────────

    private MockServerWebExchange buildExchange(MockServerHttpRequest request)
    {
        return MockServerWebExchange.from(request);
    }

    private ApiKeyAuthFilter.AuthContext buildAuthContext()
    {
        ApiKeyAuthFilter.AuthContext cachedCtx = new ApiKeyAuthFilter.AuthContext();
        cachedCtx.setAppSecret(TEST_APP_SECRET);
        cachedCtx.setAppId(1L);
        cachedCtx.setTenantId(100L);
        cachedCtx.setDailyQuota(1000);
        cachedCtx.setStatus("0");
        cachedCtx.setKeyType("production");
        return cachedCtx;
    }

    /**
     * 计算 HMAC-SHA256 签名（与过滤器使用相同算法）
     */
    private String computeSignature(String secret, String method, String path,
            String timestamp, String nonce, String body)
    {
        try
        {
            String data = method + path + timestamp + nonce + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
            {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            throw new RuntimeException("签名计算失败", e);
        }
    }

    private static class RecordingGatewayFilterChain implements GatewayFilterChain
    {
        private boolean called;
        private ServerWebExchange exchange;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange)
        {
            called = true;
            this.exchange = exchange;
            return Mono.empty();
        }

        boolean wasCalled()
        {
            return called;
        }

        ServerWebExchange capturedExchange()
        {
            return exchange;
        }
    }

    private static class FakeRedisService extends RedisService
    {
        private ApiKeyAuthFilter.AuthContext authContext;
        private String replayedNonceKeyPart;
        private final List<String> setKeys = new ArrayList<>();
        private Long incrementReturn = 1L;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getCacheObject(String key)
        {
            if (key.startsWith("openapi:auth_context:"))
            {
                return (T) authContext;
            }
            return null;
        }

        @Override
        public Boolean hasKey(String key)
        {
            return replayedNonceKeyPart != null && key.contains(replayedNonceKeyPart);
        }

        @Override
        public <T> void setCacheObject(String key, T value, Long timeout, TimeUnit timeUnit)
        {
            setKeys.add(key);
        }

        @Override
        public Long increment(String key)
        {
            return incrementReturn;
        }

        @Override
        public boolean expire(String key, long timeout, TimeUnit unit)
        {
            return true;
        }
    }

    private WebClient buildWebClient(String responseBody)
    {
        return WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse
                        .create(HttpStatus.OK)
                        .body(responseBody)
                        .build()))
                .build();
    }
}
