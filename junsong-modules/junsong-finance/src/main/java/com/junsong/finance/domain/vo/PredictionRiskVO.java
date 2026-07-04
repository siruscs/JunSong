package com.junsong.finance.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 预测风险 VO。
 * 每个预测对象（现金流/应收/会员/库存）对应一个 PredictionRiskVO。
 * 字段约束：
 * - score: 0-100 可解释规则分
 * - level: LOW/MEDIUM/HIGH/CRITICAL
 * - factors: 命中的解释因子列表
 * - basis: 预测口径说明
 * - sampleDate: 样本日期或模拟日期
 */
public class PredictionRiskVO {

    private String predictionType;
    private String predictionLabel;

    private Long actionId;
    private Integer score = 0;
    private String level = "LOW";

    private BigDecimal forecastAmount = BigDecimal.ZERO;
    private BigDecimal actualAmount = BigDecimal.ZERO;
    private BigDecimal deviationAmount = BigDecimal.ZERO;
    private BigDecimal deviationRate = BigDecimal.ZERO;

    private Integer windowDays;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date sampleDate;

    private String basis;
    private String recommendation;
    private List<PredictionFactorVO> factors = new ArrayList<>();

    public String getPredictionType() { return predictionType; }
    public void setPredictionType(String predictionType) { this.predictionType = predictionType; }
    public String getPredictionLabel() { return predictionLabel; }
    public void setPredictionLabel(String predictionLabel) { this.predictionLabel = predictionLabel; }
    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score == null ? 0 : score; }
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
    public Integer getWindowDays() { return windowDays; }
    public void setWindowDays(Integer windowDays) { this.windowDays = windowDays; }
    public Date getSampleDate() { return sampleDate; }
    public void setSampleDate(Date sampleDate) { this.sampleDate = sampleDate; }
    public String getBasis() { return basis; }
    public void setBasis(String basis) { this.basis = basis; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public List<PredictionFactorVO> getFactors() { return factors; }
    public void setFactors(List<PredictionFactorVO> factors) { this.factors = factors == null ? new ArrayList<>() : factors; }
}
