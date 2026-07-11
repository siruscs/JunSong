package com.junsong.open.context;

/**
 * 开放平台可信请求上下文
 *
 * 由网关验签后注入的 X-Open-* 头解析得到，开放服务内部调用下游时只能透传该上下文，
 * 不得再用固定 admin 身份冒用。
 *
 * @author junsong
 */
public class OpenApiRequestContext
{
    private Long appId;
    private String appKey;
    private Long tenantId;
    private String keyType;
    private String requestId;

    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getKeyType() { return keyType; }
    public void setKeyType(String keyType) { this.keyType = keyType; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
