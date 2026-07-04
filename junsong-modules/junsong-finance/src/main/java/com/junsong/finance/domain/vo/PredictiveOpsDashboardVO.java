package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 预测辅助 V2 仪表盘 VO。
 * 4 个预测对象 + 现金压力基线 + 最近样本。
 */
public class PredictiveOpsDashboardVO {

    private Long deptId;
    private Integer windowDays = 7;
    private Integer basePressureScore = 0;
    private String basePressureLevel = "LOW";
    private String basis;

    private PredictionRiskVO cashflow;
    private PredictionRiskVO receivable;
    private PredictionRiskVO memberAction;
    private PredictionRiskVO stock;

    private List<PredictionFactorVO> recentFactors = new ArrayList<>();
    private BigDecimal totalForecastAmount = BigDecimal.ZERO;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Integer getWindowDays() { return windowDays; }
    public void setWindowDays(Integer windowDays) { this.windowDays = windowDays == null ? 7 : windowDays; }
    public Integer getBasePressureScore() { return basePressureScore; }
    public void setBasePressureScore(Integer basePressureScore) { this.basePressureScore = basePressureScore == null ? 0 : basePressureScore; }
    public String getBasePressureLevel() { return basePressureLevel; }
    public void setBasePressureLevel(String basePressureLevel) { this.basePressureLevel = basePressureLevel; }
    public String getBasis() { return basis; }
    public void setBasis(String basis) { this.basis = basis; }
    public PredictionRiskVO getCashflow() { return cashflow; }
    public void setCashflow(PredictionRiskVO cashflow) { this.cashflow = cashflow; }
    public PredictionRiskVO getReceivable() { return receivable; }
    public void setReceivable(PredictionRiskVO receivable) { this.receivable = receivable; }
    public PredictionRiskVO getMemberAction() { return memberAction; }
    public void setMemberAction(PredictionRiskVO memberAction) { this.memberAction = memberAction; }
    public PredictionRiskVO getStock() { return stock; }
    public void setStock(PredictionRiskVO stock) { this.stock = stock; }
    public List<PredictionFactorVO> getRecentFactors() { return recentFactors; }
    public void setRecentFactors(List<PredictionFactorVO> recentFactors) { this.recentFactors = recentFactors == null ? new ArrayList<>() : recentFactors; }
    public BigDecimal getTotalForecastAmount() { return totalForecastAmount; }
    public void setTotalForecastAmount(BigDecimal totalForecastAmount) { this.totalForecastAmount = totalForecastAmount == null ? BigDecimal.ZERO : totalForecastAmount; }
}
