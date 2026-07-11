package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

/**
 * 复合核算汇总 VO
 *
 * @author junsong
 */
public class CompositeAccountingSummaryVO {

    /** 汇总后的总费用 */
    private BigDecimal totalVerifiedExpense = BigDecimal.ZERO;

    /** 汇总后的总进货 */
    private BigDecimal totalPurchase = BigDecimal.ZERO;

    /** 汇总后的总销售 */
    private BigDecimal totalSaleAmount = BigDecimal.ZERO;

    /** 汇总后的总缴款 */
    private BigDecimal totalSalePayment = BigDecimal.ZERO;

    /** 汇总后的未核销借支 */
    private BigDecimal totalUnverifiedAdvance = BigDecimal.ZERO;

    /** 回本差额 */
    private BigDecimal breakEvenGap = BigDecimal.ZERO;

    /** 理论回本差额(汇总后的总费用+总进货+未核销借支-总缴款) */
    private BigDecimal theoreticalBreakEvenGap = BigDecimal.ZERO;

    public BigDecimal getTotalVerifiedExpense() { return totalVerifiedExpense; }
    public void setTotalVerifiedExpense(BigDecimal totalVerifiedExpense) { this.totalVerifiedExpense = totalVerifiedExpense; }
    public BigDecimal getTotalPurchase() { return totalPurchase; }
    public void setTotalPurchase(BigDecimal totalPurchase) { this.totalPurchase = totalPurchase; }
    public BigDecimal getTotalSaleAmount() { return totalSaleAmount; }
    public void setTotalSaleAmount(BigDecimal totalSaleAmount) { this.totalSaleAmount = totalSaleAmount; }
    public BigDecimal getTotalSalePayment() { return totalSalePayment; }
    public void setTotalSalePayment(BigDecimal totalSalePayment) { this.totalSalePayment = totalSalePayment; }
    public BigDecimal getTotalUnverifiedAdvance() { return totalUnverifiedAdvance; }
    public void setTotalUnverifiedAdvance(BigDecimal totalUnverifiedAdvance) { this.totalUnverifiedAdvance = totalUnverifiedAdvance; }
    public BigDecimal getBreakEvenGap() { return breakEvenGap; }
    public void setBreakEvenGap(BigDecimal breakEvenGap) { this.breakEvenGap = breakEvenGap; }
    public BigDecimal getTheoreticalBreakEvenGap() { return theoreticalBreakEvenGap; }
    public void setTheoreticalBreakEvenGap(BigDecimal theoreticalBreakEvenGap) { this.theoreticalBreakEvenGap = theoreticalBreakEvenGap; }
}
