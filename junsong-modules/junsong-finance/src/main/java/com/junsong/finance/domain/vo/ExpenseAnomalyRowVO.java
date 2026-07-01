package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

public class ExpenseAnomalyRowVO {
    private String anomalyType;
    private String label;
    private Long deptId;
    private String deptName;
    private BigDecimal currentAmount = BigDecimal.ZERO;
    private BigDecimal previousAmount = BigDecimal.ZERO;
    private BigDecimal changeRate = BigDecimal.ZERO;
    private Long expenseId;
    private String expenseNo;
    private String detail;

    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }
    public BigDecimal getPreviousAmount() { return previousAmount; }
    public void setPreviousAmount(BigDecimal previousAmount) { this.previousAmount = previousAmount; }
    public BigDecimal getChangeRate() { return changeRate; }
    public void setChangeRate(BigDecimal changeRate) { this.changeRate = changeRate; }
    public Long getExpenseId() { return expenseId; }
    public void setExpenseId(Long expenseId) { this.expenseId = expenseId; }
    public String getExpenseNo() { return expenseNo; }
    public void setExpenseNo(String expenseNo) { this.expenseNo = expenseNo; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
