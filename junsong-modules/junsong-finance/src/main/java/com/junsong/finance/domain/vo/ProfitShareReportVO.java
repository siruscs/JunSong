package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ProfitShareReportVO {
    private BigDecimal totalProfitShare;
    private BigDecimal totalManagerProfit;
    private BigDecimal totalInvestorProfit;
    private List<Map<String, Object>> managerStats;
    private List<Map<String, Object>> investorStats;
    private List<Map<String, Object>> deptStats;
    private List<Map<String, Object>> trendStats;
    private List<Map<String, Object>> detailList;

    public BigDecimal getTotalProfitShare() {
        return totalProfitShare;
    }

    public void setTotalProfitShare(BigDecimal totalProfitShare) {
        this.totalProfitShare = totalProfitShare;
    }

    public BigDecimal getTotalManagerProfit() {
        return totalManagerProfit;
    }

    public void setTotalManagerProfit(BigDecimal totalManagerProfit) {
        this.totalManagerProfit = totalManagerProfit;
    }

    public BigDecimal getTotalInvestorProfit() {
        return totalInvestorProfit;
    }

    public void setTotalInvestorProfit(BigDecimal totalInvestorProfit) {
        this.totalInvestorProfit = totalInvestorProfit;
    }

    public List<Map<String, Object>> getManagerStats() {
        return managerStats;
    }

    public void setManagerStats(List<Map<String, Object>> managerStats) {
        this.managerStats = managerStats;
    }

    public List<Map<String, Object>> getInvestorStats() {
        return investorStats;
    }

    public void setInvestorStats(List<Map<String, Object>> investorStats) {
        this.investorStats = investorStats;
    }

    public List<Map<String, Object>> getDeptStats() {
        return deptStats;
    }

    public void setDeptStats(List<Map<String, Object>> deptStats) {
        this.deptStats = deptStats;
    }

    public List<Map<String, Object>> getTrendStats() {
        return trendStats;
    }

    public void setTrendStats(List<Map<String, Object>> trendStats) {
        this.trendStats = trendStats;
    }

    public List<Map<String, Object>> getDetailList() {
        return detailList;
    }

    public void setDetailList(List<Map<String, Object>> detailList) {
        this.detailList = detailList;
    }
}
