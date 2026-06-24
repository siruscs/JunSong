package com.junsong.finance.mapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.junsong.finance.domain.FinAccountingPeriod;

public interface FinAccountingPeriodMapper
{
    public FinAccountingPeriod selectFinAccountingPeriodByPeriodId(Long periodId);
    public FinAccountingPeriod selectCurrentPeriodByDeptId(Long deptId);
    public FinAccountingPeriod selectLatestCarriedPeriodByDeptId(Long deptId);
    public List<FinAccountingPeriod> selectFinAccountingPeriodList(FinAccountingPeriod finAccountingPeriod);
    public int insertFinAccountingPeriod(FinAccountingPeriod finAccountingPeriod);
    public int updateFinAccountingPeriod(FinAccountingPeriod finAccountingPeriod);
    public int resetCarryForwardByPeriodId(@Param("periodId") Long periodId, @Param("updateBy") String updateBy);
    public int deleteFinAccountingPeriodByPeriodId(Long periodId);
    public int deleteFinAccountingPeriodByPeriodIds(Long[] periodIds);
    public BigDecimal selectTotalVerifiedExpense(@Param("periodId") Long periodId, @Param("deptId") Long deptId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    public BigDecimal selectTotalPurchase(@Param("periodId") Long periodId, @Param("deptId") Long deptId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    public BigDecimal selectTotalSalePayment(@Param("periodId") Long periodId, @Param("deptId") Long deptId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    public BigDecimal selectTotalSaleAmount(@Param("periodId") Long periodId, @Param("deptId") Long deptId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    public BigDecimal selectTotalUnverifiedAdvance(@Param("periodId") Long periodId, @Param("deptId") Long deptId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
}
