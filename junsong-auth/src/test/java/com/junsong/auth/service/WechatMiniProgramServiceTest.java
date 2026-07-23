package com.junsong.auth.service;

import com.junsong.auth.config.WechatMiniProgramProperties;
import com.junsong.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * WechatMiniProgramService 单元测试。
 *
 * <p>覆盖：code 过期、微信错误码、超时、响应解析失败、成功、空 openid、http 5xx。
 * 使用 MockRestServiceServer 模拟微信接口，不发起真实网络请求。</p>
 *
 * <p>安全校验：所有错误信息不得包含 code、openid、AppSecret 明文。</p>
 */
class WechatMiniProgramServiceTest
{
    private WechatMiniProgramProperties properties;
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private WechatMiniProgramService service;

    @BeforeEach
    void setUp()
    {
        properties = new WechatMiniProgramProperties();
        properties.setAppId("wxtestappid");
        properties.setAppSecret("wxtestsecret");
        properties.setTimeoutMs(5000);
        properties.setMaxRetries(0); // 测试不重试，便于精确模拟

        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        service = new WechatMiniProgramService(properties, restClientBuilder.build());
    }

    // ==================== 成功场景 ====================

    @Test
    void exchangeCodeForIdentity_success_returnsOpenidAndUnionid()
    {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"openid\":\"openid_12345\",\"unionid\":\"unionid_67890\","
                                + "\"session_key\":\"sk\",\"errcode\":0}",
                        MediaType.APPLICATION_JSON));

        WechatMiniProgramService.WechatIdentity identity =
                service.exchangeCodeForIdentity("valid_code");

        assertNotNull(identity);
        assertEquals("openid_12345", identity.openid());
        assertEquals("unionid_67890", identity.unionid());
        mockServer.verify();
    }

    @Test
    void exchangeCodeForIdentity_success_withoutUnionid()
    {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"openid\":\"openid_only\",\"session_key\":\"sk\",\"errcode\":0}",
                        MediaType.APPLICATION_JSON));

        WechatMiniProgramService.WechatIdentity identity =
                service.exchangeCodeForIdentity("valid_code");

        assertNotNull(identity);
        assertEquals("openid_only", identity.openid());
        assertNull(identity.unionid());
    }

    // ==================== code 过期 / 无效 ====================

    @Test
    void exchangeCodeForIdentity_codeExpired_throwsServiceException()
    {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"errcode\":40029,\"errmsg\":\"invalid code\"}",
                        MediaType.APPLICATION_JSON));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("expired_code"));

        // 错误信息不得包含 code、openid、AppSecret 明文
        assertFalse(ex.getMessage().contains("expired_code"),
                "错误信息不得包含 code 明文");
        assertFalse(ex.getMessage().toLowerCase().contains("appsecret"),
                "错误信息不得包含 AppSecret");
    }

    // ==================== 微信频率限制 ====================

    @Test
    void exchangeCodeForIdentity_frequencyLimited_throwsServiceException()
    {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"errcode\":45011,\"errmsg\":\"frequency limited\"}",
                        MediaType.APPLICATION_JSON));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("some_code"));

        assertFalse(ex.getMessage().contains("some_code"));
    }

    // ==================== 微信系统繁忙 ====================

    @Test
    void exchangeCodeForIdentity_systemBusy_throwsServiceException()
    {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"errcode\":-1,\"errmsg\":\"system busy\"}",
                        MediaType.APPLICATION_JSON));

        assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("some_code"));
    }

    // ==================== 高风险用户 ====================

    @Test
    void exchangeCodeForIdentity_highRiskUser_throwsServiceException()
    {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"errcode\":40226,\"errmsg\":\"high risk user\"}",
                        MediaType.APPLICATION_JSON));

        assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("some_code"));
    }

    // ==================== 超时 ====================

    @Test
    void exchangeCodeForIdentity_timeout_throwsServiceException()
    {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withException(new java.io.IOException("read timeout")));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("some_code"));

        assertFalse(ex.getMessage().contains("some_code"));
    }

    // ==================== 响应解析失败 ====================

    @Test
    void exchangeCodeForIdentity_invalidJson_throwsServiceException()
    {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "not a valid json",
                        MediaType.APPLICATION_JSON));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("some_code"));

        assertFalse(ex.getMessage().contains("some_code"));
    }

    // ==================== 空 openid ====================

    @Test
    void exchangeCodeForIdentity_emptyOpenid_throwsServiceException()
    {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"openid\":\"\",\"session_key\":\"sk\",\"errcode\":0}",
                        MediaType.APPLICATION_JSON));

        assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("some_code"));
    }

    // ==================== HTTP 5xx ====================

    @Test
    void exchangeCodeForIdentity_http5xx_throwsServiceException()
    {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withServerError());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("some_code"));

        assertFalse(ex.getMessage().contains("some_code"));
    }

    // ==================== 未配置 AppSecret ====================

    @Test
    void exchangeCodeForIdentity_appSecretNotConfigured_throwsServiceException()
    {
        WechatMiniProgramProperties emptyProps = new WechatMiniProgramProperties();
        emptyProps.setAppId("wxtestappid");
        emptyProps.setAppSecret(""); // 未配置
        emptyProps.setTimeoutMs(5000);
        emptyProps.setMaxRetries(0);

        RestClient.Builder builder = RestClient.builder();
        // 不需要 mockServer，因为应在发起 HTTP 请求前就失败
        WechatMiniProgramService emptyService =
                new WechatMiniProgramService(emptyProps, builder.build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> emptyService.exchangeCodeForIdentity("some_code"));

        // 不得发起 HTTP 请求
        assertFalse(ex.getMessage().toLowerCase().contains("appsecret"));
    }

    // ==================== 未配置 AppID ====================

    @Test
    void exchangeCodeForIdentity_appIdNotConfigured_throwsServiceException()
    {
        WechatMiniProgramProperties emptyProps = new WechatMiniProgramProperties();
        emptyProps.setAppId(""); // 未配置
        emptyProps.setAppSecret("somelongsecret");
        emptyProps.setTimeoutMs(5000);
        emptyProps.setMaxRetries(0);

        RestClient.Builder builder = RestClient.builder();
        WechatMiniProgramService emptyService =
                new WechatMiniProgramService(emptyProps, builder.build());

        assertThrows(ServiceException.class,
                () -> emptyService.exchangeCodeForIdentity("some_code"));
    }

    // ==================== 重试场景 ====================

    /**
     * 1. 网络超时（HTTP 5xx），第 2 次成功，最终登录成功。
     */
    @Test
    void retry_httpErrorThenSuccess_succeedsAfterRetry()
    {
        properties.setMaxRetries(2);
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withServerError());
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"openid\":\"openid_retry_success\",\"session_key\":\"sk\",\"errcode\":0}",
                        MediaType.APPLICATION_JSON));

        WechatMiniProgramService.WechatIdentity identity =
                service.exchangeCodeForIdentity("valid_code");

        assertNotNull(identity);
        assertEquals("openid_retry_success", identity.openid());
        mockServer.verify();
    }

    /**
     * 2. HTTP 503，第 3 次成功（max-retries=2，最多 3 次请求）。
     */
    @Test
    void retry_503ThenSuccess_succeedsOnThirdAttempt()
    {
        properties.setMaxRetries(2);
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"openid\":\"openid_503_success\",\"session_key\":\"sk\",\"errcode\":0}",
                        MediaType.APPLICATION_JSON));

        WechatMiniProgramService.WechatIdentity identity =
                service.exchangeCodeForIdentity("valid_code");

        assertNotNull(identity);
        assertEquals("openid_503_success", identity.openid());
        mockServer.verify();
    }

    /**
     * 3. 微信 -1（系统繁忙），达到最大次数后失败。
     */
    @Test
    void retry_systemBusyAllFail_throwsAfterMaxRetries()
    {
        properties.setMaxRetries(2);
        // 3 次系统繁忙（1 次初始 + 2 次重试）
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"errcode\":-1,\"errmsg\":\"system busy\"}",
                        MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"errcode\":-1,\"errmsg\":\"system busy\"}",
                        MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"errcode\":-1,\"errmsg\":\"system busy\"}",
                        MediaType.APPLICATION_JSON));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("valid_code"));

        assertTrue(ex.getMessage().contains("暂时不可用"));
        mockServer.verify();
    }

    /**
     * 4. 微信 40029（无效 code），只请求 1 次，不重试。
     */
    @Test
    void retry_invalidCodeDoesNotRetry_throwsImmediately()
    {
        properties.setMaxRetries(3);
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"errcode\":40029,\"errmsg\":\"invalid code\"}",
                        MediaType.APPLICATION_JSON));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("invalid_code"));

        assertTrue(ex.getMessage().contains("凭证已失效"));
        mockServer.verify();
    }

    /**
     * 5. 微信 40226（高风险账号），只请求 1 次，不重试。
     */
    @Test
    void retry_highRiskUserDoesNotRetry_throwsImmediately()
    {
        properties.setMaxRetries(3);
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"errcode\":40226,\"errmsg\":\"high risk\"}",
                        MediaType.APPLICATION_JSON));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("some_code"));

        assertTrue(ex.getMessage().contains("安全风险"));
        mockServer.verify();
    }

    /**
     * 6. max-retries=0 时只请求 1 次（不重试）。
     */
    @Test
    void retry_maxRetriesZero_onlyOneRequest()
    {
        properties.setMaxRetries(0);
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withServerError());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("valid_code"));

        assertTrue(ex.getMessage().contains("暂时不可用"));
        mockServer.verify(); // 只期望 1 次请求
    }

    /**
     * 7a. 负数 max-retries 被限制为安全默认值（1 次额外重试）。
     */
    @Test
    void retry_negativeMaxRetries_usesSafeDefault()
    {
        properties.setMaxRetries(-5);
        // 默认值=1，所以最多 2 次请求
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withServerError());
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"openid\":\"openid_negative\",\"session_key\":\"sk\",\"errcode\":0}",
                        MediaType.APPLICATION_JSON));

        WechatMiniProgramService.WechatIdentity identity =
                service.exchangeCodeForIdentity("valid_code");

        assertNotNull(identity);
        assertEquals("openid_negative", identity.openid());
        mockServer.verify();
    }

    /**
     * 7b. 超大值 max-retries 被限制为安全默认值（1 次额外重试）。
     */
    @Test
    void retry_oversizedMaxRetries_usesSafeDefault()
    {
        properties.setMaxRetries(999);
        // 默认值=1，所以最多 2 次请求
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withServerError());
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"openid\":\"openid_oversized\",\"session_key\":\"sk\",\"errcode\":0}",
                        MediaType.APPLICATION_JSON));

        WechatMiniProgramService.WechatIdentity identity =
                service.exchangeCodeForIdentity("valid_code");

        assertNotNull(identity);
        assertEquals("openid_oversized", identity.openid());
        mockServer.verify();
    }

    /**
     * 8. 重试过程中不输出 code、openid、unionid、AppSecret（通过异常信息校验）。
     */
    @Test
    void retry_errorMessagesDoNotLeakSensitiveInfo()
    {
        properties.setMaxRetries(2);
        String testCode = "sensitive_test_code_12345";
        String testSecret = properties.getAppSecret();

        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withServerError());
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withServerError());
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withServerError());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity(testCode));

        String msg = ex.getMessage();
        assertFalse(msg.contains(testCode), "异常信息不应包含 code");
        assertFalse(msg.contains(testSecret), "异常信息不应包含 AppSecret");
        assertFalse(msg.toLowerCase().contains("openid"), "异常信息不应包含 openid");
        assertFalse(msg.toLowerCase().contains("unionid"), "异常信息不应包含 unionid");
        mockServer.verify();
    }

    /**
     * 9. 总等待时间不会超过配置的最大范围。
     *
     * <p>max-retries=3（被限制为 3），最大等待 = 200+500+1000 + 3*50 = 1750 + 200ms 容差 = 1950ms。
     * 实际等待应小于 3000ms（含请求时间）。</p>
     */
    @Test
    void retry_totalWaitTimeWithinBounds()
    {
        properties.setMaxRetries(3);
        // 4 次 HTTP 500
        for (int i = 0; i < 4; i++)
        {
            mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                    "https://api.weixin.qq.com/sns/jscode2session")))
                    .andRespond(withServerError());
        }

        long start = System.currentTimeMillis();
        assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("valid_code"));
        long elapsed = System.currentTimeMillis() - start;

        // 最大退避 = (200+50) + (500+50) + (1000+50) = 1850ms，加请求时间应 < 5000ms
        assertTrue(elapsed < 5000,
                "总耗时 " + elapsed + "ms 应小于 5000ms（退避上限 1850ms + 请求时间）");
        // 至少等待了第一次退避（200ms）
        assertTrue(elapsed >= 200,
                "总耗时 " + elapsed + "ms 应至少包含第一次退避 200ms");
        mockServer.verify();
    }

    /**
     * 10. 并发登录请求不会产生无限重试或线程阻塞。
     *
     * <p>5 个并发线程同时调用，每个线程有独立的 service 和 mock 实例。
     * 所有线程应在合理时间内完成。</p>
     */
    @Test
    void retry_concurrentRequests_noInfiniteRetryOrBlocking() throws Exception
    {
        int threadCount = 5;
        java.util.concurrent.ExecutorService executor =
                java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.List<Throwable> errors = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int i = 0; i < threadCount; i++)
        {
            executor.submit(() -> {
                try
                {
                    // 每个线程需要独立的 service 实例和 mockServer
                    WechatMiniProgramProperties threadProps = new WechatMiniProgramProperties();
                    threadProps.setAppId("wxtestappid");
                    threadProps.setAppSecret("wxtestsecret");
                    threadProps.setMaxRetries(1);
                    RestClient.Builder threadBuilder = RestClient.builder();
                    MockRestServiceServer threadMock = MockRestServiceServer.bindTo(threadBuilder).build();
                    threadMock.expect(org.springframework.test.web.client.ExpectedCount.manyTimes(),
                            requestTo(org.hamcrest.Matchers.startsWith(
                                    "https://api.weixin.qq.com/sns/jscode2session")))
                            .andRespond(withSuccess(
                                    "{\"openid\":\"openid_concurrent\",\"session_key\":\"sk\",\"errcode\":0}",
                                    MediaType.APPLICATION_JSON));
                    WechatMiniProgramService threadService =
                            new WechatMiniProgramService(threadProps, threadBuilder.build());

                    WechatMiniProgramService.WechatIdentity identity =
                            threadService.exchangeCodeForIdentity("concurrent_code");
                    assertNotNull(identity);
                }
                catch (Throwable t)
                {
                    errors.add(t);
                }
                finally
                {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(completed, "并发请求应在 10 秒内完成，不应无限阻塞");
        assertTrue(errors.isEmpty(), "并发请求不应产生错误: " + errors);
    }

    // ==================== HTTP 状态码分类测试 ====================

    /**
     * 11. HTTP 400 不重试，只请求 1 次。
     */
    @Test
    void retry_http400DoesNotRetry_throwsImmediately()
    {
        properties.setMaxRetries(3);
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("valid_code"));

        assertTrue(ex.getMessage().contains("暂时不可用"));
        mockServer.verify(); // 只期望 1 次请求
    }

    /**
     * 12. HTTP 401 不重试，只请求 1 次。
     */
    @Test
    void retry_http401DoesNotRetry_throwsImmediately()
    {
        properties.setMaxRetries(3);
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("valid_code"));

        assertTrue(ex.getMessage().contains("暂时不可用"));
        mockServer.verify();
    }

    /**
     * 13. HTTP 403 不重试，只请求 1 次。
     */
    @Test
    void retry_http403DoesNotRetry_throwsImmediately()
    {
        properties.setMaxRetries(3);
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.FORBIDDEN));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("valid_code"));

        assertTrue(ex.getMessage().contains("暂时不可用"));
        mockServer.verify();
    }

    /**
     * 14. HTTP 404 不重试，只请求 1 次。
     */
    @Test
    void retry_http404DoesNotRetry_throwsImmediately()
    {
        properties.setMaxRetries(3);
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("valid_code"));

        assertTrue(ex.getMessage().contains("暂时不可用"));
        mockServer.verify();
    }

    /**
     * 15. HTTP 429（Too Many Requests）可重试，第 2 次成功。
     */
    @Test
    void retry_http429ThenSuccess_succeedsAfterRetry()
    {
        properties.setMaxRetries(2);
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"openid\":\"openid_429_success\",\"session_key\":\"sk\",\"errcode\":0}",
                        MediaType.APPLICATION_JSON));

        WechatMiniProgramService.WechatIdentity identity =
                service.exchangeCodeForIdentity("valid_code");

        assertNotNull(identity);
        assertEquals("openid_429_success", identity.openid());
        mockServer.verify();
    }

    // ==================== 响应格式错误不重试测试 ====================

    /**
     * 16. 空响应不重试，直接失败。
     */
    @Test
    void retry_emptyResponseDoesNotRetry_throwsImmediately()
    {
        properties.setMaxRetries(3);
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("valid_code"));

        assertTrue(ex.getMessage().contains("空响应"));
        mockServer.verify(); // 只期望 1 次请求
    }

    /**
     * 17. JSON 解析失败不重试，直接失败。
     */
    @Test
    void retry_parseFailureDoesNotRetry_throwsImmediately()
    {
        properties.setMaxRetries(3);
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "this is not valid json {{{",
                        MediaType.APPLICATION_JSON));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("valid_code"));

        assertTrue(ex.getMessage().contains("解析失败"));
        mockServer.verify(); // 只期望 1 次请求
    }

    // ==================== 网络连接异常重试测试 ====================

    /**
     * 18. 网络连接异常第一次失败，第二次成功。
     *
     * <p>模拟 ResourceAccessException（连接超时/DNS 失败等），无 HTTP 状态码，
     * 应触发重试。</p>
     */
    @Test
    void retry_networkErrorThenSuccess_succeedsAfterRetry()
    {
        properties.setMaxRetries(2);
        // 第一次抛出网络异常（无 HTTP 响应）
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(org.springframework.test.web.client.response
                        .MockRestResponseCreators.withException(
                                new java.net.ConnectException("Connection timed out")));
        // 第二次成功
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(withSuccess(
                        "{\"openid\":\"openid_network_success\",\"session_key\":\"sk\",\"errcode\":0}",
                        MediaType.APPLICATION_JSON));

        WechatMiniProgramService.WechatIdentity identity =
                service.exchangeCodeForIdentity("valid_code");

        assertNotNull(identity);
        assertEquals("openid_network_success", identity.openid());
        mockServer.verify();
    }

    /**
     * 19. 网络连接异常达到最大重试次数后失败。
     *
     * <p>模拟 SocketTimeoutException，无 HTTP 状态码，应触发重试，
     * 达到最大次数后返回统一脱敏提示。</p>
     */
    @Test
    void retry_networkErrorAllFail_throwsAfterMaxRetries()
    {
        properties.setMaxRetries(1);
        // 2 次网络异常（1 次初始 + 1 次重试）
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(org.springframework.test.web.client.response
                        .MockRestResponseCreators.withException(
                                new java.net.SocketTimeoutException("Read timed out")));
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(
                "https://api.weixin.qq.com/sns/jscode2session")))
                .andRespond(org.springframework.test.web.client.response
                        .MockRestResponseCreators.withException(
                                new java.net.SocketTimeoutException("Read timed out")));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.exchangeCodeForIdentity("valid_code"));

        assertTrue(ex.getMessage().contains("暂时不可用"));
        mockServer.verify();
    }
}
