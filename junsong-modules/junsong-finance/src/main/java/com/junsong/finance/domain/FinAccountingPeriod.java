package com.junsong.finance.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class FinAccountingPeriod extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "周期ID", cellType = ColumnType.NUMERIC)
    private Long periodId;
    private Long deptId;
    @Excel(name = "周期编号")
    private String periodNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "周期开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "周期结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    @Excel(name = "状态", readConverterExp = "0=进行中,1=已回本待结转,2=已结转")
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date breakEvenTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date carryForwardTime;
    private BigDecimal totalVerifiedExpense;
    private BigDecimal totalPurchase;
    private BigDecimal totalSalePayment;
    private BigDecimal totalSaleAmount;
    private BigDecimal totalUnverifiedAdvance;
    private BigDecimal netProfit;
    private BigDecimal managerProfitRate;
    private BigDecimal managerProfitAmount;
    private BigDecimal investorProfitAmount;
    private String delFlag;

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getPeriodNo() { return periodNo; }
    public void setPeriodNo(String periodNo) { this.periodNo = periodNo; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getBreakEvenTime() { return breakEvenTime; }
    public void setBreakEvenTime(Date breakEvenTime) { this.breakEvenTime = breakEvenTime; }
    public Date getCarryForwardTime() { return carryForwardTime; }
    public void setCarryForwardTime(Date carryForwardTime) { this.carryForwardTime = carryForwardTime; }
    public BigDecimal getTotalVerifiedExpense() { return totalVerifiedExpense; }
    public void setTotalVerifiedExpense(BigDecimal totalVerifiedExpense) { this.totalVerifiedExpense = totalVerifiedExpense; }
    public BigDecimal getTotalPurchase() { return totalPurchase; }
    public void setTotalPurchase(BigDecimal totalPurchase) { this.totalPurchase = totalPurchase; }
    public BigDecimal getTotalSalePayment() { return totalSalePayment; }
    public void setTotalSalePayment(BigDecimal totalSalePayment) { this.totalSalePayment = totalSalePayment; }
    public BigDecimal getTotalSaleAmount() { return totalSaleAmount; }
    public void setTotalSaleAmount(BigDecimal totalSaleAmount) { this.totalSaleAmount = totalSaleAmount; }
    public BigDecimal getTotalUnverifiedAdvance() { return totalUnverifiedAdvance; }
    public void setTotalUnverifiedAdvance(BigDecimal totalUnverifiedAdvance) { this.totalUnverifiedAdvance = totalUnverifiedAdvance; }
    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
    public BigDecimal getManagerProfitRate() { return managerProfitRate; }
    public void setManagerProfitRate(BigDecimal managerProfitRate) { this.managerProfitRate = managerProfitRate; }
    public BigDecimal getManagerProfitAmount() { return managerProfitAmount; }
    public void setManagerProfitAmount(BigDecimal managerProfitAmount) { this.managerProfitAmount = managerProfitAmount; }
    public BigDecimal getInvestorProfitAmount() { return investorProfitAmount; }
    public void setInvestorProfitAmount(BigDecimal investorProfitAmount) { this.investorProfitAmount = investorProfitAmount; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("periodId", getPeriodId())
            .append("deptId", getDeptId())
            .append("periodNo", getPeriodNo())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("status", getStatus())
            .append("netProfit", getNetProfit())
            .append("remark", getRemark())
            .toString();
    }
}
