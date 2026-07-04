package com.junsong.finance.domain.vo;

/**
 * 预测解释因子 VO。
 * 用于前端展示命中规则。
 */
public class PredictionFactorVO {

    private String factorCode;
    private String factorName;
    private String factorValue;
    private Integer factorWeight = 0;
    private String explanation;

    public PredictionFactorVO() {
    }

    public PredictionFactorVO(String factorCode, String factorName, String factorValue, Integer factorWeight, String explanation) {
        this.factorCode = factorCode;
        this.factorName = factorName;
        this.factorValue = factorValue;
        this.factorWeight = factorWeight == null ? 0 : factorWeight;
        this.explanation = explanation;
    }

    public String getFactorCode() { return factorCode; }
    public void setFactorCode(String factorCode) { this.factorCode = factorCode; }
    public String getFactorName() { return factorName; }
    public void setFactorName(String factorName) { this.factorName = factorName; }
    public String getFactorValue() { return factorValue; }
    public void setFactorValue(String factorValue) { this.factorValue = factorValue; }
    public Integer getFactorWeight() { return factorWeight; }
    public void setFactorWeight(Integer factorWeight) { this.factorWeight = factorWeight == null ? 0 : factorWeight; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
