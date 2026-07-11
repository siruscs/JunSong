package com.junsong.finance.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 费用核销借支明细快照。 */
public class FinAdvanceVerifyDetail implements Serializable
{
    private static final long serialVersionUID = 1L;
    public static final String RELATION_SOURCE = "SOURCE";
    public static final String RELATION_SUPPLEMENT = "SUPPLEMENT";
    public static final String RELATION_SURPLUS = "SURPLUS";

    private Long detailId;
    private Long batchId;
    private Long advanceId;
    private Long tenantId;
    private Long deptId;
    private BigDecimal advanceAmount;
    private String originalStatus;
    private Long periodId;
    private String relationType;
    private String generatedFlag;
    private Date createTime;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getAdvanceId() { return advanceId; }
    public void setAdvanceId(Long advanceId) { this.advanceId = advanceId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public BigDecimal getAdvanceAmount() { return advanceAmount; }
    public void setAdvanceAmount(BigDecimal advanceAmount) { this.advanceAmount = advanceAmount; }
    public String getOriginalStatus() { return originalStatus; }
    public void setOriginalStatus(String originalStatus) { this.originalStatus = originalStatus; }
    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public String getGeneratedFlag() { return generatedFlag; }
    public void setGeneratedFlag(String generatedFlag) { this.generatedFlag = generatedFlag; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
