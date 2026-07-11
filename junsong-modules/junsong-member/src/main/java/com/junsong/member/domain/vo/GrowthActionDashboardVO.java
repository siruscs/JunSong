package com.junsong.member.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 增长动作看板VO
 *
 * @author junsong
 */
public class GrowthActionDashboardVO
{
    private Integer pendingActionCount;
    private Integer pendingMemberCount;
    private Integer executedMemberCount;
    private Integer effectiveMemberCount;
    private BigDecimal effectRate;
    private String topSegmentType;
    private String pressureLevel;
    private Boolean pressureFallbackUsed;
    private List<GrowthActionCandidateVO> candidates;
    private List<GrowthActionRowVO> recentActions;
    private GrowthActionEffectVO effectSummary;

    public Integer getPendingActionCount() { return pendingActionCount; }
    public void setPendingActionCount(Integer pendingActionCount) { this.pendingActionCount = pendingActionCount; }

    public Integer getPendingMemberCount() { return pendingMemberCount; }
    public void setPendingMemberCount(Integer pendingMemberCount) { this.pendingMemberCount = pendingMemberCount; }

    public Integer getExecutedMemberCount() { return executedMemberCount; }
    public void setExecutedMemberCount(Integer executedMemberCount) { this.executedMemberCount = executedMemberCount; }

    public Integer getEffectiveMemberCount() { return effectiveMemberCount; }
    public void setEffectiveMemberCount(Integer effectiveMemberCount) { this.effectiveMemberCount = effectiveMemberCount; }

    public BigDecimal getEffectRate() { return effectRate; }
    public void setEffectRate(BigDecimal effectRate) { this.effectRate = effectRate; }

    public String getTopSegmentType() { return topSegmentType; }
    public void setTopSegmentType(String topSegmentType) { this.topSegmentType = topSegmentType; }

    public String getPressureLevel() { return pressureLevel; }
    public void setPressureLevel(String pressureLevel) { this.pressureLevel = pressureLevel; }

    public Boolean getPressureFallbackUsed() { return pressureFallbackUsed; }
    public void setPressureFallbackUsed(Boolean pressureFallbackUsed) { this.pressureFallbackUsed = pressureFallbackUsed; }

    public List<GrowthActionCandidateVO> getCandidates() { return candidates; }
    public void setCandidates(List<GrowthActionCandidateVO> candidates) { this.candidates = candidates; }

    public List<GrowthActionRowVO> getRecentActions() { return recentActions; }
    public void setRecentActions(List<GrowthActionRowVO> recentActions) { this.recentActions = recentActions; }

    public GrowthActionEffectVO getEffectSummary() { return effectSummary; }
    public void setEffectSummary(GrowthActionEffectVO effectSummary) { this.effectSummary = effectSummary; }
}
