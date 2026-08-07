package com.junsong.member.domain;

import java.util.Date;

/** 跨机构配置同步批次。 */
public class MemConfigSyncBatch
{
    private Long batchId;
    private Long tenantId;
    private Long sourceDeptId;
    private String syncType;
    private Long previewVersion;
    private String status;
    private String idempotencyKey;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private String remark;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long value) { batchId = value; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long value) { tenantId = value; }
    public Long getSourceDeptId() { return sourceDeptId; }
    public void setSourceDeptId(Long value) { sourceDeptId = value; }
    public String getSyncType() { return syncType; }
    public void setSyncType(String value) { syncType = value; }
    public Long getPreviewVersion() { return previewVersion; }
    public void setPreviewVersion(Long value) { previewVersion = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { idempotencyKey = value; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String value) { createBy = value; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date value) { createTime = value; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String value) { updateBy = value; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date value) { updateTime = value; }
    public String getRemark() { return remark; }
    public void setRemark(String value) { remark = value; }
}
