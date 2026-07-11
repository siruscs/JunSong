package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 周经营纪要 VO（R10-F）
 */
public class WeeklyMemoVO {

    private String weekStart;
    private String weekEnd;
    private String headline;
    private List<String> keyChanges = new ArrayList<>();
    private List<String> completedActions = new ArrayList<>();
    private List<String> unresolvedRisks = new ArrayList<>();
    private List<String> nextWeekFocus = new ArrayList<>();
    private BigDecimal reviewQualityScore = BigDecimal.ZERO;
    private List<String> storeHealthHighlights = new ArrayList<>();
    private List<String> reusableKnowledgeHints = new ArrayList<>();
    private Integer riskStoreCount = 0;
    private Integer watchStoreCount = 0;
    private Integer goodStoreCount = 0;

    public String getWeekStart() { return weekStart; }
    public void setWeekStart(String weekStart) { this.weekStart = weekStart; }
    public String getWeekEnd() { return weekEnd; }
    public void setWeekEnd(String weekEnd) { this.weekEnd = weekEnd; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public List<String> getKeyChanges() { return keyChanges; }
    public void setKeyChanges(List<String> keyChanges) { this.keyChanges = keyChanges; }
    public List<String> getCompletedActions() { return completedActions; }
    public void setCompletedActions(List<String> completedActions) { this.completedActions = completedActions; }
    public List<String> getUnresolvedRisks() { return unresolvedRisks; }
    public void setUnresolvedRisks(List<String> unresolvedRisks) { this.unresolvedRisks = unresolvedRisks; }
    public List<String> getNextWeekFocus() { return nextWeekFocus; }
    public void setNextWeekFocus(List<String> nextWeekFocus) { this.nextWeekFocus = nextWeekFocus; }
    public BigDecimal getReviewQualityScore() { return reviewQualityScore; }
    public void setReviewQualityScore(BigDecimal reviewQualityScore) { this.reviewQualityScore = reviewQualityScore; }
    public List<String> getStoreHealthHighlights() { return storeHealthHighlights; }
    public void setStoreHealthHighlights(List<String> storeHealthHighlights) { this.storeHealthHighlights = storeHealthHighlights; }
    public List<String> getReusableKnowledgeHints() { return reusableKnowledgeHints; }
    public void setReusableKnowledgeHints(List<String> reusableKnowledgeHints) { this.reusableKnowledgeHints = reusableKnowledgeHints; }
    public Integer getRiskStoreCount() { return riskStoreCount; }
    public void setRiskStoreCount(Integer riskStoreCount) { this.riskStoreCount = riskStoreCount; }
    public Integer getWatchStoreCount() { return watchStoreCount; }
    public void setWatchStoreCount(Integer watchStoreCount) { this.watchStoreCount = watchStoreCount; }
    public Integer getGoodStoreCount() { return goodStoreCount; }
    public void setGoodStoreCount(Integer goodStoreCount) { this.goodStoreCount = goodStoreCount; }
}
