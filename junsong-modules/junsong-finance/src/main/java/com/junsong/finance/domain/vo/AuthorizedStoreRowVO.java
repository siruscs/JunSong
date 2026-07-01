package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AuthorizedStoreRowVO {
    private Long deptId;
    private String deptName;
    private BigDecimal totalSales;
    private BigDecimal totalExpense;
    private BigDecimal operatingProfit;
    private BigDecimal operatingProfitRate;
    private BigDecimal unverifiedAmount;
    private BigDecimal memberSalesAmount;
    private BigDecimal memberSalesRatio;
    private Integer saleCount;
    private Integer saleQuantity;
    private BigDecimal avgOrderAmount;
    private BigDecimal salesChangeRate;
    private BigDecimal expenseChangeRate;
    private BigDecimal profitChangeRate;
    private Integer healthScore;
    private String healthLevel;
    private Map<String, Integer> healthBreakdown;
    private Integer alertCount;
    private Integer reviewTaskCount;
    private List<String> reviewReasons;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal totalSales) { this.totalSales = totalSales; }

    public BigDecimal getTotalExpense() { return totalExpense; }
    public void setTotalExpense(BigDecimal totalExpense) { this.totalExpense = totalExpense; }

    public BigDecimal getOperatingProfit() { return operatingProfit; }
    public void setOperatingProfit(BigDecimal operatingProfit) { this.operatingProfit = operatingProfit; }

    public BigDecimal getOperatingProfitRate() { return operatingProfitRate; }
    public void setOperatingProfitRate(BigDecimal operatingProfitRate) { this.operatingProfitRate = operatingProfitRate; }

    public BigDecimal getUnverifiedAmount() { return unverifiedAmount; }
    public void setUnverifiedAmount(BigDecimal unverifiedAmount) { this.unverifiedAmount = unverifiedAmount; }

    public Integer getSaleCount() { return saleCount; }
    public void setSaleCount(Integer saleCount) { this.saleCount = saleCount; }

    public Integer getSaleQuantity() { return saleQuantity; }
    public void setSaleQuantity(Integer saleQuantity) { this.saleQuantity = saleQuantity; }

    public BigDecimal getAvgOrderAmount() { return avgOrderAmount; }
    public void setAvgOrderAmount(BigDecimal avgOrderAmount) { this.avgOrderAmount = avgOrderAmount; }

    public BigDecimal getSalesChangeRate() { return salesChangeRate; }
    public void setSalesChangeRate(BigDecimal salesChangeRate) { this.salesChangeRate = salesChangeRate; }

    public BigDecimal getExpenseChangeRate() { return expenseChangeRate; }
    public void setExpenseChangeRate(BigDecimal expenseChangeRate) { this.expenseChangeRate = expenseChangeRate; }

    public BigDecimal getProfitChangeRate() { return profitChangeRate; }
    public void setProfitChangeRate(BigDecimal profitChangeRate) { this.profitChangeRate = profitChangeRate; }

    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }

    public String getHealthLevel() { return healthLevel; }
    public void setHealthLevel(String healthLevel) { this.healthLevel = healthLevel; }

    public List<String> getReviewReasons() { return reviewReasons; }
    public void setReviewReasons(List<String> reviewReasons) { this.reviewReasons = reviewReasons; }

    public BigDecimal getMemberSalesAmount() { return memberSalesAmount; }
    public void setMemberSalesAmount(BigDecimal memberSalesAmount) { this.memberSalesAmount = memberSalesAmount; }

    public BigDecimal getMemberSalesRatio() { return memberSalesRatio; }
    public void setMemberSalesRatio(BigDecimal memberSalesRatio) { this.memberSalesRatio = memberSalesRatio; }

    public Map<String, Integer> getHealthBreakdown() { return healthBreakdown; }
    public void setHealthBreakdown(Map<String, Integer> healthBreakdown) { this.healthBreakdown = healthBreakdown; }

    public Integer getAlertCount() { return alertCount; }
    public void setAlertCount(Integer alertCount) { this.alertCount = alertCount; }

    public Integer getReviewTaskCount() { return reviewTaskCount; }
    public void setReviewTaskCount(Integer reviewTaskCount) { this.reviewTaskCount = reviewTaskCount; }
}
