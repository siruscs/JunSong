package com.junsong.finance.service;

import com.junsong.finance.domain.vo.CashflowDashboardVO;
import com.junsong.finance.domain.vo.ReportQueryParams;

public interface IFinanceCashflowReportService {
    CashflowDashboardVO getCashflowDashboard(ReportQueryParams params);
}
