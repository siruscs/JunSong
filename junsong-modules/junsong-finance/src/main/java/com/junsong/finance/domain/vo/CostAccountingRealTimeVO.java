package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 成本核算实时统计VO
 *
 * @author junsong
 */
public class CostAccountingRealTimeVO {

    /** 总花销费用 */
    private BigDecimal totalExpenseAmount;

    /** 总进货金额 */
    private BigDecimal totalPurchaseAmount;

    /** 总销售金额 */
    private BigDecimal totalSaleAmount;

    /** 总缴款金额 */
    private BigDecimal totalPaymentAmount;

    /** 投资来源金额 */
    private BigDecimal totalInvestAmount;

    /** 当前核算周期ID */
    private Long periodId;

    /** 当前核算周期编号 */
    private String periodNo;

    /** 当前核算周期开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date periodStartTime;

    /** 当前核算周期状态 */
    private String periodStatus;

    /** 回本时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date breakEvenTime;

    /** 当前借支（未核销借支总额） */
    private BigDecimal currentAdvance;

    /** 净利（销售缴款 - 已核销费用 - 进货款 - 借支未核销） */
    private BigDecimal profit;

    public BigDecimal getTotalExpenseAmount() {
        return totalExpenseAmount;
    }

    public void setTotalExpenseAmount(BigDecimal totalExpenseAmount) {
        this.totalExpenseAmount = totalExpenseAmount;
    }

    public BigDecimal getTotalPurchaseAmount() {
        return totalPurchaseAmount;
    }

    public void setTotalPurchaseAmount(BigDecimal totalPurchaseAmount) {
        this.totalPurchaseAmount = totalPurchaseAmount;
    }

    public BigDecimal getTotalSaleAmount() {
        return totalSaleAmount;
    }

    public void setTotalSaleAmount(BigDecimal totalSaleAmount) {
        this.totalSaleAmount = totalSaleAmount;
    }

    public BigDecimal getTotalPaymentAmount() {
        return totalPaymentAmount;
    }

    public void setTotalPaymentAmount(BigDecimal totalPaymentAmount) {
        this.totalPaymentAmount = totalPaymentAmount;
    }

    public BigDecimal getTotalInvestAmount() {
        return totalInvestAmount;
    }

    public void setTotalInvestAmount(BigDecimal totalInvestAmount) {
        this.totalInvestAmount = totalInvestAmount;
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public String getPeriodNo() {
        return periodNo;
    }

    public void setPeriodNo(String periodNo) {
        this.periodNo = periodNo;
    }

    public Date getPeriodStartTime() {
        return periodStartTime;
    }

    public void setPeriodStartTime(Date periodStartTime) {
        this.periodStartTime = periodStartTime;
    }

    public String getPeriodStatus() {
        return periodStatus;
    }

    public void setPeriodStatus(String periodStatus) {
        this.periodStatus = periodStatus;
    }

    public Date getBreakEvenTime() {
        return breakEvenTime;
    }

    public void setBreakEvenTime(Date breakEvenTime) {
        this.breakEvenTime = breakEvenTime;
    }

    public BigDecimal getCurrentAdvance() {
        return currentAdvance;
    }

    public void setCurrentAdvance(BigDecimal currentAdvance) {
        this.currentAdvance = currentAdvance;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }
}
