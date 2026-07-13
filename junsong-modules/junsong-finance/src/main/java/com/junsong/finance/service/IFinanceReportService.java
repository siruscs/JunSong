package com.junsong.finance.service;

import com.junsong.finance.domain.vo.*;
import java.math.BigDecimal;
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
    /**
     * 库存价值报表（第二期财务计价）。
     * 返回 costReady 标志、期间状态、金额汇总和毛利；costReady=false 时金额为零、items 为空。
     * LOCKED/CARRIED_FORWARD 期间拒绝调整回写。
     */
    StockValueReportVO getStockValueReport(StockReportQuery query);
    /**
     * 在当前 ACTIVE 期间生成成本调整流水。
     * LOCKED 或 CARRIED_FORWARD 期间拒绝回写；调整必须有原因和操作者。
     */
    void createCostAdjustment(StockReportQuery query, Long productId, BigDecimal amount, String reason);
    FinanceOperationDashboardVO getOperationDashboard(ReportQueryParams params);
    OperatingProfitReportVO getOperatingProfitReport(ReportQueryParams params);
    ExpenseAnomalyReportVO getExpenseAnomalyReport(ReportQueryParams params);
    SalesOperationReportVO getSalesOperationReport(ReportQueryParams params);
    ProfitShareSettlementDashboardVO getProfitShareSettlement(ReportQueryParams params);
    List<FinanceAlertVO> getAlerts(ReportQueryParams params);
    List<FinanceReviewTaskVO> getReviewTasks(ReportQueryParams params);
}