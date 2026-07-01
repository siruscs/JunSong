package com.junsong.finance.service.diagnosis;

import java.math.BigDecimal;

/**
 * Aggregated financial metrics used by the diagnosis rules.
 *
 * <p>Populated by {@code FinanceReportServiceImpl} from mapper queries before
 * running the rule engine. All monetary fields default to {@link BigDecimal#ZERO}
 * to avoid null checks inside rules.</p>
 *
 * @author junsong
 * @since NIGHT-P1-C
 */
public class FinanceDiagnosisContext {

    // ── Sales metrics ────────────────────────────────────────────────────────
    private BigDecimal monthSales = BigDecimal.ZERO;
    private BigDecimal prevMonthSales = BigDecimal.ZERO;

    // ── Expense metrics ──────────────────────────────────────────────────────
    private BigDecimal monthExpense = BigDecimal.ZERO;
    private BigDecimal prevMonthExpense = BigDecimal.ZERO;

    // ── Derived profit metrics ───────────────────────────────────────────────
    private BigDecimal netProfit = BigDecimal.ZERO;
    private BigDecimal profitRate = BigDecimal.ZERO;  // percentage (e.g. 15.5 = 15.5%)

    // ── Verification metrics ─────────────────────────────────────────────────
    private int unverifiedExpenseCount;
    private BigDecimal unverifiedExpenseAmount = BigDecimal.ZERO;

    // ── Profit share metrics ─────────────────────────────────────────────────
    private int unsettledProfitShareCount;

    // ── Member metrics ───────────────────────────────────────────────────────
    private BigDecimal memberSales = BigDecimal.ZERO;
    private BigDecimal memberSalesRatio = BigDecimal.ZERO;  // percentage

    // ── Getters and setters ──────────────────────────────────────────────────

    public BigDecimal getMonthSales() { return monthSales; }
    public void setMonthSales(BigDecimal monthSales) { this.monthSales = monthSales != null ? monthSales : BigDecimal.ZERO; }

    public BigDecimal getPrevMonthSales() { return prevMonthSales; }
    public void setPrevMonthSales(BigDecimal prevMonthSales) { this.prevMonthSales = prevMonthSales != null ? prevMonthSales : BigDecimal.ZERO; }

    public BigDecimal getMonthExpense() { return monthExpense; }
    public void setMonthExpense(BigDecimal monthExpense) { this.monthExpense = monthExpense != null ? monthExpense : BigDecimal.ZERO; }

    public BigDecimal getPrevMonthExpense() { return prevMonthExpense; }
    public void setPrevMonthExpense(BigDecimal prevMonthExpense) { this.prevMonthExpense = prevMonthExpense != null ? prevMonthExpense : BigDecimal.ZERO; }

    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit != null ? netProfit : BigDecimal.ZERO; }

    public BigDecimal getProfitRate() { return profitRate; }
    public void setProfitRate(BigDecimal profitRate) { this.profitRate = profitRate != null ? profitRate : BigDecimal.ZERO; }

    public int getUnverifiedExpenseCount() { return unverifiedExpenseCount; }
    public void setUnverifiedExpenseCount(int unverifiedExpenseCount) { this.unverifiedExpenseCount = unverifiedExpenseCount; }

    public BigDecimal getUnverifiedExpenseAmount() { return unverifiedExpenseAmount; }
    public void setUnverifiedExpenseAmount(BigDecimal unverifiedExpenseAmount) { this.unverifiedExpenseAmount = unverifiedExpenseAmount != null ? unverifiedExpenseAmount : BigDecimal.ZERO; }

    public int getUnsettledProfitShareCount() { return unsettledProfitShareCount; }
    public void setUnsettledProfitShareCount(int unsettledProfitShareCount) { this.unsettledProfitShareCount = unsettledProfitShareCount; }

    public BigDecimal getMemberSales() { return memberSales; }
    public void setMemberSales(BigDecimal memberSales) { this.memberSales = memberSales != null ? memberSales : BigDecimal.ZERO; }

    public BigDecimal getMemberSalesRatio() { return memberSalesRatio; }
    public void setMemberSalesRatio(BigDecimal memberSalesRatio) { this.memberSalesRatio = memberSalesRatio != null ? memberSalesRatio : BigDecimal.ZERO; }

    // ── Convenience: sales change rate (%) ────────────────────────────────────

    /** Returns sales change rate as a percentage, or null if prevMonthSales is zero. */
    public BigDecimal salesChangeRate() {
        if (prevMonthSales.signum() == 0) return null;
        return monthSales.subtract(prevMonthSales)
                .multiply(new BigDecimal("100"))
                .divide(prevMonthSales, 2, BigDecimal.ROUND_HALF_UP);
    }

    /** Returns expense change rate as a percentage, or null if prevMonthExpense is zero. */
    public BigDecimal expenseChangeRate() {
        if (prevMonthExpense.signum() == 0) return null;
        return monthExpense.subtract(prevMonthExpense)
                .multiply(new BigDecimal("100"))
                .divide(prevMonthExpense, 2, BigDecimal.ROUND_HALF_UP);
    }
}
