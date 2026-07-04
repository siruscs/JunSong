package com.junsong.finance.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.web.domain.BaseEntity;

import java.util.Date;

/**
 * what-if 模拟记录对象 finance_what_if_simulation。
 * 用于保存 R24 what-if 模拟输入和结果（只读模拟，不写业务表）。
 */
public class FinanceWhatIfSimulation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long simulationId;
    private Long tenantId;
    private Long deptId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date simulationDate;

    private Integer basePressureScore = 0;
    private Integer simulatedPressureScore = 0;
    private Integer deltaScore = 0;
    private String inputJson;
    private String resultJson;

    public Long getSimulationId() { return simulationId; }
    public void setSimulationId(Long simulationId) { this.simulationId = simulationId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Date getSimulationDate() { return simulationDate; }
    public void setSimulationDate(Date simulationDate) { this.simulationDate = simulationDate; }
    public Integer getBasePressureScore() { return basePressureScore; }
    public void setBasePressureScore(Integer basePressureScore) { this.basePressureScore = basePressureScore == null ? 0 : basePressureScore; }
    public Integer getSimulatedPressureScore() { return simulatedPressureScore; }
    public void setSimulatedPressureScore(Integer simulatedPressureScore) { this.simulatedPressureScore = simulatedPressureScore == null ? 0 : simulatedPressureScore; }
    public Integer getDeltaScore() { return deltaScore; }
    public void setDeltaScore(Integer deltaScore) { this.deltaScore = deltaScore == null ? 0 : deltaScore; }
    public String getInputJson() { return inputJson; }
    public void setInputJson(String inputJson) { this.inputJson = inputJson; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
}
