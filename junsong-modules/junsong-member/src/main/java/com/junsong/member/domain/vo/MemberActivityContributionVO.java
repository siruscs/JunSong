package com.junsong.member.domain.vo;

import java.math.BigDecimal;

public class MemberActivityContributionVO {
    private Long activityId;
    private String activityName;
    private BigDecimal sales = BigDecimal.ZERO;
    private BigDecimal cost = BigDecimal.ZERO;
    private BigDecimal profit = BigDecimal.ZERO;
    private int participantCount;

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public BigDecimal getSales() { return sales; }
    public void setSales(BigDecimal sales) { this.sales = sales; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public BigDecimal getProfit() { return profit; }
    public void setProfit(BigDecimal profit) { this.profit = profit; }
    public int getParticipantCount() { return participantCount; }
    public void setParticipantCount(int participantCount) { this.participantCount = participantCount; }
}
