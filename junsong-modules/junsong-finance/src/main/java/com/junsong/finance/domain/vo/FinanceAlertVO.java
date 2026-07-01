package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 经营预警 VO - 统一经营预警中心
 */
public class FinanceAlertVO {
    private String alertId;
    private String alertLevel;   // HIGH / MEDIUM / LOW
    private String alertType;    // SALES_DROP / EXPENSE_SPIKE / PROFIT_RATE_DROP / PENDING_VERIFY / PROFIT_SHARE_EXCEPTION / MEMBER_CONTRIBUTION_DROP
    private Long deptId;
    private String deptName;
    private String title;
    private String reason;
    private BigDecimal metricValue;
    private BigDecimal compareValue;
    private BigDecimal impactAmount;
    private String suggestedAction;
    private String targetRoute;
    private String targetParams;
    private Date occurTime;

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }
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
}
