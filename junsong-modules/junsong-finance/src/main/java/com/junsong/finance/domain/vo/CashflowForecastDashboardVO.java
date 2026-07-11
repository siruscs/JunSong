package com.junsong.finance.domain.vo;

import java.util.ArrayList;
import java.util.List;

public class CashflowForecastDashboardVO {
    private List<CashflowForecastWindowVO> windows = new ArrayList<>();
    private CashflowPressureVO pressure = new CashflowPressureVO();
    private List<CashflowForecastDeviationVO> forecastDeviation = new ArrayList<>();
    private CashflowForecastRhythmVO weeklyRhythm = new CashflowForecastRhythmVO();

    public List<CashflowForecastWindowVO> getWindows() { return windows; }
    public void setWindows(List<CashflowForecastWindowVO> windows) { this.windows = windows == null ? new ArrayList<>() : windows; }
    public CashflowPressureVO getPressure() { return pressure; }
    public void setPressure(CashflowPressureVO pressure) { this.pressure = pressure == null ? new CashflowPressureVO() : pressure; }
    public List<CashflowForecastDeviationVO> getForecastDeviation() { return forecastDeviation; }
    public void setForecastDeviation(List<CashflowForecastDeviationVO> forecastDeviation) { this.forecastDeviation = forecastDeviation == null ? new ArrayList<>() : forecastDeviation; }
    public CashflowForecastRhythmVO getWeeklyRhythm() { return weeklyRhythm; }
    public void setWeeklyRhythm(CashflowForecastRhythmVO weeklyRhythm) { this.weeklyRhythm = weeklyRhythm == null ? new CashflowForecastRhythmVO() : weeklyRhythm; }
}
