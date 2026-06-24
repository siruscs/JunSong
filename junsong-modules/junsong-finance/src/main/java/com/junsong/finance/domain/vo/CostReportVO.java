package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 成本分析报表 VO
 */
public class CostReportVO {
    /** 总成本 */
    private BigDecimal totalCost = BigDecimal.ZERO;
    /** 分类统计 */
    private List<Map<String, Object>> categoryStats;
    /** 趋势统计 */
    private List<Map<String, Object>> trendStats;
    /** 门店统计 */
    private List<Map<String, Object>> deptStats;

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
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
}