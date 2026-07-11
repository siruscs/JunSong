package com.junsong.open.interceptor;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.open.context.OpenApiRequestContext;
import com.junsong.open.context.OpenApiRequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenApiContextInterceptor 单元测试
 *
 * @author junsong
 */
class OpenApiContextInterceptorTest
{
    private final OpenApiContextInterceptor interceptor = new OpenApiContextInterceptor();

    @AfterEach
    void cleanup()
    {
        OpenApiRequestContextHolder.clear();
    }

    @Test
    void shouldFailClosedWhenOpenApiPathMissingHeaders()
    {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/open/apps");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(ServiceException.class,
                () -> interceptor.preHandle(request, response, null),
                "missing X-Open-* headers must fail closed");
        assertNull(OpenApiRequestContextHolder.get(), "holder must remain empty when context is rejected");
    }

    @Test
    void shouldWriteHolderWhenHeadersComplete()
    {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/open/apps");
        request.addHeader("X-Open-App-Id", "7");
        request.addHeader("X-Open-App-Key", "test-app-key");
        request.addHeader("X-Open-Tenant-Id", "99");
        request.addHeader("X-Open-Key-Type", "production");
        request.addHeader("X-Open-Request-Id", "req-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, null);

        assertTrue(result, "preHandle should return true when context is valid");
        OpenApiRequestContext context = OpenApiRequestContextHolder.get();
        assertNotNull(context, "holder must be populated");
        assertEquals(7L, context.getAppId());
        assertEquals("test-app-key", context.getAppKey());
        assertEquals(99L, context.getTenantId());
        assertEquals("production", context.getKeyType());
        assertEquals("req-abc", context.getRequestId());
    }

    @Test
    void shouldClearHolderInAfterCompletion()
    {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/members");
        request.addHeader("X-Open-App-Id", "1");
        request.addHeader("X-Open-App-Key", "k");
        request.addHeader("X-Open-Tenant-Id", "10");
        request.addHeader("X-Open-Key-Type", "production");
        request.addHeader("X-Open-Request-Id", "r1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, null);
        assertNotNull(OpenApiRequestContextHolder.get());

        interceptor.afterCompletion(request, response, null, null);
        assertNull(OpenApiRequestContextHolder.get(), "afterCompletion must clear the holder");
    }

    @Test
    void shouldPassThroughNonOpenApiPathWithoutHeaders()
    {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/apiLog/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, null);

        assertTrue(result, "non-openapi paths must pass through without X-Open-* headers");
        assertNull(OpenApiRequestContextHolder.get());
    }
}
