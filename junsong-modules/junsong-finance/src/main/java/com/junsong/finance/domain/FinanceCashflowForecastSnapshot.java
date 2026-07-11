package com.junsong.finance.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 现金流预测快照对象 finance_cashflow_forecast_snapshot。
 */
public class FinanceCashflowForecastSnapshot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long snapshotId;
    private Long deptId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date forecastDate;

    private Integer windowDays;
    private BigDecimal forecastReceivableAmount = BigDecimal.ZERO;
    private BigDecimal actualReceivableAmount;
    private BigDecimal deviationAmount;
    private BigDecimal deviationRate;
    private Integer pressureScore = 0;
    private String pressureLevel = "LOW";
    private String forecastBasis;
    private Long tenantId;

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
    public void setActualReceivableAmount(BigDecimal actualReceivableAmount) { this.actualReceivableAmount = actualReceivableAmount; }
    public BigDecimal getDeviationAmount() { return deviationAmount; }
    public void setDeviationAmount(BigDecimal deviationAmount) { this.deviationAmount = deviationAmount; }
    public BigDecimal getDeviationRate() { return deviationRate; }
    public void setDeviationRate(BigDecimal deviationRate) { this.deviationRate = deviationRate; }
    public Integer getPressureScore() { return pressureScore; }
    public void setPressureScore(Integer pressureScore) { this.pressureScore = pressureScore; }
    public String getPressureLevel() { return pressureLevel; }
    public void setPressureLevel(String pressureLevel) { this.pressureLevel = pressureLevel; }
    public String getForecastBasis() { return forecastBasis; }
    public void setForecastBasis(String forecastBasis) { this.forecastBasis = forecastBasis; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}
