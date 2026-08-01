package com.junsong.finance.mapper;

import com.junsong.finance.domain.vo.AuthorizedStoreRowVO;
import com.junsong.finance.domain.vo.StoreExpenseCategoryVO;
import com.junsong.finance.domain.vo.StorePendingItemVO;
import com.junsong.finance.domain.vo.StoreReportQueryParams;
import com.junsong.finance.domain.vo.StoreTrendRowVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 单门店财务报表Mapper接口
 */
public interface StoreFinanceReportMapper {

    BigDecimal selectStoreTotalSales(StoreReportQueryParams params);

    Integer countStoreSaleRecords(StoreReportQueryParams params);

    BigDecimal sumStoreSaleQuantity(StoreReportQueryParams params);

    BigDecimal selectStoreTotalExpense(StoreReportQueryParams params);

    BigDecimal selectStoreUnverifiedExpense(StoreReportQueryParams params);

    BigDecimal selectStoreUnverifiedAdvance(StoreReportQueryParams params);

    String selectCurrentAccountingPeriodStatus(@Param("deptId") Long deptId);

    List<StoreTrendRowVO> selectStoreSalesTrend(StoreReportQueryParams params);

    List<StoreTrendRowVO> selectStoreExpenseTrend(StoreReportQueryParams params);

    List<StoreExpenseCategoryVO> selectStoreExpenseCategories(StoreReportQueryParams params);

    List<StorePendingItemVO> selectStoreUnverifiedExpenses(StoreReportQueryParams params);

    List<StorePendingItemVO> selectStoreUnverifiedAdvances(StoreReportQueryParams params);

    BigDecimal selectStoreTotalSalesForRange(@Param("deptId") Long deptId,
                                             @Param("startTime") Date startTime,
                                             @Param("endTime") Date endTime);

    BigDecimal selectStoreTotalExpenseForRange(@Param("deptId") Long deptId,
                                               @Param("startTime") Date startTime,
                                               @Param("endTime") Date endTime);

    List<AuthorizedStoreRowVO> selectAuthorizedStoreRows(@Param("deptIds") List<Long> deptIds,
                                                         @Param("startTime") Date startTime,
                                                         @Param("endTime") Date endTime);

    List<Map<String, Object>> selectMemberSalesByDepts(@Param("deptIds") List<Long> deptIds,
                                                       @Param("startTime") Date startTime,
                                                       @Param("endTime") Date endTime);

    // R8-C: 多门店对比可行动字段

    /** 查询各门店实收现金（按 payment_date） */
    List<Map<String, Object>> selectCashInByDepts(@Param("deptIds") List<Long> deptIds,
                                                   @Param("startTime") Date startTime,
                                                   @Param("endTime") Date endTime);

    /** 查询各门店已核销费用（按 expense_date, status='1'） */
    List<Map<String, Object>> selectVerifiedExpenseByDepts(@Param("deptIds") List<Long> deptIds,
                                                             @Param("startTime") Date startTime,
                                                             @Param("endTime") Date endTime);

    /** 查询各门店高优先级复盘任务数（severity=HIGH 且 PENDING/IN_PROGRESS） */
    List<Map<String, Object>> selectHighRiskTaskCountByDepts(@Param("deptIds") List<Long> deptIds);

    /** R11-FIX-C: 查询各门店按周期的高优先级复盘任务数（按 create_time 落入周期） */
    List<Map<String, Object>> selectHighRiskTaskCountByDeptsAndPeriod(@Param("deptIds") List<Long> deptIds,
                                                                       @Param("startTime") Date startTime,
                                                                       @Param("endTime") Date endTime,
                                                                       @Param("groupBy") String groupBy);

    /** R11-C: 查询各门店按周期的销售额/费用/利润趋势（用于健康分计算） */
    List<Map<String, Object>> selectHealthTrendByDepts(@Param("deptIds") List<Long> deptIds,
                                                        @Param("startTime") Date startTime,
                                                        @Param("endTime") Date endTime,
                                                        @Param("groupBy") String groupBy);
}
