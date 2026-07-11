package com.junsong.finance.domain.vo;

public class ExpenseOperationCapabilityVO
{
    private boolean canVerify;
    private boolean canUnverify;
    private Long batchId;
    private String operationDisabledReason;
    public boolean isCanVerify() { return canVerify; }
    public void setCanVerify(boolean canVerify) { this.canVerify = canVerify; }
    public boolean isCanUnverify() { return canUnverify; }
    public void setCanUnverify(boolean canUnverify) { this.canUnverify = canUnverify; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getOperationDisabledReason() { return operationDisabledReason; }
    public void setOperationDisabledReason(String operationDisabledReason) { this.operationDisabledReason = operationDisabledReason; }
}
