package com.junsong.finance.mapper;

import com.junsong.finance.domain.FinanceCashflowForecastSnapshot;
import com.junsong.finance.domain.vo.CashflowForecastDeviationVO;
import com.junsong.finance.domain.vo.CashflowForecastQueryParams;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CashflowForecastMapper {
    BigDecimal sumPromisedAmount(@Param("params") CashflowForecastQueryParams params, @Param("windowDays") int windowDays);

    BigDecimal sumActualPaymentAmount(@Param("params") CashflowForecastQueryParams params, @Param("windowDays") int windowDays);

    BigDecimal sumTotalUnpaidAmount(CashflowForecastQueryParams params);

    BigDecimal sumOverduePromiseAmount(CashflowForecastQueryParams params);

    BigDecimal sumAge30PlusAmount(CashflowForecastQueryParams params);

    BigDecimal sumRecentCashInAmount(CashflowForecastQueryParams params);

    BigDecimal sumRecentExpenseAmount(CashflowForecastQueryParams params);

    List<CashflowForecastDeviationVO> selectRecentDeviation(CashflowForecastQueryParams params);

    int insertSnapshot(FinanceCashflowForecastSnapshot snapshot);
}
