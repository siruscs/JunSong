package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;

public class StoreOperationSummaryVO {
    private Long deptId;
    private String deptName;
    private BigDecimal totalSales;
    private Integer saleCount;
    private BigDecimal saleQuantity;
    private BigDecimal avgOrderAmount;
    private BigDecimal totalExpense;
    private BigDecimal unverifiedExpense;
    private BigDecimal unverifiedAdvance;
    private BigDecimal operatingProfit;
    private BigDecimal operatingProfitRate;
    private String accountingPeriodStatus;
    private List<String> alerts;
    private List<StoreTrendRowVO> trendRows;
    private List<StoreExpenseCategoryVO> expenseCategories;
    private List<StorePendingItemVO> pendingItems;
    private BigDecimal previousTotalSales;
    private BigDecimal previousTotalExpense;
    private BigDecimal previousOperatingProfit;
    private BigDecimal salesChangeRate;
    private BigDecimal expenseChangeRate;
    private BigDecimal profitChangeRate;
    private List<String> suggestions;

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public Integer getSaleCount() {
        return saleCount;
    }

    public void setSaleCount(Integer saleCount) {
        this.saleCount = saleCount;
    }

    public java.math.BigDecimal getSaleQuantity() {
        return saleQuantity;
    }

    public void setSaleQuantity(java.math.BigDecimal saleQuantity) {
        this.saleQuantity = saleQuantity;
    }

    public BigDecimal getAvgOrderAmount() {
        return avgOrderAmount;
    }

    public void setAvgOrderAmount(BigDecimal avgOrderAmount) {
        this.avgOrderAmount = avgOrderAmount;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getUnverifiedExpense() {
        return unverifiedExpense;
    }

    public void setUnverifiedExpense(BigDecimal unverifiedExpense) {
        this.unverifiedExpense = unverifiedExpense;
    }

    public BigDecimal getUnverifiedAdvance() {
        return unverifiedAdvance;
    }

    public void setUnverifiedAdvance(BigDecimal unverifiedAdvance) {
        this.unverifiedAdvance = unverifiedAdvance;
    }

    public BigDecimal getOperatingProfit() {
        return operatingProfit;
    }

    public void setOperatingProfit(BigDecimal operatingProfit) {
        this.operatingProfit = operatingProfit;
    }

    public BigDecimal getOperatingProfitRate() {
        return operatingProfitRate;
    }

    public void setOperatingProfitRate(BigDecimal operatingProfitRate) {
        this.operatingProfitRate = operatingProfitRate;
    }

    public String getAccountingPeriodStatus() {
        return accountingPeriodStatus;
    }

    public void setAccountingPeriodStatus(String accountingPeriodStatus) {
        this.accountingPeriodStatus = accountingPeriodStatus;
    }

    public List<String> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<String> alerts) {
        this.alerts = alerts;
    }

    public List<StoreTrendRowVO> getTrendRows() {
        return trendRows;
    }

    public void setTrendRows(List<StoreTrendRowVO> trendRows) {
        this.trendRows = trendRows;
    }

    public List<StoreExpenseCategoryVO> getExpenseCategories() {
        return expenseCategories;
    }

    public void setExpenseCategories(List<StoreExpenseCategoryVO> expenseCategories) {
        this.expenseCategories = expenseCategories;
    }

    public List<StorePendingItemVO> getPendingItems() {
        return pendingItems;
    }

    public void setPendingItems(List<StorePendingItemVO> pendingItems) {
        this.pendingItems = pendingItems;
    }

    public BigDecimal getPreviousTotalSales() {
        return previousTotalSales;
    }

    public void setPreviousTotalSales(BigDecimal previousTotalSales) {
        this.previousTotalSales = previousTotalSales;
    }

    public BigDecimal getPreviousTotalExpense() {
        return previousTotalExpense;
    }

    public void setPreviousTotalExpense(BigDecimal previousTotalExpense) {
        this.previousTotalExpense = previousTotalExpense;
    }

    public BigDecimal getPreviousOperatingProfit() {
        return previousOperatingProfit;
    }

    public void setPreviousOperatingProfit(BigDecimal previousOperatingProfit) {
        this.previousOperatingProfit = previousOperatingProfit;
    }

    public BigDecimal getSalesChangeRate() {
        return salesChangeRate;
    }

    public void setSalesChangeRate(BigDecimal salesChangeRate) {
        this.salesChangeRate = salesChangeRate;
    }

    public BigDecimal getExpenseChangeRate() {
        return expenseChangeRate;
    }

    public void setExpenseChangeRate(BigDecimal expenseChangeRate) {
        this.expenseChangeRate = expenseChangeRate;
    }

    public BigDecimal getProfitChangeRate() {
        return profitChangeRate;
    }

    public void setProfitChangeRate(BigDecimal profitChangeRate) {
        this.profitChangeRate = profitChangeRate;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
