package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.Date;

public class ProfitDrilldownRowVO {
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long deptId;
    private String deptName;
    private BigDecimal amount = BigDecimal.ZERO;
    private Date occurTime;

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getSourceNo() { return sourceNo; }
    public void setSourceNo(String sourceNo) { this.sourceNo = sourceNo; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Date getOccurTime() { return occurTime; }
    public void setOccurTime(Date occurTime) { this.occurTime = occurTime; }
}
