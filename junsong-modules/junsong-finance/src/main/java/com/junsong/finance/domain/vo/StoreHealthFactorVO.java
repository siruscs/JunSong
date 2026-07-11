package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

public class StoreHealthFactorVO {
    private String factorCode;
    private String factorName;
    private String severity;
    private Integer deductedScore;
    private BigDecimal metricValue;
    private BigDecimal thresholdValue;
    private String reason;
    private String suggestion;
    private String targetRoute;

    public String getFactorCode() { return factorCode; }
    public void setFactorCode(String factorCode) { this.factorCode = factorCode; }
    public String getFactorName() { return factorName; }
    public void setFactorName(String factorName) { this.factorName = factorName; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Integer getDeductedScore() { return deductedScore; }
    public void setDeductedScore(Integer deductedScore) { this.deductedScore = deductedScore; }
    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public String getTargetRoute() { return targetRoute; }
    public void setTargetRoute(String targetRoute) { this.targetRoute = targetRoute; }
}
