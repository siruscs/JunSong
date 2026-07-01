package com.junsong.member.domain.vo;

import java.math.BigDecimal;
import java.util.List;

public class MemberContributionReportVO {
    private int newMemberCount;
    private int activeMemberCount;
    private int repurchaseCount;
    private BigDecimal memberSales = BigDecimal.ZERO;
    private BigDecimal nonMemberSales = BigDecimal.ZERO;
    private BigDecimal memberSalesRatio = BigDecimal.ZERO;
    private BigDecimal pointsRedemptionCost = BigDecimal.ZERO;
    private BigDecimal seckillSales = BigDecimal.ZERO;
    private BigDecimal seckillCost = BigDecimal.ZERO;
    private BigDecimal seckillProfit = BigDecimal.ZERO;
    private int memberSaleCount;
    private BigDecimal avgMemberSaleAmount = BigDecimal.ZERO;
    private BigDecimal newMemberFirstPurchaseRate = BigDecimal.ZERO;
    private int repeatPurchaseCount;
    private BigDecimal repeatPurchaseRate = BigDecimal.ZERO;
    private String dataNote;
    private List<MemberContributionTrendVO> trends;
    private List<MemberActivityContributionVO> activityContributions;

    public int getNewMemberCount() { return newMemberCount; }
    public void setNewMemberCount(int newMemberCount) { this.newMemberCount = newMemberCount; }
    public int getActiveMemberCount() { return activeMemberCount; }
    public void setActiveMemberCount(int activeMemberCount) { this.activeMemberCount = activeMemberCount; }
    public int getRepurchaseCount() { return repurchaseCount; }
    public void setRepurchaseCount(int repurchaseCount) { this.repurchaseCount = repurchaseCount; }
    public BigDecimal getMemberSales() { return memberSales; }
    public void setMemberSales(BigDecimal memberSales) { this.memberSales = memberSales; }
    public BigDecimal getNonMemberSales() { return nonMemberSales; }
    public void setNonMemberSales(BigDecimal nonMemberSales) { this.nonMemberSales = nonMemberSales; }
    public BigDecimal getMemberSalesRatio() { return memberSalesRatio; }
    public void setMemberSalesRatio(BigDecimal memberSalesRatio) { this.memberSalesRatio = memberSalesRatio; }
    public BigDecimal getPointsRedemptionCost() { return pointsRedemptionCost; }
    public void setPointsRedemptionCost(BigDecimal pointsRedemptionCost) { this.pointsRedemptionCost = pointsRedemptionCost; }
    public BigDecimal getSeckillSales() { return seckillSales; }
    public void setSeckillSales(BigDecimal seckillSales) { this.seckillSales = seckillSales; }
    public BigDecimal getSeckillCost() { return seckillCost; }
    public void setSeckillCost(BigDecimal seckillCost) { this.seckillCost = seckillCost; }
    public BigDecimal getSeckillProfit() { return seckillProfit; }
    public void setSeckillProfit(BigDecimal seckillProfit) { this.seckillProfit = seckillProfit; }
    public int getMemberSaleCount() { return memberSaleCount; }
    public void setMemberSaleCount(int memberSaleCount) { this.memberSaleCount = memberSaleCount; }
    public BigDecimal getAvgMemberSaleAmount() { return avgMemberSaleAmount; }
    public void setAvgMemberSaleAmount(BigDecimal avgMemberSaleAmount) { this.avgMemberSaleAmount = avgMemberSaleAmount; }
    public BigDecimal getNewMemberFirstPurchaseRate() { return newMemberFirstPurchaseRate; }
    public void setNewMemberFirstPurchaseRate(BigDecimal newMemberFirstPurchaseRate) { this.newMemberFirstPurchaseRate = newMemberFirstPurchaseRate; }
    public int getRepeatPurchaseCount() { return repeatPurchaseCount; }
    public void setRepeatPurchaseCount(int repeatPurchaseCount) { this.repeatPurchaseCount = repeatPurchaseCount; }
    public BigDecimal getRepeatPurchaseRate() { return repeatPurchaseRate; }
    public void setRepeatPurchaseRate(BigDecimal repeatPurchaseRate) { this.repeatPurchaseRate = repeatPurchaseRate; }
    public String getDataNote() { return dataNote; }
    public void setDataNote(String dataNote) { this.dataNote = dataNote; }
    public List<MemberContributionTrendVO> getTrends() { return trends; }
    public void setTrends(List<MemberContributionTrendVO> trends) { this.trends = trends; }
    public List<MemberActivityContributionVO> getActivityContributions() { return activityContributions; }
    public void setActivityContributions(List<MemberActivityContributionVO> activityContributions) { this.activityContributions = activityContributions; }
}
