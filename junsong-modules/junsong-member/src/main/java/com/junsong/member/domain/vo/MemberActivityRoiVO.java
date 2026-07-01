package com.junsong.member.domain.vo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 会员活动 ROI 视图对象
 * 用于回答"这个活动是否盈利"的问题
 */
public class MemberActivityRoiVO {
    private Long activityId;
    private String activityName;
    private String activityType;    // "SECKILL"
    private Date startTime;
    private Date endTime;
    private String status;          // "0" active, "1" ended

    private BigDecimal totalSalesAmount;     // sum from mem_seckill_record.total_amount
    private int totalOrders;                 // count of valid records
    private int newCustomerCount;            // members with join_date during activity period
    private BigDecimal sellThroughRate;      // (total_shares - remain_shares) / total_shares * 100

    private BigDecimal discountCost;         // estimated discount cost (may be UNAVAILABLE)
    private String discountCostStatus;       // "AVAILABLE" or "UNAVAILABLE"

    private BigDecimal activityCostAmount;   // R5-D: 活动成本（来自活动录入）
    private BigDecimal relatedSalesAmount;   // R5-D: 关联销售金额
    private BigDecimal grossProfitAmount;    // R5-D: 毛利 = 关联销售 - 活动成本
    private Long participantCount;           // R5-D: 参与人数（去重）
    private Long firstPurchaseMemberCount;   // R5-D: 首购会员数
    private Long repurchaseMemberCount;      // R5-D: 复购会员数

    private BigDecimal roi;                  // (sales - cost) / cost * 100 (may be UNAVAILABLE)
    private String roiStatus;               // "READY" or "UNAVAILABLE"
    private String unavailableReason;        // R5-D: MISSING_ACTIVITY_COST / NO_RELATED_SALES / MISSING_ACTIVITY_SALE_LINK

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public void setTotalSalesAmount(BigDecimal totalSalesAmount) {
        this.totalSalesAmount = totalSalesAmount;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public int getNewCustomerCount() {
        return newCustomerCount;
    }

    public void setNewCustomerCount(int newCustomerCount) {
        this.newCustomerCount = newCustomerCount;
    }

    public BigDecimal getSellThroughRate() {
        return sellThroughRate;
    }

    public void setSellThroughRate(BigDecimal sellThroughRate) {
        this.sellThroughRate = sellThroughRate;
    }

    public BigDecimal getDiscountCost() {
        return discountCost;
    }

    public void setDiscountCost(BigDecimal discountCost) {
        this.discountCost = discountCost;
    }

    public String getDiscountCostStatus() {
        return discountCostStatus;
    }

    public void setDiscountCostStatus(String discountCostStatus) {
        this.discountCostStatus = discountCostStatus;
    }

    public BigDecimal getRoi() {
        return roi;
    }

    public void setRoi(BigDecimal roi) {
        this.roi = roi;
    }

    public String getRoiStatus() {
        return roiStatus;
    }

    public void setRoiStatus(String roiStatus) {
        this.roiStatus = roiStatus;
    }

    public BigDecimal getActivityCostAmount() {
        return activityCostAmount;
    }

    public void setActivityCostAmount(BigDecimal activityCostAmount) {
        this.activityCostAmount = activityCostAmount;
    }

    public BigDecimal getRelatedSalesAmount() {
        return relatedSalesAmount;
    }

    public void setRelatedSalesAmount(BigDecimal relatedSalesAmount) {
        this.relatedSalesAmount = relatedSalesAmount;
    }

    public BigDecimal getGrossProfitAmount() {
        return grossProfitAmount;
    }

    public void setGrossProfitAmount(BigDecimal grossProfitAmount) {
        this.grossProfitAmount = grossProfitAmount;
    }

    public Long getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(Long participantCount) {
        this.participantCount = participantCount;
    }

    public Long getFirstPurchaseMemberCount() {
        return firstPurchaseMemberCount;
    }

    public void setFirstPurchaseMemberCount(Long firstPurchaseMemberCount) {
        this.firstPurchaseMemberCount = firstPurchaseMemberCount;
    }

    public Long getRepurchaseMemberCount() {
        return repurchaseMemberCount;
    }

    public void setRepurchaseMemberCount(Long repurchaseMemberCount) {
        this.repurchaseMemberCount = repurchaseMemberCount;
    }

    public String getUnavailableReason() {
        return unavailableReason;
    }

    public void setUnavailableReason(String unavailableReason) {
        this.unavailableReason = unavailableReason;
    }
}
