package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

/**
 * 投资人返款统计VO
 * 
 * @author junsong
 */
public class InvestorPaymentSummary
{
    /** 投资来源金额 */
    private BigDecimal totalInvestAmount;

    /** 总返款金额 */
    private BigDecimal totalReturnAmount;

    /** 备用金总额 */
    private BigDecimal reserveFund;

    /** 当前阶段已核销费用 */
    private BigDecimal verifiedExpenseAmount;

    /** 未核销借支金额 */
    private BigDecimal unverifiedAdvanceAmount;

    /** 店面费用余额 */
    private BigDecimal storeExpenseBalance;

    /** 费用余额（别名为前端兼容） */
    private BigDecimal expenseBalance;

    public BigDecimal getTotalInvestAmount()
    {
        return totalInvestAmount;
    }

    public void setTotalInvestAmount(BigDecimal totalInvestAmount)
    {
        this.totalInvestAmount = totalInvestAmount;
    }

    public BigDecimal getTotalReturnAmount()
    {
        return totalReturnAmount;
    }

    public void setTotalReturnAmount(BigDecimal totalReturnAmount)
    {
        this.totalReturnAmount = totalReturnAmount;
    }

    public BigDecimal getReserveFund()
    {
        return reserveFund;
    }

    public void setReserveFund(BigDecimal reserveFund)
    {
        this.reserveFund = reserveFund;
    }

    public BigDecimal getVerifiedExpenseAmount()
    {
        return verifiedExpenseAmount;
    }

    public void setVerifiedExpenseAmount(BigDecimal verifiedExpenseAmount)
    {
        this.verifiedExpenseAmount = verifiedExpenseAmount;
    }

    public BigDecimal getUnverifiedAdvanceAmount()
    {
        return unverifiedAdvanceAmount;
    }

    public void setUnverifiedAdvanceAmount(BigDecimal unverifiedAdvanceAmount)
    {
        this.unverifiedAdvanceAmount = unverifiedAdvanceAmount;
    }

    public BigDecimal getStoreExpenseBalance()
    {
        return storeExpenseBalance;
    }

    public void setStoreExpenseBalance(BigDecimal storeExpenseBalance)
    {
        this.storeExpenseBalance = storeExpenseBalance;
    }

    public BigDecimal getExpenseBalance()
    {
        return expenseBalance;
    }

    public void setExpenseBalance(BigDecimal expenseBalance)
    {
        this.expenseBalance = expenseBalance;
    }
}
