package com.junsong.member.domain;

import java.util.Date;

/** 跨机构配置同步明细。JSON 快照仅用于差异展示和审计。 */
public class MemConfigSyncDetail
{
    private Long detailId;
    private Long batchId;
    private Long tenantId;
    private Long targetDeptId;
    private Long targetPeriodId;
    private String businessKey;
    private Long sourceRecordId;
    private Long targetRecordId;
    private String operation;
    private String decision;
    private String sourceSnapshot;
    private String targetSnapshot;
    private String diffSnapshot;
    private Long sourceRowVersion;
    private Long targetRowVersion;
    private String resultStatus;
    private String errorCode;
    private String errorMessage;
    private Date resultTime;
    private Date createTime;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long value) { detailId = value; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long value) { batchId = value; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long value) { tenantId = value; }
    public Long getTargetDeptId() { return targetDeptId; }
    public void setTargetDeptId(Long value) { targetDeptId = value; }
    public Long getTargetPeriodId() { return targetPeriodId; }
    public void setTargetPeriodId(Long value) { targetPeriodId = value; }
    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String value) { businessKey = value; }
    public Long getSourceRecordId() { return sourceRecordId; }
    public void setSourceRecordId(Long value) { sourceRecordId = value; }
    public Long getTargetRecordId() { return targetRecordId; }
    public void setTargetRecordId(Long value) { targetRecordId = value; }
    public String getOperation() { return operation; }
    public void setOperation(String value) { operation = value; }
    public String getDecision() { return decision; }
    public void setDecision(String value) { decision = value; }
    public String getSourceSnapshot() { return sourceSnapshot; }
    public void setSourceSnapshot(String value) { sourceSnapshot = value; }
    public String getTargetSnapshot() { return targetSnapshot; }
    public void setTargetSnapshot(String value) { targetSnapshot = value; }
    public String getDiffSnapshot() { return diffSnapshot; }
    public void setDiffSnapshot(String value) { diffSnapshot = value; }
    public Long getSourceRowVersion() { return sourceRowVersion; }
    public void setSourceRowVersion(Long value) { sourceRowVersion = value; }
    public Long getTargetRowVersion() { return targetRowVersion; }
    public void setTargetRowVersion(Long value) { targetRowVersion = value; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String value) { resultStatus = value; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String value) { errorCode = value; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String value) { errorMessage = value; }
    public Date getResultTime() { return resultTime; }
    public void setResultTime(Date value) { resultTime = value; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date value) { createTime = value; }
}
