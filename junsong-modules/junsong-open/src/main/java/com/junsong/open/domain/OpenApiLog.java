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

    @Excel(name = "请求ID")
    private String requestId;

    @Excel(name = "错误码")
    private String errorCode;

    @Excel(name = "调用状态")
    private String status;

    @Excel(name = "Key类型")
    private String keyType;

    @Excel(name = "响应摘要")
    private String responseMessage;

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

    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    public String getErrorCode()
    {
        return errorCode;
    }

    public void setErrorCode(String errorCode)
    {
        this.errorCode = errorCode;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getKeyType()
    {
        return keyType;
    }

    public void setKeyType(String keyType)
    {
        this.keyType = keyType;
    }

    public String getResponseMessage()
    {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage)
    {
        this.responseMessage = responseMessage;
    }
}
