package com.junsong.finance.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * what-if 模拟结果 VO。
 * 只读模拟：返回基线压力分、模拟后压力分、变化分、命中的因子建议。
 * 不修改任何业务表（应收、会员、库存、动作中心）。
 */
public class WhatIfSimulationResultVO {

    private Long simulationId;
    private Long deptId;
    private Integer windowDays = 7;

    private Integer basePressureScore = 0;
    private String basePressureLevel = "LOW";
    private Integer simulatedPressureScore = 0;
    private String simulatedPressureLevel = "LOW";
    private Integer deltaScore = 0;
    private String deltaLevel = "UNCHANGED";

    private String basis;
    private String recommendation;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date simulationDate;

    private List<PredictionFactorVO> factors = new ArrayList<>();
    private List<String> impactAreas = new ArrayList<>();

    public Long getSimulationId() { return simulationId; }
    public void setSimulationId(Long simulationId) { this.simulationId = simulationId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Integer getWindowDays() { return windowDays; }
    public void setWindowDays(Integer windowDays) { this.windowDays = windowDays == null ? 7 : windowDays; }
    public Integer getBasePressureScore() { return basePressureScore; }
    public void setBasePressureScore(Integer basePressureScore) { this.basePressureScore = basePressureScore == null ? 0 : basePressureScore; }
    public String getBasePressureLevel() { return basePressureLevel; }
    public void setBasePressureLevel(String basePressureLevel) { this.basePressureLevel = basePressureLevel; }
    public Integer getSimulatedPressureScore() { return simulatedPressureScore; }
    public void setSimulatedPressureScore(Integer simulatedPressureScore) { this.simulatedPressureScore = simulatedPressureScore == null ? 0 : simulatedPressureScore; }
    public String getSimulatedPressureLevel() { return simulatedPressureLevel; }
    public void setSimulatedPressureLevel(String simulatedPressureLevel) { this.simulatedPressureLevel = simulatedPressureLevel; }
    public Integer getDeltaScore() { return deltaScore; }
    public void setDeltaScore(Integer deltaScore) { this.deltaScore = deltaScore == null ? 0 : deltaScore; }
    public String getDeltaLevel() { return deltaLevel; }
    public void setDeltaLevel(String deltaLevel) { this.deltaLevel = deltaLevel; }
    public String getBasis() { return basis; }
    public void setBasis(String basis) { this.basis = basis; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public Date getSimulationDate() { return simulationDate; }
    public void setSimulationDate(Date simulationDate) { this.simulationDate = simulationDate; }
    public List<PredictionFactorVO> getFactors() { return factors; }
    public void setFactors(List<PredictionFactorVO> factors) { this.factors = factors == null ? new ArrayList<>() : factors; }
    public List<String> getImpactAreas() { return impactAreas; }
    public void setImpactAreas(List<String> impactAreas) { this.impactAreas = impactAreas == null ? new ArrayList<>() : impactAreas; }
}
