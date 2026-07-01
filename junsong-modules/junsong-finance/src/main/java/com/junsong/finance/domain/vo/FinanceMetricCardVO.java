package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

public class FinanceMetricCardVO {
    private String label;
    private BigDecimal value = BigDecimal.ZERO;
    private BigDecimal previousValue = BigDecimal.ZERO;
    private BigDecimal changeRate = BigDecimal.ZERO;
    private String unit = "";
    private String desc;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public BigDecimal getPreviousValue() { return previousValue; }
    public void setPreviousValue(BigDecimal previousValue) { this.previousValue = previousValue; }
    public BigDecimal getChangeRate() { return changeRate; }
    public void setChangeRate(BigDecimal changeRate) { this.changeRate = changeRate; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
}
