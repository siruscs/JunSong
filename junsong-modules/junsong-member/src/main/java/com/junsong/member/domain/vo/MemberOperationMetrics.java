package com.junsong.member.domain.vo;

import java.math.BigDecimal;

/**
 * 会员经营指标聚合输入，供 MemberOperationSuggestionService 生成经营建议。
 * 字段均来自真实表统计，不包含前端假值。
 */
public class MemberOperationMetrics {
    private Long deptId;
    private long totalMemberCount;
    private long newMemberCount;
    private long activeMemberCount;
    private long silentMemberCount;
    private long highValueMemberCount;
    private BigDecimal firstPurchaseRate;
    private BigDecimal repurchaseRate30d;
    private BigDecimal pointsLiabilityAmount;
    private boolean activityRoiAvailable;
    private String businessDate;

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public long getTotalMemberCount() {
        return totalMemberCount;
    }

    public void setTotalMemberCount(long totalMemberCount) {
        this.totalMemberCount = totalMemberCount;
    }

    public long getNewMemberCount() {
        return newMemberCount;
    }

    public void setNewMemberCount(long newMemberCount) {
        this.newMemberCount = newMemberCount;
    }

    public long getActiveMemberCount() {
        return activeMemberCount;
    }

    public void setActiveMemberCount(long activeMemberCount) {
        this.activeMemberCount = activeMemberCount;
    }

    public long getSilentMemberCount() {
        return silentMemberCount;
    }

    public void setSilentMemberCount(long silentMemberCount) {
        this.silentMemberCount = silentMemberCount;
    }

    public long getHighValueMemberCount() {
        return highValueMemberCount;
    }

    public void setHighValueMemberCount(long highValueMemberCount) {
        this.highValueMemberCount = highValueMemberCount;
    }

    public BigDecimal getFirstPurchaseRate() {
        return firstPurchaseRate;
    }

    public void setFirstPurchaseRate(BigDecimal firstPurchaseRate) {
        this.firstPurchaseRate = firstPurchaseRate;
    }

    public BigDecimal getRepurchaseRate30d() {
        return repurchaseRate30d;
    }

    public void setRepurchaseRate30d(BigDecimal repurchaseRate30d) {
        this.repurchaseRate30d = repurchaseRate30d;
    }

    public BigDecimal getPointsLiabilityAmount() {
        return pointsLiabilityAmount;
    }

    public void setPointsLiabilityAmount(BigDecimal pointsLiabilityAmount) {
        this.pointsLiabilityAmount = pointsLiabilityAmount;
    }

    public boolean isActivityRoiAvailable() {
        return activityRoiAvailable;
    }

    public void setActivityRoiAvailable(boolean activityRoiAvailable) {
        this.activityRoiAvailable = activityRoiAvailable;
    }

    public String getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(String businessDate) {
        this.businessDate = businessDate;
    }
}
