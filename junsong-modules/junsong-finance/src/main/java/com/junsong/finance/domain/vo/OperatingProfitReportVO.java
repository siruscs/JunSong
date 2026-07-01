package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OperatingProfitReportVO {
    private BigDecimal totalIncome = BigDecimal.ZERO;
    private BigDecimal productCost = BigDecimal.ZERO;
    private BigDecimal operatingExpense = BigDecimal.ZERO;
    private BigDecimal grossProfit = BigDecimal.ZERO;
    private BigDecimal netProfit = BigDecimal.ZERO;
    private BigDecimal profitRate = BigDecimal.ZERO;
    private BigDecimal recoveryRate = BigDecimal.ZERO;
    private boolean costReliable = true;
    private String costNote;
    private List<Map<String, Object>> storeProfitRank;
    private List<Map<String, Object>> storeProfitRateRank;
    private List<Map<String, Object>> trendStats;
    private List<ProfitDrilldownRowVO> drilldownRows;

    public BigDecimal getTotalIncome() { return totalIncome; }
    public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }
    public BigDecimal getProductCost() { return productCost; }
    public void setProductCost(BigDecimal productCost) { this.productCost = productCost; }
    public BigDecimal getOperatingExpense() { return operatingExpense; }
    public void setOperatingExpense(BigDecimal operatingExpense) { this.operatingExpense = operatingExpense; }
    public BigDecimal getGrossProfit() { return grossProfit; }
    public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }
    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
    public BigDecimal getProfitRate() { return profitRate; }
    public void setProfitRate(BigDecimal profitRate) { this.profitRate = profitRate; }
    public BigDecimal getRecoveryRate() { return recoveryRate; }
    public void setRecoveryRate(BigDecimal recoveryRate) { this.recoveryRate = recoveryRate; }
    public boolean isCostReliable() { return costReliable; }
    public void setCostReliable(boolean costReliable) { this.costReliable = costReliable; }
    public String getCostNote() { return costNote; }
    public void setCostNote(String costNote) { this.costNote = costNote; }
    public List<Map<String, Object>> getStoreProfitRank() { return storeProfitRank; }
    public void setStoreProfitRank(List<Map<String, Object>> storeProfitRank) { this.storeProfitRank = storeProfitRank; }
    public List<Map<String, Object>> getStoreProfitRateRank() { return storeProfitRateRank; }
    public void setStoreProfitRateRank(List<Map<String, Object>> storeProfitRateRank) { this.storeProfitRateRank = storeProfitRateRank; }
    public List<Map<String, Object>> getTrendStats() { return trendStats; }
    public void setTrendStats(List<Map<String, Object>> trendStats) { this.trendStats = trendStats; }
    public List<ProfitDrilldownRowVO> getDrilldownRows() { return drilldownRows; }
    public void setDrilldownRows(List<ProfitDrilldownRowVO> drilldownRows) { this.drilldownRows = drilldownRows; }
}
