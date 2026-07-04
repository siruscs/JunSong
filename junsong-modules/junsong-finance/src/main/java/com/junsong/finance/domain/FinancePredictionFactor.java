package com.junsong.finance.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;

import java.util.Date;

/**
 * 预测解释因子对象 finance_prediction_factor。
 * 用于保存 R24 预测命中的可解释因子。
 */
public class FinancePredictionFactor extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long factorId;
    private Long sampleId;
    private String factorCode;
    private String factorName;
    private String factorValue;
    private Integer factorWeight = 0;
    private String explanation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getFactorId() { return factorId; }
    public void setFactorId(Long factorId) { this.factorId = factorId; }
    public Long getSampleId() { return sampleId; }
    public void setSampleId(Long sampleId) { this.sampleId = sampleId; }
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
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
