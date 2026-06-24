package com.junsong.finance.domain.vo;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 费用批量核销请求对象
 * 
 * @author junsong
 */
public class ExpenseVerifyVO
{
    /** 费用记录ID列表 */
    @NotEmpty(message = "请选择要核销的费用记录")
    private List<Long> expenseIds;

    /** 关联借支记录ID列表 */
    private List<Long> advanceIds;

    public List<Long> getExpenseIds()
    {
        return expenseIds;
    }

    public void setExpenseIds(List<Long> expenseIds)
    {
        this.expenseIds = expenseIds;
    }

    public List<Long> getAdvanceIds()
    {
        return advanceIds;
    }

    public void setAdvanceIds(List<Long> advanceIds)
    {
        this.advanceIds = advanceIds;
    }
}
