package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;

public class FinanceOperationDashboardVO {
    private BigDecimal todaySales = BigDecimal.ZERO;
    private BigDecimal monthSales = BigDecimal.ZERO;
    private BigDecimal todayExpense = BigDecimal.ZERO;
    private BigDecimal monthExpense = BigDecimal.ZERO;
    private BigDecimal grossProfit = BigDecimal.ZERO;
    private BigDecimal netProfit = BigDecimal.ZERO;
    private BigDecimal profitRate = BigDecimal.ZERO;
    private List<FinanceStoreRankRowVO> salesTopStores;
    private List<FinanceStoreRankRowVO> profitTopStores;
    private List<FinanceStoreRankRowVO> expenseAnomalyStores;
    private List<FinanceMetricCardVO> trendCards;
    private int unverifiedExpenseCount;
    private BigDecimal unverifiedExpenseAmount = BigDecimal.ZERO;
    private int unverifiedAdvanceCount;
    private BigDecimal unverifiedAdvanceAmount = BigDecimal.ZERO;
    private int unsettledProfitShareCount;
    private String currentPeriodStatus;
    private List<FinanceWarningVO> warnings;

    // Generate all getters and setters
    public BigDecimal getTodaySales() { return todaySales; }
    public void setTodaySales(BigDecimal todaySales) { this.todaySales = todaySales; }
    public BigDecimal getMonthSales() { return monthSales; }
    public void setMonthSales(BigDecimal monthSales) { this.monthSales = monthSales; }
    public BigDecimal getTodayExpense() { return todayExpense; }
    public void setTodayExpense(BigDecimal todayExpense) { this.todayExpense = todayExpense; }
    public BigDecimal getMonthExpense() { return monthExpense; }
    public void setMonthExpense(BigDecimal monthExpense) { this.monthExpense = monthExpense; }
    public BigDecimal getGrossProfit() { return grossProfit; }
    public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }
    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
    public BigDecimal getProfitRate() { return profitRate; }
    public void setProfitRate(BigDecimal profitRate) { this.profitRate = profitRate; }
    public List<FinanceStoreRankRowVO> getSalesTopStores() { return salesTopStores; }
    public void setSalesTopStores(List<FinanceStoreRankRowVO> salesTopStores) { this.salesTopStores = salesTopStores; }
    public List<FinanceStoreRankRowVO> getProfitTopStores() { return profitTopStores; }
    public void setProfitTopStores(List<FinanceStoreRankRowVO> profitTopStores) { this.profitTopStores = profitTopStores; }
    public List<FinanceStoreRankRowVO> getExpenseAnomalyStores() { return expenseAnomalyStores; }
    public void setExpenseAnomalyStores(List<FinanceStoreRankRowVO> expenseAnomalyStores) { this.expenseAnomalyStores = expenseAnomalyStores; }
    public List<FinanceMetricCardVO> getTrendCards() { return trendCards; }
    public void setTrendCards(List<FinanceMetricCardVO> trendCards) { this.trendCards = trendCards; }
    public int getUnverifiedExpenseCount() { return unverifiedExpenseCount; }
    public void setUnverifiedExpenseCount(int unverifiedExpenseCount) { this.unverifiedExpenseCount = unverifiedExpenseCount; }
    public BigDecimal getUnverifiedExpenseAmount() { return unverifiedExpenseAmount; }
    public void setUnverifiedExpenseAmount(BigDecimal unverifiedExpenseAmount) { this.unverifiedExpenseAmount = unverifiedExpenseAmount; }
    public int getUnverifiedAdvanceCount() { return unverifiedAdvanceCount; }
    public void setUnverifiedAdvanceCount(int unverifiedAdvanceCount) { this.unverifiedAdvanceCount = unverifiedAdvanceCount; }
    public BigDecimal getUnverifiedAdvanceAmount() { return unverifiedAdvanceAmount; }
    public void setUnverifiedAdvanceAmount(BigDecimal unverifiedAdvanceAmount) { this.unverifiedAdvanceAmount = unverifiedAdvanceAmount; }
    public int getUnsettledProfitShareCount() { return unsettledProfitShareCount; }
    public void setUnsettledProfitShareCount(int unsettledProfitShareCount) { this.unsettledProfitShareCount = unsettledProfitShareCount; }
    public String getCurrentPeriodStatus() { return currentPeriodStatus; }
    public void setCurrentPeriodStatus(String currentPeriodStatus) { this.currentPeriodStatus = currentPeriodStatus; }
    public List<FinanceWarningVO> getWarnings() { return warnings; }
    public void setWarnings(List<FinanceWarningVO> warnings) { this.warnings = warnings; }
}
