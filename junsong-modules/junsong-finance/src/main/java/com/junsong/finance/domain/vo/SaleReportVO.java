package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 销售报表 VO
 */
public class SaleReportVO {
    /** 总销售额 */
    private BigDecimal totalSales = BigDecimal.ZERO;
    /** 销售笔数 */
    private Integer totalCount = 0;
    /** 客单价 */
    private BigDecimal avgPrice = BigDecimal.ZERO;
    /** 门店统计 */
    private List<Map<String, Object>> deptStats;
    /** 销售排行 */
    private List<Map<String, Object>> rankStats;
    /** 趋势统计 */
    private List<Map<String, Object>> trendStats;

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public BigDecimal getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(BigDecimal avgPrice) {
        this.avgPrice = avgPrice;
    }

    public List<Map<String, Object>> getDeptStats() {
        return deptStats;
    }

    public void setDeptStats(List<Map<String, Object>> deptStats) {
        this.deptStats = deptStats;
    }

    public List<Map<String, Object>> getRankStats() {
        return rankStats;
    }

    public void setRankStats(List<Map<String, Object>> rankStats) {
        this.rankStats = rankStats;
    }

    public List<Map<String, Object>> getTrendStats() {
        return trendStats;
    }

    public void setTrendStats(List<Map<String, Object>> trendStats) {
        this.trendStats = trendStats;
    }
}