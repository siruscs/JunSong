package com.junsong.open.config;

import com.junsong.open.context.OpenApiRequestContext;
import com.junsong.open.context.OpenApiRequestContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * OpenApiConfig 单元测试
 *
 * 断言内部调用拦截器透传 X-Open-* 开放上下文，且不再注入固定 admin 身份。
 *
 * @author junsong
 */
class OpenApiConfigTest
{
    private final OpenApiConfig config = new OpenApiConfig();

    @AfterEach
    void cleanup()
    {
        OpenApiRequestContextHolder.clear();
    }

    @Test
    void shouldForwardOpenContextHeadersWhenHolderIsSet() throws Exception
    {
        OpenApiRequestContext context = new OpenApiRequestContext();
        context.setAppId(7L);
        context.setAppKey("test-app-key");
        context.setTenantId(99L);
        context.setRequestId("req-abc");
        OpenApiRequestContextHolder.set(context);

        ClientHttpRequestInterceptor interceptor = config.internalCallInterceptor();
        MockClientHttpRequest request = new MockClientHttpRequest();
        final HttpHeaders[] captured = new HttpHeaders[1];
        ClientHttpRequestExecution execution = (req, body) -> {
            captured[0] = req.getHeaders();
            return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        };

        interceptor.intercept(request, new byte[0], execution);

        assertNotNull(captured[0]);
        assertEquals("7", captured[0].getFirst("X-Open-App-Id"));
        assertEquals("test-app-key", captured[0].getFirst("X-Open-App-Key"));
        assertEquals("99", captured[0].getFirst("X-Open-Tenant-Id"));
        assertEquals("req-abc", captured[0].getFirst("X-Open-Request-Id"));
        assertEquals("open-api", captured[0].getFirst("from-source"));
    }

    @Test
    void shouldNotInjectFixedAdminIdentity() throws Exception
    {
        ClientHttpRequestInterceptor interceptor = config.internalCallInterceptor();
        MockClientHttpRequest request = new MockClientHttpRequest();
        final HttpHeaders[] captured = new HttpHeaders[1];
        ClientHttpRequestExecution execution = (req, body) -> {
            captured[0] = req.getHeaders();
            return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        };

        interceptor.intercept(request, new byte[0], execution);

        assertNotNull(captured[0]);
        assertNull(captured[0].getFirst("user_id"), "must not inject user_id=1");
        assertNull(captured[0].getFirst("username"), "must not inject admin username");
        assertNull(captured[0].getFirst("user_key"), "must not inject fixed openapi-internal user key");
    }

    @Test
    void shouldNotForwardContextWhenHolderIsEmpty() throws Exception
    {
        ClientHttpRequestInterceptor interceptor = config.internalCallInterceptor();
        MockClientHttpRequest request = new MockClientHttpRequest();
        final HttpHeaders[] captured = new HttpHeaders[1];
        ClientHttpRequestExecution execution = (req, body) -> {
            captured[0] = req.getHeaders();
            return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        };

        interceptor.intercept(request, new byte[0], execution);

        assertNotNull(captured[0]);
        assertEquals("open-api", captured[0].getFirst("from-source"));
        assertNull(captured[0].getFirst("X-Open-App-Id"));
    }
}
