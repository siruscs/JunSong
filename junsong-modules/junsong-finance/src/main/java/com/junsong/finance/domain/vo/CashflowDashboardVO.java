package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cash health dashboard VO.
 * Aggregates cashflow-related metrics across authorized departments.
 */
public class CashflowDashboardVO {

    /** netCashInflow = totalReceivedSalePayment - totalVerifiedExpense */
    private BigDecimal netCashInflow = BigDecimal.ZERO;

    /** SUM(payment_amount) FROM fin_sale_payment WHERE del_flag='0' */
    private BigDecimal totalReceivedSalePayment = BigDecimal.ZERO;

    /** SUM(expense_amount) FROM fin_expense WHERE status='1' AND del_flag='0' */
    private BigDecimal totalVerifiedExpense = BigDecimal.ZERO;

    /** SUM(expense_amount) FROM fin_expense WHERE status='0' AND del_flag='0' */
    private BigDecimal totalUnverifiedExpense = BigDecimal.ZERO;

    /** SUM(advance_amount) FROM fin_advance WHERE status='0' AND del_flag='0' */
    private BigDecimal totalAdvanceBalance = BigDecimal.ZERO;

    /** SUM(amount) FROM fin_investor_payment WHERE payment_status='1' AND del_flag='0' */
    private BigDecimal totalPaidInvestorPayment = BigDecimal.ZERO;

    /** SUM(amount) FROM fin_investor_payment WHERE payment_status='0' AND del_flag='0' */
    private BigDecimal totalUnpaidInvestorPayment = BigDecimal.ZERO;

    /** count of unverified expenses + count of unpaid investor payments */
    private int cashPressureItems = 0;

    /** departments that were queried */
    private List<Long> deptIds;

    public BigDecimal getNetCashInflow() {
        return netCashInflow;
    }

    public void setNetCashInflow(BigDecimal netCashInflow) {
        this.netCashInflow = netCashInflow;
    }

    public BigDecimal getTotalReceivedSalePayment() {
        return totalReceivedSalePayment;
    }

    public void setTotalReceivedSalePayment(BigDecimal totalReceivedSalePayment) {
        this.totalReceivedSalePayment = totalReceivedSalePayment;
    }

    public BigDecimal getTotalVerifiedExpense() {
        return totalVerifiedExpense;
    }

    public void setTotalVerifiedExpense(BigDecimal totalVerifiedExpense) {
        this.totalVerifiedExpense = totalVerifiedExpense;
    }

    public BigDecimal getTotalUnverifiedExpense() {
        return totalUnverifiedExpense;
    }

    public void setTotalUnverifiedExpense(BigDecimal totalUnverifiedExpense) {
        this.totalUnverifiedExpense = totalUnverifiedExpense;
    }

    public BigDecimal getTotalAdvanceBalance() {
        return totalAdvanceBalance;
    }

    public void setTotalAdvanceBalance(BigDecimal totalAdvanceBalance) {
        this.totalAdvanceBalance = totalAdvanceBalance;
    }

    public BigDecimal getTotalPaidInvestorPayment() {
        return totalPaidInvestorPayment;
    }

    public void setTotalPaidInvestorPayment(BigDecimal totalPaidInvestorPayment) {
        this.totalPaidInvestorPayment = totalPaidInvestorPayment;
    }

    public BigDecimal getTotalUnpaidInvestorPayment() {
        return totalUnpaidInvestorPayment;
    }

    public void setTotalUnpaidInvestorPayment(BigDecimal totalUnpaidInvestorPayment) {
        this.totalUnpaidInvestorPayment = totalUnpaidInvestorPayment;
    }

    public int getCashPressureItems() {
        return cashPressureItems;
    }

    public void setCashPressureItems(int cashPressureItems) {
        this.cashPressureItems = cashPressureItems;
    }

    public List<Long> getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(List<Long> deptIds) {
        this.deptIds = deptIds;
    }
}
