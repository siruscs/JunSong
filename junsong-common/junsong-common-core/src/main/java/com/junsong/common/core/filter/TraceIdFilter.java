package com.junsong.common.core.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 全链路 TraceId 过滤器
 *
 * <p>为每个 HTTP 请求生成唯一的 traceId，写入 MDC 和响应头，实现跨服务链路追踪。</p>
 *
 * @author junsong
 */
@Component
@Order(-100)
public class TraceIdFilter implements Filter
{
    public static final String TRACE_ID_KEY = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty())
        {
            traceId = generateTraceId();
        }

        MDC.put(TRACE_ID_KEY, traceId);
        httpResponse.setHeader(TRACE_ID_HEADER, traceId);

        try
        {
            chain.doFilter(request, response);
        }
        finally
        {
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private String generateTraceId()
    {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
