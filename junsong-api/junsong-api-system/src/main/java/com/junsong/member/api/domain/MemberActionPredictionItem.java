package com.junsong.member.api.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 会员动作预测信号（内部服务返回，R24 预测辅助 V2 展示用）。
 */
public class MemberActionPredictionItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long actionId;
    private String actionTitle;
    private Long deptId;
    private String deptName;
    private String segmentCode;
    private String level;
    private Integer score = 0;
    private BigDecimal recentActiveRate = BigDecimal.ZERO;
    private BigDecimal historicalEffectRate = BigDecimal.ZERO;
    private String basis;
    private String recommendation;

    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }
    public String getActionTitle() { return actionTitle; }
    public void setActionTitle(String actionTitle) { this.actionTitle = actionTitle; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getSegmentCode() { return segmentCode; }
    public void setSegmentCode(String segmentCode) { this.segmentCode = segmentCode; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score == null ? 0 : score; }
    public BigDecimal getRecentActiveRate() { return recentActiveRate; }
    public void setRecentActiveRate(BigDecimal recentActiveRate) { this.recentActiveRate = recentActiveRate == null ? BigDecimal.ZERO : recentActiveRate; }
    public BigDecimal getHistoricalEffectRate() { return historicalEffectRate; }
    public void setHistoricalEffectRate(BigDecimal historicalEffectRate) { this.historicalEffectRate = historicalEffectRate == null ? BigDecimal.ZERO : historicalEffectRate; }
    public String getBasis() { return basis; }
    public void setBasis(String basis) { this.basis = basis; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
}
