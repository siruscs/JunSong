package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

public class CashflowForecastRhythmVO {
    private BigDecimal weeklyForecastAmount = BigDecimal.ZERO;
    private BigDecimal weeklyOverduePromiseAmount = BigDecimal.ZERO;
    private String weeklyPressureLevel = "LOW";
    private String recommendedAction;

    public BigDecimal getWeeklyForecastAmount() { return weeklyForecastAmount; }
    public void setWeeklyForecastAmount(BigDecimal weeklyForecastAmount) { this.weeklyForecastAmount = weeklyForecastAmount == null ? BigDecimal.ZERO : weeklyForecastAmount; }
    public BigDecimal getWeeklyOverduePromiseAmount() { return weeklyOverduePromiseAmount; }
    public void setWeeklyOverduePromiseAmount(BigDecimal weeklyOverduePromiseAmount) { this.weeklyOverduePromiseAmount = weeklyOverduePromiseAmount == null ? BigDecimal.ZERO : weeklyOverduePromiseAmount; }
    public String getWeeklyPressureLevel() { return weeklyPressureLevel; }
    public void setWeeklyPressureLevel(String weeklyPressureLevel) { this.weeklyPressureLevel = weeklyPressureLevel; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
}
