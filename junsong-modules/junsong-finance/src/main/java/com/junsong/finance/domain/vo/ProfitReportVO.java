package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 利润报表 VO
 */
public class ProfitReportVO {
    /** 总利润 */
    private BigDecimal totalProfit = BigDecimal.ZERO;
    /** 利润率 */
    private BigDecimal profitRate = BigDecimal.ZERO;
    /** 回本进度 */
    private BigDecimal recoveryRate = BigDecimal.ZERO;
    /** 门店统计 */
    private List<Map<String, Object>> deptStats;
    /** 回本情况 */
    private List<Map<String, Object>> recoveryStats;
    /** 趋势统计 */
    private List<Map<String, Object>> trendStats;

    public BigDecimal getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(BigDecimal totalProfit) {
        this.totalProfit = totalProfit;
    }

    public BigDecimal getProfitRate() {
        return profitRate;
    }

    public void setProfitRate(BigDecimal profitRate) {
        this.profitRate = profitRate;
    }

    public BigDecimal getRecoveryRate() {
        return recoveryRate;
    }

    public void setRecoveryRate(BigDecimal recoveryRate) {
        this.recoveryRate = recoveryRate;
    }

    public List<Map<String, Object>> getDeptStats() {
        return deptStats;
    }

    public void setDeptStats(List<Map<String, Object>> deptStats) {
        this.deptStats = deptStats;
    }

    public List<Map<String, Object>> getRecoveryStats() {
        return recoveryStats;
    }

    public void setRecoveryStats(List<Map<String, Object>> recoveryStats) {
        this.recoveryStats = recoveryStats;
    }

    public List<Map<String, Object>> getTrendStats() {
        return trendStats;
    }

    public void setTrendStats(List<Map<String, Object>> trendStats) {
        this.trendStats = trendStats;
    }
}