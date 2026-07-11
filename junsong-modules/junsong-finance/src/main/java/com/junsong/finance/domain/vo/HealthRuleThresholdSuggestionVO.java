package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

/**
 * R12-C: 健康规则阈值建议VO
 */
public class HealthRuleThresholdSuggestionVO {
    private String ruleCode;
    private String ruleName;
    private String currentThreshold;
    private String suggestedThreshold;
    private String suggestionType; // KEEP, TIGHTEN, RELAX, INSUFFICIENT_DATA
    private String reason;
    private Integer sampleDays;
    private Integer affectedStoreCount;
    private BigDecimal p50;
    private BigDecimal p75;
    private BigDecimal p90;

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getCurrentThreshold() {
        return currentThreshold;
    }

    public void setCurrentThreshold(String currentThreshold) {
        this.currentThreshold = currentThreshold;
    }

    public String getSuggestedThreshold() {
        return suggestedThreshold;
    }

    public void setSuggestedThreshold(String suggestedThreshold) {
        this.suggestedThreshold = suggestedThreshold;
    }

    public String getSuggestionType() {
        return suggestionType;
    }

    public void setSuggestionType(String suggestionType) {
        this.suggestionType = suggestionType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getSampleDays() {
        return sampleDays;
    }

    public void setSampleDays(Integer sampleDays) {
        this.sampleDays = sampleDays;
    }

    public Integer getAffectedStoreCount() {
        return affectedStoreCount;
    }

    public void setAffectedStoreCount(Integer affectedStoreCount) {
        this.affectedStoreCount = affectedStoreCount;
    }

    public BigDecimal getP50() {
        return p50;
    }

    public void setP50(BigDecimal p50) {
        this.p50 = p50;
    }

    public BigDecimal getP75() {
        return p75;
    }

    public void setP75(BigDecimal p75) {
        this.p75 = p75;
    }

    public BigDecimal getP90() {
        return p90;
    }

    public void setP90(BigDecimal p90) {
        this.p90 = p90;
    }
}
