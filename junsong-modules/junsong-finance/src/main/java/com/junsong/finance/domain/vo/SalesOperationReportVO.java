package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class SalesOperationReportVO {
    private BigDecimal totalSales = BigDecimal.ZERO;
    private int orderCount;
    private int totalQuantity;
    private BigDecimal avgOrderAmount = BigDecimal.ZERO;
    private BigDecimal avgItemAmount = BigDecimal.ZERO;
    private BigDecimal memberSales = BigDecimal.ZERO;
    private BigDecimal nonMemberSales = BigDecimal.ZERO;
    private BigDecimal seckillSales = BigDecimal.ZERO;
    private BigDecimal normalSales = BigDecimal.ZERO;
    private List<SalesRankRowVO> storeRank;
    private List<SalesRankRowVO> productRank;
    private List<Map<String, Object>> trendStats;
    private List<FinanceWarningVO> warnings;

    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal totalSales) { this.totalSales = totalSales; }
    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }
    public BigDecimal getAvgOrderAmount() { return avgOrderAmount; }
    public void setAvgOrderAmount(BigDecimal avgOrderAmount) { this.avgOrderAmount = avgOrderAmount; }
    public BigDecimal getAvgItemAmount() { return avgItemAmount; }
    public void setAvgItemAmount(BigDecimal avgItemAmount) { this.avgItemAmount = avgItemAmount; }
    public BigDecimal getMemberSales() { return memberSales; }
    public void setMemberSales(BigDecimal memberSales) { this.memberSales = memberSales; }
    public BigDecimal getNonMemberSales() { return nonMemberSales; }
    public void setNonMemberSales(BigDecimal nonMemberSales) { this.nonMemberSales = nonMemberSales; }
    public BigDecimal getSeckillSales() { return seckillSales; }
    public void setSeckillSales(BigDecimal seckillSales) { this.seckillSales = seckillSales; }
    public BigDecimal getNormalSales() { return normalSales; }
    public void setNormalSales(BigDecimal normalSales) { this.normalSales = normalSales; }
    public List<SalesRankRowVO> getStoreRank() { return storeRank; }
    public void setStoreRank(List<SalesRankRowVO> storeRank) { this.storeRank = storeRank; }
    public List<SalesRankRowVO> getProductRank() { return productRank; }
    public void setProductRank(List<SalesRankRowVO> productRank) { this.productRank = productRank; }
    public List<Map<String, Object>> getTrendStats() { return trendStats; }
    public void setTrendStats(List<Map<String, Object>> trendStats) { this.trendStats = trendStats; }
    public List<FinanceWarningVO> getWarnings() { return warnings; }
    public void setWarnings(List<FinanceWarningVO> warnings) { this.warnings = warnings; }
}
