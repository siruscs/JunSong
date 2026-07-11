package com.junsong.finance.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 费用核销批次。 */
public class FinExpenseVerifyBatch implements Serializable
{
    private static final long serialVersionUID = 1L;
    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_REVERSED = "REVERSED";
    public static final String SOURCE_NORMAL = "NORMAL";
    public static final String SOURCE_LEGACY = "LEGACY";

    private Long batchId;
    private String batchNo;
    private String requestId;
    private Long tenantId;
    private Long deptId;
    private BigDecimal totalExpenseAmount;
    private BigDecimal totalAdvanceAmount;
    private BigDecimal differenceAmount;
    private String status;
    private String sourceType;
    private String verifyBy;
    private Date verifyTime;
    private String reverseBy;
    private Date reverseTime;
    private String reverseReason;
    private String reverseRequestId;
    private Integer version;
    private Date createTime;
    private Date updateTime;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public BigDecimal getTotalExpenseAmount() { return totalExpenseAmount; }
    public void setTotalExpenseAmount(BigDecimal totalExpenseAmount) { this.totalExpenseAmount = totalExpenseAmount; }
    public BigDecimal getTotalAdvanceAmount() { return totalAdvanceAmount; }
    public void setTotalAdvanceAmount(BigDecimal totalAdvanceAmount) { this.totalAdvanceAmount = totalAdvanceAmount; }
    public BigDecimal getDifferenceAmount() { return differenceAmount; }
    public void setDifferenceAmount(BigDecimal differenceAmount) { this.differenceAmount = differenceAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getVerifyBy() { return verifyBy; }
    public void setVerifyBy(String verifyBy) { this.verifyBy = verifyBy; }
    public Date getVerifyTime() { return verifyTime; }
    public void setVerifyTime(Date verifyTime) { this.verifyTime = verifyTime; }
    public String getReverseBy() { return reverseBy; }
    public void setReverseBy(String reverseBy) { this.reverseBy = reverseBy; }
    public Date getReverseTime() { return reverseTime; }
    public void setReverseTime(Date reverseTime) { this.reverseTime = reverseTime; }
    public String getReverseReason() { return reverseReason; }
    public void setReverseReason(String reverseReason) { this.reverseReason = reverseReason; }
    public String getReverseRequestId() { return reverseRequestId; }
    public void setReverseRequestId(String reverseRequestId) { this.reverseRequestId = reverseRequestId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
