package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

/**
 * 费用统计VO
 * 
 * @author junsong
 */
public class ExpenseSummary
{
    /** 未核销借支金额 */
    private BigDecimal unverifiedAdvanceAmount;

    /** 未核销费用总金额 */
    private BigDecimal unverifiedExpenseAmount;

    /** 借支余额 */
    private BigDecimal advanceBalance;

    private BigDecimal totalExpenseAmount;

    public BigDecimal getTotalExpenseAmount()
    {
        return totalExpenseAmount;
    }

    public void setTotalExpenseAmount(BigDecimal totalExpenseAmount)
    {
        this.totalExpenseAmount = totalExpenseAmount;
    }

    public BigDecimal getUnverifiedAdvanceAmount()
    {
        return unverifiedAdvanceAmount;
    }

    public void setUnverifiedAdvanceAmount(BigDecimal unverifiedAdvanceAmount)
    {
        this.unverifiedAdvanceAmount = unverifiedAdvanceAmount;
    }

    public BigDecimal getUnverifiedExpenseAmount()
    {
        return unverifiedExpenseAmount;
    }

    public void setUnverifiedExpenseAmount(BigDecimal unverifiedExpenseAmount)
    {
        this.unverifiedExpenseAmount = unverifiedExpenseAmount;
    }

    public BigDecimal getAdvanceBalance()
    {
        return advanceBalance;
    }

    public void setAdvanceBalance(BigDecimal advanceBalance)
    {
        this.advanceBalance = advanceBalance;
    }
}
