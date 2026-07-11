package com.junsong.open.interceptor;

import com.junsong.common.core.utils.ip.IpUtils;
import com.junsong.open.context.OpenApiRequestContext;
import com.junsong.open.context.OpenApiRequestContextHolder;
import com.junsong.open.domain.OpenApiLog;
import com.junsong.open.service.IOpenApiLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 开放平台调用日志拦截器
 *
 * 在 afterCompletion 阶段将每次调用的请求身份、响应状态、耗时和错误码落库，
 * 确保开放 API 调用可审计。
 *
 * 注册顺序：必须在 OpenApiContextInterceptor 之后注册，
 * 这样 afterCompletion 在 OpenApiContextContextHolder.clear() 之前执行。
 *
 * @author junsong
 */
@Component
public class OpenApiLogInterceptor implements HandlerInterceptor
{
    private static final Logger log = LoggerFactory.getLogger(OpenApiLogInterceptor.class);

    private static final String START_TIME_ATTR = "openApiLogStartTime";

    @Autowired
    private IOpenApiLogService openApiLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
    {
        try
        {
            OpenApiLog apiLog = new OpenApiLog();
            apiLog.setRequestMethod(request.getMethod());
            apiLog.setRequestPath(request.getRequestURI());
            apiLog.setRequestIp(IpUtils.getIpAddr(request));
            apiLog.setResponseCode(response.getStatus());

            Object startTimeObj = request.getAttribute(START_TIME_ATTR);
            if (startTimeObj instanceof Long startTime)
            {
                apiLog.setResponseTime((int) (System.currentTimeMillis() - startTime));
            }

            OpenApiRequestContext context = OpenApiRequestContextHolder.get();
            if (context != null)
            {
                apiLog.setAppId(context.getAppId());
                apiLog.setAppKey(context.getAppKey());
                apiLog.setTenantId(context.getTenantId());
                apiLog.setRequestId(context.getRequestId());
                apiLog.setKeyType(context.getKeyType());
            }

            int status = response.getStatus();
            if (status >= 200 && status < 300)
            {
                apiLog.setStatus("success");
            }
            else
            {
                apiLog.setStatus("fail");
                if (status == 403)
                {
                    apiLog.setErrorCode("OPEN_PERMISSION_DENIED");
                }
                else if (status == 404)
                {
                    apiLog.setErrorCode("OPEN_NOT_FOUND");
                }
                else if (status >= 500)
                {
                    apiLog.setErrorCode("OPEN_INTERNAL_ERROR");
                }
            }

            if (ex != null && apiLog.getErrorCode() == null)
            {
                apiLog.setErrorCode("OPEN_INTERNAL_ERROR");
                apiLog.setResponseMessage(ex.getMessage() != null && ex.getMessage().length() > 500
                        ? ex.getMessage().substring(0, 500) : ex.getMessage());
            }

            openApiLogService.insertOpenApiLog(apiLog);
        }
        catch (Exception e)
        {
            log.warn("[OpenApiLog] 日志写入失败: {}", e.getMessage());
        }
    }
}
