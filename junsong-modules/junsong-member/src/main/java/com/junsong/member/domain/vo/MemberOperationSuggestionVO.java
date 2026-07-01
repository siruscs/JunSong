package com.junsong.member.domain.vo;

import java.math.BigDecimal;

/**
 * 会员经营建议 VO，由确定性规则生成，用于概览建议区和复盘任务候选。
 */
public class MemberOperationSuggestionVO {
    private String ruleCode;
    private String severity;
    private Long deptId;
    private String title;
    private String reason;
    private String suggestion;
    private String targetRoute;
    private BigDecimal impactAmount;
    private String dedupKey;

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getTargetRoute() {
        return targetRoute;
    }

    public void setTargetRoute(String targetRoute) {
        this.targetRoute = targetRoute;
    }

    public BigDecimal getImpactAmount() {
        return impactAmount;
    }

    public void setImpactAmount(BigDecimal impactAmount) {
        this.impactAmount = impactAmount;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey) {
        this.dedupKey = dedupKey;
    }
}
