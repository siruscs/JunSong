package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ExpenseReportVO {
    private BigDecimal totalExpense;
    private List<Map<String, Object>> categoryStats;
    private List<Map<String, Object>> trendStats;
    private List<Map<String, Object>> deptStats;
    private List<Map<String, Object>> detailList;

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public List<Map<String, Object>> getCategoryStats() {
        return categoryStats;
    }

    public void setCategoryStats(List<Map<String, Object>> categoryStats) {
        this.categoryStats = categoryStats;
    }

    public List<Map<String, Object>> getTrendStats() {
        return trendStats;
    }

    public void setTrendStats(List<Map<String, Object>> trendStats) {
        this.trendStats = trendStats;
    }

    public List<Map<String, Object>> getDeptStats() {
        return deptStats;
    }

    public void setDeptStats(List<Map<String, Object>> deptStats) {
        this.deptStats = deptStats;
    }

    public List<Map<String, Object>> getDetailList() {
        return detailList;
    }

    public void setDetailList(List<Map<String, Object>> detailList) {
        this.detailList = detailList;
    }
}
