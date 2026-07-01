package com.junsong.finance.mapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.junsong.finance.domain.FinProfitShareRecord;

public interface FinProfitShareRecordMapper
{
    public FinProfitShareRecord selectFinProfitShareRecordByShareId(Long shareId);
    public FinProfitShareRecord selectFinProfitShareRecordByPeriodId(Long periodId);
    public List<FinProfitShareRecord> selectFinProfitShareRecordList(FinProfitShareRecord finProfitShareRecord);
    public int insertFinProfitShareRecord(FinProfitShareRecord finProfitShareRecord);
    public int updateFinProfitShareRecord(FinProfitShareRecord finProfitShareRecord);
    public int deleteFinProfitShareRecordByShareId(Long shareId);
    public int deleteFinProfitShareRecordByShareIds(Long[] shareIds);
    
    /**
     * 分润总额统计
     */
    public java.math.BigDecimal selectProfitShareTotal(Map<String, Object> params);
    
    /**
     * 店长分润总额统计
     */
    public java.math.BigDecimal selectManagerProfitTotal(Map<String, Object> params);
    
    /**
     * 投资人分润总额统计
     */
    public java.math.BigDecimal selectInvestorProfitTotal(Map<String, Object> params);
    
    /**
     * 店长分润门店统计
     */
    public List<Map<String, Object>> selectManagerProfitByDept(Map<String, Object> params);
    
    /**
     * 投资人分润门店统计
     */
    public List<Map<String, Object>> selectInvestorProfitByDept(Map<String, Object> params);
    
    /**
     * 分润趋势统计
     */
    public List<Map<String, Object>> selectProfitShareTrend(Map<String, Object> params);

    int countUnsettledRecords(@Param("deptIds") List<Long> deptIds);
    int countUnsettledRecordsByPeriodId(@Param("deptIds") List<Long> deptIds, @Param("periodId") Long periodId);
    List<Map<String, Object>> selectSettlementByDept(@Param("deptIds") List<Long> deptIds, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    BigDecimal selectPaidAmount(@Param("deptIds") List<Long> deptIds, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
}
