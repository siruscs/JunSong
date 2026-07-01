package com.junsong.finance.domain.vo;

import java.util.List;

/**
 * 锁账前检查结果VO
 *
 * @author junsong
 */
public class AccountingPeriodCheckResultVO
{
    /** 是否允许锁账 */
    private boolean canLock;

    /** 是否存在警告项 */
    private boolean hasWarning;

    /** 部门ID */
    private Long deptId;

    /** 核算周期ID */
    private Long periodId;

    /** 核算周期名称（周期编号） */
    private String periodName;

    /** 检查项列表 */
    private List<AccountingPeriodCheckItemVO> items;

    public boolean isCanLock()
    {
        return canLock;
    }

    public void setCanLock(boolean canLock)
    {
        this.canLock = canLock;
    }

    public boolean isHasWarning()
    {
        return hasWarning;
    }

    public void setHasWarning(boolean hasWarning)
    {
        this.hasWarning = hasWarning;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getPeriodId()
    {
        return periodId;
    }

    public void setPeriodId(Long periodId)
    {
        this.periodId = periodId;
    }

    public String getPeriodName()
    {
        return periodName;
    }

    public void setPeriodName(String periodName)
    {
        this.periodName = periodName;
    }

    public List<AccountingPeriodCheckItemVO> getItems()
    {
        return items;
    }

    public void setItems(List<AccountingPeriodCheckItemVO> items)
    {
        this.items = items;
    }
}
