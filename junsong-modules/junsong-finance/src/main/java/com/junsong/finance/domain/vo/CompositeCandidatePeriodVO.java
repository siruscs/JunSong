package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 复合核算手动纳入候选周期 VO
 *
 * @author junsong
 */
public class CompositeCandidatePeriodVO {

    /** 周期ID */
    private Long periodId;

    /** 店面ID */
    private Long deptId;

    /** 店面名称 */
    private String deptName;

    /** 周期编号 */
    private String periodNo;

    /** 周期净利 */
    private BigDecimal netProfit;

    /** 店长分润金额 */
    private BigDecimal managerProfitAmount;

    /** 可纳入复合核算金额 */
    private BigDecimal investorProfitAmount;

    /** 结转时间 */
    private String carryForwardTime;

    /** 是否已被纳入(true=已被其他池纳入) */
    private Boolean includedByOther;

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getPeriodNo() { return periodNo; }
    public void setPeriodNo(String periodNo) { this.periodNo = periodNo; }
    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
    public BigDecimal getManagerProfitAmount() { return managerProfitAmount; }
    public void setManagerProfitAmount(BigDecimal managerProfitAmount) { this.managerProfitAmount = managerProfitAmount; }
    public BigDecimal getInvestorProfitAmount() { return investorProfitAmount; }
    public void setInvestorProfitAmount(BigDecimal investorProfitAmount) { this.investorProfitAmount = investorProfitAmount; }
    public String getCarryForwardTime() { return carryForwardTime; }
    public void setCarryForwardTime(String carryForwardTime) { this.carryForwardTime = carryForwardTime; }
    public Boolean getIncludedByOther() { return includedByOther; }
    public void setIncludedByOther(Boolean includedByOther) { this.includedByOther = includedByOther; }
}
