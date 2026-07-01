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

    Integer sumStoreSaleQuantity(StoreReportQueryParams params);

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
}
