package com.junsong.common.core.idempotency;

import java.util.Date;

/**
 * 幂等记录实体。
 *
 * 作用域：tenantId + scene + idempotencyKey（唯一索引保证）
 * 状态机：PROCESSING → SUCCEEDED / FAILED
 *
 * @author junsong
 */
public class IdempotencyRecord {

    private Long recordId;
    private Long tenantId;
    private String scene;
    private String idempotencyKey;
    private String status;
    private String fingerprint;
    private String resourceType;
    private String resourceId;
    private String resultSummary;
    private String errorSummary;
    private Date createdTime;
    private Date updatedTime;
    private Date expireTime;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }

    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }

    public Date getCreatedTime() { return createdTime; }
    public void setCreatedTime(Date createdTime) { this.createdTime = createdTime; }

    public Date getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Date updatedTime) { this.updatedTime = updatedTime; }

    public Date getExpireTime() { return expireTime; }
    public void setExpireTime(Date expireTime) { this.expireTime = expireTime; }
}
