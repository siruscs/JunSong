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
    private BigDecimal saleQuantity;
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
    // R8-C: 可行动字段
    private BigDecimal cashInAmount;
    private BigDecimal netCashflowAmount;
    private Integer highRiskCount;
    private String primaryRisk;
    private String nextAction;
    // R11-B: 健康分 V2 字段
    private String healthSummary;
    private String healthScoreVersion;
    private BigDecimal authorizedAverageSales;
    private BigDecimal authorizedAverageProfitRate;
    private BigDecimal salesVsAuthorizedAverageRate;
    private BigDecimal profitRateVsAuthorizedAverage;
    private List<StoreHealthFactorVO> healthFactors;
    // R11-P1-fix: 复盘质量得分（完成率%）
    private BigDecimal reviewScore;

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

    public java.math.BigDecimal getSaleQuantity() { return saleQuantity; }
    public void setSaleQuantity(java.math.BigDecimal saleQuantity) { this.saleQuantity = saleQuantity; }

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

    public BigDecimal getCashInAmount() { return cashInAmount; }
    public void setCashInAmount(BigDecimal cashInAmount) { this.cashInAmount = cashInAmount; }

    public BigDecimal getNetCashflowAmount() { return netCashflowAmount; }
    public void setNetCashflowAmount(BigDecimal netCashflowAmount) { this.netCashflowAmount = netCashflowAmount; }

    public Integer getHighRiskCount() { return highRiskCount; }
    public void setHighRiskCount(Integer highRiskCount) { this.highRiskCount = highRiskCount; }

    public String getPrimaryRisk() { return primaryRisk; }
    public void setPrimaryRisk(String primaryRisk) { this.primaryRisk = primaryRisk; }

    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }

    public String getHealthSummary() { return healthSummary; }
    public void setHealthSummary(String healthSummary) { this.healthSummary = healthSummary; }

    public String getHealthScoreVersion() { return healthScoreVersion; }
    public void setHealthScoreVersion(String healthScoreVersion) { this.healthScoreVersion = healthScoreVersion; }

    public BigDecimal getAuthorizedAverageSales() { return authorizedAverageSales; }
    public void setAuthorizedAverageSales(BigDecimal authorizedAverageSales) { this.authorizedAverageSales = authorizedAverageSales; }

    public BigDecimal getAuthorizedAverageProfitRate() { return authorizedAverageProfitRate; }
    public void setAuthorizedAverageProfitRate(BigDecimal authorizedAverageProfitRate) { this.authorizedAverageProfitRate = authorizedAverageProfitRate; }

    public BigDecimal getSalesVsAuthorizedAverageRate() { return salesVsAuthorizedAverageRate; }
    public void setSalesVsAuthorizedAverageRate(BigDecimal salesVsAuthorizedAverageRate) { this.salesVsAuthorizedAverageRate = salesVsAuthorizedAverageRate; }

    public BigDecimal getProfitRateVsAuthorizedAverage() { return profitRateVsAuthorizedAverage; }
    public void setProfitRateVsAuthorizedAverage(BigDecimal profitRateVsAuthorizedAverage) { this.profitRateVsAuthorizedAverage = profitRateVsAuthorizedAverage; }

    public List<StoreHealthFactorVO> getHealthFactors() { return healthFactors; }
    public void setHealthFactors(List<StoreHealthFactorVO> healthFactors) { this.healthFactors = healthFactors; }

    public BigDecimal getReviewScore() { return reviewScore; }
    public void setReviewScore(BigDecimal reviewScore) { this.reviewScore = reviewScore; }
}
