package com.junsong.finance.service;

import com.junsong.finance.domain.vo.*;

public interface IFinanceReportService {
    ExpenseReportVO getExpenseReport(ReportQueryParams params);
    CostReportVO getCostReport(ReportQueryParams params);
    ProfitShareReportVO getProfitShareReport(ReportQueryParams params);
    SaleReportVO getSaleReport(ReportQueryParams params);
    ProfitReportVO getProfitReport(ReportQueryParams params);
    StockReportVO getStockReport(ReportQueryParams params);
}