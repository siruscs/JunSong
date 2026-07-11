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

    String selectCurrentPeriodStatusByDeptIds(@Param("deptIds") List<Long> deptIds);
    FinAccountingPeriod selectPeriodById(@Param("periodId") Long periodId);
    FinAccountingPeriod selectPeriodForUpdate(@Param("periodId") Long periodId,
        @Param("tenantId") Long tenantId, @Param("deptId") Long deptId);

    /**
     * 查询上一周期（同店面、未删除、结束时间小于等于当前周期起始时间，排除当前周期）
     */
    public FinAccountingPeriod selectPreviousPeriod(@Param("deptId") Long deptId, @Param("startTime") Date startTime, @Param("periodId") Long periodId);

    /**
     * 查询下一周期（同店面、未删除、起始时间大于等于当前周期起始时间，排除当前周期）
     */
    public FinAccountingPeriod selectNextPeriod(@Param("deptId") Long deptId, @Param("startTime") Date startTime, @Param("periodId") Long periodId);

    /**
     * 运维调整：只更新起始时间、更新人、备注（不触碰金额和其他字段）
     */
    public int updateStartTimeOnly(@Param("periodId") Long periodId, @Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("updateBy") String updateBy, @Param("remark") String remark);
}
