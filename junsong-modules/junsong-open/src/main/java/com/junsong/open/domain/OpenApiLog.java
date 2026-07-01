package com.junsong.open.domain;

import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * API调用日志对象 open_api_log
 *
 * @author junsong
 */
public class OpenApiLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    @Excel(name = "应用ID")
    private Long appId;

    @Excel(name = "AppKey")
    private String appKey;

    @Excel(name = "请求方法")
    private String requestMethod;

    @Excel(name = "请求路径")
    private String requestPath;

    @Excel(name = "请求IP")
    private String requestIp;

    @Excel(name = "响应状态码")
    private Integer responseCode;

    @Excel(name = "响应耗时(ms)")
    private Integer responseTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    public Long getAppId()
    {
        return appId;
    }

    public void setAppId(Long appId)
    {
        this.appId = appId;
    }

    public String getAppKey()
    {
        return appKey;
    }

    public void setAppKey(String appKey)
    {
        this.appKey = appKey;
    }

    public String getRequestMethod()
    {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod)
    {
        this.requestMethod = requestMethod;
    }

    public String getRequestPath()
    {
        return requestPath;
    }

    public void setRequestPath(String requestPath)
    {
        this.requestPath = requestPath;
    }

    public String getRequestIp()
    {
        return requestIp;
    }

    public void setRequestIp(String requestIp)
    {
        this.requestIp = requestIp;
    }

    public Integer getResponseCode()
    {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode)
    {
        this.responseCode = responseCode;
    }

    public Integer getResponseTime()
    {
        return responseTime;
    }

    public void setResponseTime(Integer responseTime)
    {
        this.responseTime = responseTime;
    }
}
