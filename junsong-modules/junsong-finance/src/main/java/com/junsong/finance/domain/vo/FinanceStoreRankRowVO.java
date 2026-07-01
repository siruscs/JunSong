package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

public class FinanceStoreRankRowVO {
    private Long deptId;
    private String deptName;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal previousAmount = BigDecimal.ZERO;
    private BigDecimal changeRate = BigDecimal.ZERO;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPreviousAmount() { return previousAmount; }
    public void setPreviousAmount(BigDecimal previousAmount) { this.previousAmount = previousAmount; }
    public BigDecimal getChangeRate() { return changeRate; }
    public void setChangeRate(BigDecimal changeRate) { this.changeRate = changeRate; }
}
