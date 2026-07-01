package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

/**
 * 锁账前检查项VO
 *
 * @author junsong
 */
public class AccountingPeriodCheckItemVO
{
    /** 检查类型：UNVERIFIED_EXPENSE, UNVERIFIED_ADVANCE, UNSETTLED_PROFIT_SHARE, UNPAID_INVESTOR */
    private String checkType;

    /** 级别：BLOCK, WARNING, INFO */
    private String level;

    /** 中文标题 */
    private String title;

    /** 中文描述 */
    private String description;

    /** 记录数 */
    private int count;

    /** 总金额 */
    private BigDecimal amount;

    public AccountingPeriodCheckItemVO()
    {
    }

    public AccountingPeriodCheckItemVO(String checkType, String level, String title, String description, int count, BigDecimal amount)
    {
        this.checkType = checkType;
        this.level = level;
        this.title = title;
        this.description = description;
        this.count = count;
        this.amount = amount;
    }

    public String getCheckType()
    {
        return checkType;
    }

    public void setCheckType(String checkType)
    {
        this.checkType = checkType;
    }

    public String getLevel()
    {
        return level;
    }

    public void setLevel(String level)
    {
        this.level = level;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getCount()
    {
        return count;
    }

    public void setCount(int count)
    {
        this.count = count;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }
}
