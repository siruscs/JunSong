package com.junsong.open.interceptor;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.open.context.OpenApiRequestContext;
import com.junsong.open.context.OpenApiRequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 开放平台请求上下文拦截器
 *
 * 从网关注入的 X-Open-* 头解析可信上下文到线程变量；缺 header 时 fail closed。
 * 仅对公共 openapi 控制器路径生效，管理后台路径仍走用户登录鉴权。
 *
 * @author junsong
 */
@Component
public class OpenApiContextInterceptor implements HandlerInterceptor
{
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        String path = request.getRequestURI();
        if (!isOpenApiPath(path))
        {
            return true;
        }

        String appId = request.getHeader("X-Open-App-Id");
        String appKey = request.getHeader("X-Open-App-Key");
        String tenantId = request.getHeader("X-Open-Tenant-Id");
        String keyType = request.getHeader("X-Open-Key-Type");
        String requestId = request.getHeader("X-Open-Request-Id");

        if (appId == null || appKey == null || tenantId == null || keyType == null || requestId == null)
        {
            throw new ServiceException("开放平台上下文缺失");
        }

        OpenApiRequestContext context = new OpenApiRequestContext();
        context.setAppId(Long.valueOf(appId));
        context.setAppKey(appKey);
        context.setTenantId(Long.valueOf(tenantId));
        context.setKeyType(keyType);
        context.setRequestId(requestId);
        OpenApiRequestContextHolder.set(context);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
    {
        OpenApiRequestContextHolder.clear();
    }

    private boolean isOpenApiPath(String path)
    {
        if (path == null)
        {
            return false;
        }
        return path.startsWith("/members")
                || path.startsWith("/workflow")
                || path.startsWith("/store-openings")
                || path.startsWith("/store-opening")
                || path.startsWith("/open/apps")
                || path.startsWith("/open/webhooks");
    }
}
