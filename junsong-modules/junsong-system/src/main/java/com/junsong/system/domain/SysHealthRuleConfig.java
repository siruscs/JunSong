package com.junsong.system.domain;

import java.math.BigDecimal;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 自检规则配置 sys_health_rule_config
 */
public class SysHealthRuleConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long ruleId;
    private String ruleCode;
    private String ruleName;
    private String ruleDomain;
    private String metricKey;
    private String compareOp;
    private BigDecimal thresholdValue;
    private String severity;
    private String enabled;
    private String notifyEnabled;
    private String deptScope;
    private String suggestion;
    private Integer sortOrder;

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getRuleDomain() { return ruleDomain; }
    public void setRuleDomain(String ruleDomain) { this.ruleDomain = ruleDomain; }
    public String getMetricKey() { return metricKey; }
    public void setMetricKey(String metricKey) { this.metricKey = metricKey; }
    public String getCompareOp() { return compareOp; }
    public void setCompareOp(String compareOp) { this.compareOp = compareOp; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getNotifyEnabled() { return notifyEnabled; }
    public void setNotifyEnabled(String notifyEnabled) { this.notifyEnabled = notifyEnabled; }
    public String getDeptScope() { return deptScope; }
    public void setDeptScope(String deptScope) { this.deptScope = deptScope; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
