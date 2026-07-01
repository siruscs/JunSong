package com.junsong.finance.service.diagnosis;

import java.math.BigDecimal;
import java.util.Date;

/**
 * A single diagnosis finding produced by a {@link FinanceDiagnosisRule}.
 *
 * <p>Fields map directly to {@code FinanceAlertVO} and {@code FinanceReviewTaskVO}
 * so conversion is trivial and field names are preserved for the frontend contract.</p>
 *
 * @author junsong
 * @since NIGHT-P1-C
 */
public class FinanceDiagnosisResult {

    private String ruleId;          // maps to alertType / taskType
    private String ruleName;        // human-readable Chinese name for the rule
    private String alertLevel;      // HIGH / MEDIUM / LOW
    private Long deptId;
    private String deptName;
    private String title;
    private String reason;
    private String metricName;      // what metric triggered the rule
    private BigDecimal metricValue;
    private BigDecimal threshold;   // the threshold value that was breached
    private BigDecimal compareValue;
    private BigDecimal impactAmount;
    private String suggestedAction;
    private String targetRoute;
    private String targetParams;
    private Date occurTime;

    // Priority ordinal for sorting (lower = higher priority)
    private int priorityOrdinal;

    public FinanceDiagnosisResult() {}

    /**
     * Full constructor used by all rule implementations.
     *
     * @param ruleId         unique rule identifier (e.g. "SALES_DROP")
     * @param ruleName       human-readable rule name (e.g. "销售下滑预警规则")
     * @param alertLevel     severity: HIGH / MEDIUM / LOW
     * @param title          alert title shown to user
     * @param reason         detailed reason text
     * @param metricName     the metric that triggered the rule (e.g. "销售环比变化率")
     * @param metricValue    actual value of the metric
     * @param threshold      threshold that was breached
     * @param compareValue   comparison value (e.g. previous month)
     * @param impactAmount   estimated financial impact
     * @param suggestedAction recommended action text
     * @param targetRoute    frontend route to navigate to
     * @param targetParams   JSON params for the target route
     */
    public FinanceDiagnosisResult(String ruleId, String ruleName, String alertLevel,
                                   String title, String reason,
                                   String metricName, BigDecimal metricValue,
                                   BigDecimal threshold, BigDecimal compareValue,
                                   BigDecimal impactAmount,
                                   String suggestedAction, String targetRoute, String targetParams) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.alertLevel = alertLevel;
        this.title = title;
        this.reason = reason;
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.threshold = threshold;
        this.compareValue = compareValue;
        this.impactAmount = impactAmount;
        this.suggestedAction = suggestedAction;
        this.targetRoute = targetRoute;
        this.targetParams = targetParams;
        this.occurTime = new Date();
        this.priorityOrdinal = alertLevelToOrdinal(alertLevel);
    }

    private static int alertLevelToOrdinal(String alertLevel) {
        if (alertLevel == null) return 99;
        switch (alertLevel) {
            case "HIGH":   return 0;
            case "MEDIUM": return 1;
            case "LOW":    return 2;
            default:       return 99;
        }
    }

    // ─── Getters and setters ────────────────────────────────────────────────

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
        this.priorityOrdinal = alertLevelToOrdinal(alertLevel);
    }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }

    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }

    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }

    public BigDecimal getCompareValue() { return compareValue; }
    public void setCompareValue(BigDecimal compareValue) { this.compareValue = compareValue; }

    public BigDecimal getImpactAmount() { return impactAmount; }
    public void setImpactAmount(BigDecimal impactAmount) { this.impactAmount = impactAmount; }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }

    public String getTargetRoute() { return targetRoute; }
    public void setTargetRoute(String targetRoute) { this.targetRoute = targetRoute; }

    public String getTargetParams() { return targetParams; }
    public void setTargetParams(String targetParams) { this.targetParams = targetParams; }

    public Date getOccurTime() { return occurTime; }
    public void setOccurTime(Date occurTime) { this.occurTime = occurTime; }

    public int getPriorityOrdinal() { return priorityOrdinal; }

    @Override
    public String toString() {
        return "FinanceDiagnosisResult{" +
                "ruleId='" + ruleId + '\'' +
                ", ruleName='" + ruleName + '\'' +
                ", alertLevel='" + alertLevel + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
