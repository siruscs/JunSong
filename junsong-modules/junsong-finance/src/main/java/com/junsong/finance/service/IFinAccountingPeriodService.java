package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinAccountingPeriod;

public interface IFinAccountingPeriodService
{
    public FinAccountingPeriod selectFinAccountingPeriodByPeriodId(Long periodId);
    public FinAccountingPeriod selectCurrentPeriodByDeptId(Long deptId);
    public FinAccountingPeriod initCurrentPeriod(Long deptId);
    /** 试算回本：只刷新统计数据并返回结果，不改变周期状态 */
    public FinAccountingPeriod trialBreakEven(Long deptId);
    /** 结转：关闭当前周期，执行分润，创建新周期 */
    public FinAccountingPeriod carryForward(Long deptId);
    /** 结转回退：删除当前进行中的空周期，将最新已结转周期回退为进行中 */
    public FinAccountingPeriod rollbackCarryForward(Long deptId);
    public void assertPeriodEditable(Long periodId);
    public List<FinAccountingPeriod> selectFinAccountingPeriodList(FinAccountingPeriod finAccountingPeriod);
    public int insertFinAccountingPeriod(FinAccountingPeriod finAccountingPeriod);
    public int updateFinAccountingPeriod(FinAccountingPeriod finAccountingPeriod);
    public int deleteFinAccountingPeriodByPeriodIds(Long[] periodIds);
}
