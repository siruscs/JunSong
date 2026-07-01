package com.junsong.member.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 积分经营摘要：用于回答"积分沉淀了多少兑换成本压力"。
 * 口径：100 积分 = 1 元，仅用于经营估算。
 */
public class MemberPointsOperationSummaryVO {
    private Long totalAvailablePoints;
    private BigDecimal estimatedPointsLiabilityAmount;
    private BigDecimal redeemedCostAmount;
    private Long highPointsMemberCount;
    private List<MemberPointsRiskRowVO> highPointsMembers;
    private String pointsLiabilityFormula = "100积分=1元，仅用于经营估算";

    public Long getTotalAvailablePoints() {
        return totalAvailablePoints;
    }

    public void setTotalAvailablePoints(Long totalAvailablePoints) {
        this.totalAvailablePoints = totalAvailablePoints;
    }

    public BigDecimal getEstimatedPointsLiabilityAmount() {
        return estimatedPointsLiabilityAmount;
    }

    public void setEstimatedPointsLiabilityAmount(BigDecimal estimatedPointsLiabilityAmount) {
        this.estimatedPointsLiabilityAmount = estimatedPointsLiabilityAmount;
    }

    public BigDecimal getRedeemedCostAmount() {
        return redeemedCostAmount;
    }

    public void setRedeemedCostAmount(BigDecimal redeemedCostAmount) {
        this.redeemedCostAmount = redeemedCostAmount;
    }

    public Long getHighPointsMemberCount() {
        return highPointsMemberCount;
    }

    public void setHighPointsMemberCount(Long highPointsMemberCount) {
        this.highPointsMemberCount = highPointsMemberCount;
    }

    public List<MemberPointsRiskRowVO> getHighPointsMembers() {
        return highPointsMembers;
    }

    public void setHighPointsMembers(List<MemberPointsRiskRowVO> highPointsMembers) {
        this.highPointsMembers = highPointsMembers;
    }

    public String getPointsLiabilityFormula() {
        return pointsLiabilityFormula;
    }

    public void setPointsLiabilityFormula(String pointsLiabilityFormula) {
        this.pointsLiabilityFormula = pointsLiabilityFormula;
    }
}
