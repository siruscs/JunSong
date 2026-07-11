package com.junsong.finance.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;

public class CashflowForecastDeviationVO {
    private Long snapshotId;
    private Long deptId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date forecastDate;

    private Integer windowDays;
    private BigDecimal forecastReceivableAmount = BigDecimal.ZERO;
    private BigDecimal actualReceivableAmount = BigDecimal.ZERO;
    private BigDecimal deviationAmount = BigDecimal.ZERO;
    private BigDecimal deviationRate = BigDecimal.ZERO;
    private String pressureLevel;

    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Date getForecastDate() { return forecastDate; }
    public void setForecastDate(Date forecastDate) { this.forecastDate = forecastDate; }
    public Integer getWindowDays() { return windowDays; }
    public void setWindowDays(Integer windowDays) { this.windowDays = windowDays; }
    public BigDecimal getForecastReceivableAmount() { return forecastReceivableAmount; }
    public void setForecastReceivableAmount(BigDecimal forecastReceivableAmount) { this.forecastReceivableAmount = forecastReceivableAmount == null ? BigDecimal.ZERO : forecastReceivableAmount; }
    public BigDecimal getActualReceivableAmount() { return actualReceivableAmount; }
    public void setActualReceivableAmount(BigDecimal actualReceivableAmount) { this.actualReceivableAmount = actualReceivableAmount == null ? BigDecimal.ZERO : actualReceivableAmount; }
    public BigDecimal getDeviationAmount() { return deviationAmount; }
    public void setDeviationAmount(BigDecimal deviationAmount) { this.deviationAmount = deviationAmount == null ? BigDecimal.ZERO : deviationAmount; }
    public BigDecimal getDeviationRate() { return deviationRate; }
    public void setDeviationRate(BigDecimal deviationRate) { this.deviationRate = deviationRate == null ? BigDecimal.ZERO : deviationRate; }
    public String getPressureLevel() { return pressureLevel; }
    public void setPressureLevel(String pressureLevel) { this.pressureLevel = pressureLevel; }
}
