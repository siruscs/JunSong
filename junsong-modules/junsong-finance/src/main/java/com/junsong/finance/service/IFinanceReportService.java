package com.junsong.finance.service;

import com.junsong.finance.domain.vo.*;
import java.util.Map;

import java.time.LocalDate;
import java.util.List;

public interface IFinanceReportService {
    ExpenseReportVO getExpenseReport(ReportQueryParams params);
    CostReportVO getCostReport(ReportQueryParams params);
    ProfitShareReportVO getProfitShareReport(ReportQueryParams params);
    SaleReportVO getSaleReport(ReportQueryParams params);
    ProfitReportVO getProfitReport(ReportQueryParams params);
    StockReportVO getStockReport(StockReportQuery query);
    StockReportSummaryVO getStockReportSummary(StockReportQuery query);
    List<StockReportItemVO> getStockReportPage(StockReportQuery query);
    Map<String, Object> getStockLedgerPage(Long deptId, Long productId, LocalDate startDate, LocalDate endDate, Integer pageNum, Integer pageSize);
    List<StockReportItemVO> exportStockReport(StockReportQuery query);
    StockReconciliationResultVO getStockReconciliation(StockReportQuery query);
    FinanceOperationDashboardVO getOperationDashboard(ReportQueryParams params);
    OperatingProfitReportVO getOperatingProfitReport(ReportQueryParams params);
    ExpenseAnomalyReportVO getExpenseAnomalyReport(ReportQueryParams params);
    SalesOperationReportVO getSalesOperationReport(ReportQueryParams params);
    ProfitShareSettlementDashboardVO getProfitShareSettlement(ReportQueryParams params);
    List<FinanceAlertVO> getAlerts(ReportQueryParams params);
    List<FinanceReviewTaskVO> getReviewTasks(ReportQueryParams params);
}