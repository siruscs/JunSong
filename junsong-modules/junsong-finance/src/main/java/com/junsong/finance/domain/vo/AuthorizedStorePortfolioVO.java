package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;

public class AuthorizedStorePortfolioVO {
    private List<Long> allowedDeptIds;
    private List<Long> selectedDeptIds;
    private Integer storeCount;
    private BigDecimal totalSales;
    private BigDecimal totalExpense;
    private BigDecimal operatingProfit;
    private BigDecimal operatingProfitRate;
    private BigDecimal avgSales;
    private BigDecimal avgExpense;
    private BigDecimal avgProfit;
    private BigDecimal avgProfitRate;
    private List<AuthorizedStoreRowVO> stores;
    private List<StoreReviewTaskVO> reviewTasks;
    private List<String> suggestions;

    public List<Long> getAllowedDeptIds() { return allowedDeptIds; }
    public void setAllowedDeptIds(List<Long> allowedDeptIds) { this.allowedDeptIds = allowedDeptIds; }

    public List<Long> getSelectedDeptIds() { return selectedDeptIds; }
    public void setSelectedDeptIds(List<Long> selectedDeptIds) { this.selectedDeptIds = selectedDeptIds; }

    public Integer getStoreCount() { return storeCount; }
    public void setStoreCount(Integer storeCount) { this.storeCount = storeCount; }

    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal totalSales) { this.totalSales = totalSales; }

    public BigDecimal getTotalExpense() { return totalExpense; }
    public void setTotalExpense(BigDecimal totalExpense) { this.totalExpense = totalExpense; }

    public BigDecimal getOperatingProfit() { return operatingProfit; }
    public void setOperatingProfit(BigDecimal operatingProfit) { this.operatingProfit = operatingProfit; }

    public BigDecimal getOperatingProfitRate() { return operatingProfitRate; }
    public void setOperatingProfitRate(BigDecimal operatingProfitRate) { this.operatingProfitRate = operatingProfitRate; }

    public List<AuthorizedStoreRowVO> getStores() { return stores; }
    public void setStores(List<AuthorizedStoreRowVO> stores) { this.stores = stores; }

    public List<StoreReviewTaskVO> getReviewTasks() { return reviewTasks; }
    public void setReviewTasks(List<StoreReviewTaskVO> reviewTasks) { this.reviewTasks = reviewTasks; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public BigDecimal getAvgSales() { return avgSales; }
    public void setAvgSales(BigDecimal avgSales) { this.avgSales = avgSales; }

    public BigDecimal getAvgExpense() { return avgExpense; }
    public void setAvgExpense(BigDecimal avgExpense) { this.avgExpense = avgExpense; }

    public BigDecimal getAvgProfit() { return avgProfit; }
    public void setAvgProfit(BigDecimal avgProfit) { this.avgProfit = avgProfit; }

    public BigDecimal getAvgProfitRate() { return avgProfitRate; }
    public void setAvgProfitRate(BigDecimal avgProfitRate) { this.avgProfitRate = avgProfitRate; }
}
