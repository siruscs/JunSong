package com.junsong.finance.service;

import com.junsong.finance.domain.vo.CashflowForecastDashboardVO;
import com.junsong.finance.domain.vo.CashflowForecastQueryParams;

public interface ICashflowForecastService {
    CashflowForecastDashboardVO getDashboard(CashflowForecastQueryParams params);

    int createSnapshot(CashflowForecastQueryParams params);
}
