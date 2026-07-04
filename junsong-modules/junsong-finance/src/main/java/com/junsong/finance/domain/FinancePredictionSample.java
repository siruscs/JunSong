package com.junsong.finance.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 预测样本对象 finance_prediction_sample。
 * 用于保存 R24 预测辅助 V2 生成的样本（现金流/应收/会员/库存）。
 */
public class FinancePredictionSample extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long sampleId;
    private Long tenantId;
    private Long deptId;
    private String predictionType;
    private String sourceId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date sampleDate;

    private Integer windowDays;
    private Integer score = 0;
    private String level = "LOW";
    private BigDecimal forecastAmount = BigDecimal.ZERO;
    private BigDecimal actualAmount = BigDecimal.ZERO;
    private BigDecimal deviationAmount = BigDecimal.ZERO;
    private BigDecimal deviationRate = BigDecimal.ZERO;
    private String basis;

    public Long getSampleId() { return sampleId; }
    public void setSampleId(Long sampleId) { this.sampleId = sampleId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getPredictionType() { return predictionType; }
    public void setPredictionType(String predictionType) { this.predictionType = predictionType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public Date getSampleDate() { return sampleDate; }
    public void setSampleDate(Date sampleDate) { this.sampleDate = sampleDate; }
    public Integer getWindowDays() { return windowDays; }
    public void setWindowDays(Integer windowDays) { this.windowDays = windowDays; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public BigDecimal getForecastAmount() { return forecastAmount; }
    public void setForecastAmount(BigDecimal forecastAmount) { this.forecastAmount = forecastAmount == null ? BigDecimal.ZERO : forecastAmount; }
    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount == null ? BigDecimal.ZERO : actualAmount; }
    public BigDecimal getDeviationAmount() { return deviationAmount; }
    public void setDeviationAmount(BigDecimal deviationAmount) { this.deviationAmount = deviationAmount == null ? BigDecimal.ZERO : deviationAmount; }
    public BigDecimal getDeviationRate() { return deviationRate; }
    public void setDeviationRate(BigDecimal deviationRate) { this.deviationRate = deviationRate == null ? BigDecimal.ZERO : deviationRate; }
    public String getBasis() { return basis; }
    public void setBasis(String basis) { this.basis = basis; }
}
