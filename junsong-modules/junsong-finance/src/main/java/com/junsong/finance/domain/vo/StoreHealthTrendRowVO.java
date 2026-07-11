package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

public class StoreHealthTrendRowVO {
    private Long deptId;
    private String deptName;
    private String periodLabel;
    private Integer healthScore;
    private String healthLevel;
    private BigDecimal totalSales;
    private BigDecimal totalExpense;
    private BigDecimal operatingProfitRate;
    private BigDecimal netCashflowAmount;
    private Integer highRiskCount;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getPeriodLabel() { return periodLabel; }
    public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }
    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }
    public String getHealthLevel() { return healthLevel; }
    public void setHealthLevel(String healthLevel) { this.healthLevel = healthLevel; }
    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal totalSales) { this.totalSales = totalSales; }
    public BigDecimal getTotalExpense() { return totalExpense; }
    public void setTotalExpense(BigDecimal totalExpense) { this.totalExpense = totalExpense; }
    public BigDecimal getOperatingProfitRate() { return operatingProfitRate; }
    public void setOperatingProfitRate(BigDecimal operatingProfitRate) { this.operatingProfitRate = operatingProfitRate; }
    public BigDecimal getNetCashflowAmount() { return netCashflowAmount; }
    public void setNetCashflowAmount(BigDecimal netCashflowAmount) { this.netCashflowAmount = netCashflowAmount; }
    public Integer getHighRiskCount() { return highRiskCount; }
    public void setHighRiskCount(Integer highRiskCount) { this.highRiskCount = highRiskCount; }
}
