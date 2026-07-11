package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

public class CashflowForecastWindowVO {
    private Integer windowDays;
    private BigDecimal forecastReceivableAmount = BigDecimal.ZERO;
    private BigDecimal promisedAmount = BigDecimal.ZERO;
    private BigDecimal actualReceivableAmount = BigDecimal.ZERO;
    private BigDecimal deviationAmount = BigDecimal.ZERO;
    private BigDecimal deviationRate = BigDecimal.ZERO;
    private String windowLabel;

    public Integer getWindowDays() { return windowDays; }
    public void setWindowDays(Integer windowDays) { this.windowDays = windowDays; }
    public BigDecimal getForecastReceivableAmount() { return forecastReceivableAmount; }
    public void setForecastReceivableAmount(BigDecimal forecastReceivableAmount) { this.forecastReceivableAmount = forecastReceivableAmount == null ? BigDecimal.ZERO : forecastReceivableAmount; }
    public BigDecimal getPromisedAmount() { return promisedAmount; }
    public void setPromisedAmount(BigDecimal promisedAmount) { this.promisedAmount = promisedAmount == null ? BigDecimal.ZERO : promisedAmount; }
    public BigDecimal getActualReceivableAmount() { return actualReceivableAmount; }
    public void setActualReceivableAmount(BigDecimal actualReceivableAmount) { this.actualReceivableAmount = actualReceivableAmount == null ? BigDecimal.ZERO : actualReceivableAmount; }
    public BigDecimal getDeviationAmount() { return deviationAmount; }
    public void setDeviationAmount(BigDecimal deviationAmount) { this.deviationAmount = deviationAmount == null ? BigDecimal.ZERO : deviationAmount; }
    public BigDecimal getDeviationRate() { return deviationRate; }
    public void setDeviationRate(BigDecimal deviationRate) { this.deviationRate = deviationRate == null ? BigDecimal.ZERO : deviationRate; }
    public String getWindowLabel() { return windowLabel; }
    public void setWindowLabel(String windowLabel) { this.windowLabel = windowLabel; }
}
