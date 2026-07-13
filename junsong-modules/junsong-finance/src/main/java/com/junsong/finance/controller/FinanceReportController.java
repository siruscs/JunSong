package com.junsong.finance.controller;

import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.finance.domain.vo.*;
import com.junsong.finance.service.IFinanceReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/report")
public class FinanceReportController extends BaseController {

    @Autowired
    private IFinanceReportService financeReportService;

    @RequiresPermissions("finance:report:expense")
    @PostMapping("/expense")
    public AjaxResult getExpenseReport(@RequestBody ReportQueryParams params) {
        ExpenseReportVO report = financeReportService.getExpenseReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:cost")
    @PostMapping("/cost")
    public AjaxResult getCostReport(@RequestBody ReportQueryParams params) {
        CostReportVO report = financeReportService.getCostReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:profitShare")
    @PostMapping("/profitShare")
    public AjaxResult getProfitShareReport(@RequestBody ReportQueryParams params) {
        ProfitShareReportVO report = financeReportService.getProfitShareReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:sale")
    @PostMapping("/sale")
    public AjaxResult getSaleReport(@RequestBody ReportQueryParams params) {
        SaleReportVO report = financeReportService.getSaleReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:profit")
    @PostMapping("/profit")
    public AjaxResult getProfitReport(@RequestBody ReportQueryParams params) {
        ProfitReportVO report = financeReportService.getProfitReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:stock")
    @PostMapping("/stock")
    public AjaxResult getStockReport(@RequestBody StockReportQuery query) {
        StockReportVO report = financeReportService.getStockReport(query);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:stock")
    @PostMapping("/stock/summary")
    public AjaxResult getStockReportSummary(@RequestBody StockReportQuery query) {
        StockReportSummaryVO summary = financeReportService.getStockReportSummary(query);
        return AjaxResult.success(summary);
    }

    @RequiresPermissions("finance:report:stock")
    @PostMapping("/stock/page")
    public AjaxResult getStockReportPage(@RequestBody StockReportQuery query) {
        List<StockReportItemVO> items = financeReportService.getStockReportPage(query);
        return AjaxResult.success(items);
    }

    @RequiresPermissions("finance:report:stock")
    @PostMapping("/stock/ledger/page")
    public AjaxResult getStockLedgerPage(@RequestBody StockLedgerQuery query) {
        java.util.Map<String, Object> result = financeReportService.getStockLedgerPage(
                query.getDeptId(), query.getProductId(),
                query.getStartDate(), query.getEndDate(),
                query.getPageNum(), query.getPageSize());
        return AjaxResult.success(result);
    }

    @RequiresPermissions("finance:report:stock:export")
    @PostMapping("/stock/export")
    public AjaxResult exportStockReport(@RequestBody StockReportQuery query) {
        List<StockReportItemVO> items = financeReportService.exportStockReport(query);
        return AjaxResult.success(items);
    }

    @RequiresPermissions("finance:stock:reconciliation")
    @PostMapping("/stock/reconciliation")
    public AjaxResult getStockReconciliation(@RequestBody StockReportQuery query) {
        StockReconciliationResultVO result = financeReportService.getStockReconciliation(query);
        return AjaxResult.success(result);
    }

    @RequiresPermissions("finance:report:stock")
    @PostMapping("/stock/value")
    public AjaxResult getStockValueReport(@RequestBody StockReportQuery query) {
        StockValueReportVO report = financeReportService.getStockValueReport(query);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:stock:costAdjust")
    @PostMapping("/stock/cost-adjustment")
    public AjaxResult createCostAdjustment(@RequestBody CostAdjustmentRequest request) {
        financeReportService.createCostAdjustment(
                request.toQuery(), request.getProductId(), request.getAmount(), request.getReason());
        return AjaxResult.success();
    }

    /** 库存流水下钻查询参数。 */
    public static class StockLedgerQuery {
        private Long deptId;
        private Long productId;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer pageNum;
        private Integer pageSize;

        public Long getDeptId() { return deptId; }
        public void setDeptId(Long deptId) { this.deptId = deptId; }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

        public Integer getPageNum() { return pageNum; }
        public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }

        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    }

    /** 成本调整请求参数。 */
    public static class CostAdjustmentRequest {
        private List<Long> deptIds;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long productId;
        private BigDecimal amount;
        private String reason;

        public List<Long> getDeptIds() { return deptIds; }
        public void setDeptIds(List<Long> deptIds) { this.deptIds = deptIds; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public StockReportQuery toQuery() {
            StockReportQuery q = new StockReportQuery();
            q.setDeptIds(deptIds);
            q.setStartDate(startDate);
            q.setEndDate(endDate);
            return q;
        }
    }

    @RequiresPermissions("finance:report:profit")
    @PostMapping("/profit/operating")
    public AjaxResult getOperatingProfitReport(@RequestBody ReportQueryParams params) {
        OperatingProfitReportVO report = financeReportService.getOperatingProfitReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:expense")
    @PostMapping("/expense/anomalies")
    public AjaxResult getExpenseAnomalyReport(@RequestBody ReportQueryParams params) {
        ExpenseAnomalyReportVO report = financeReportService.getExpenseAnomalyReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:sale")
    @PostMapping("/sale/operation")
    public AjaxResult getSalesOperationReport(@RequestBody ReportQueryParams params) {
        SalesOperationReportVO report = financeReportService.getSalesOperationReport(params);
        return AjaxResult.success(report);
    }

    @RequiresPermissions("finance:report:profitShare")
    @PostMapping("/profitShare/settlement")
    public AjaxResult getProfitShareSettlement(@RequestBody ReportQueryParams params) {
        ProfitShareSettlementDashboardVO report = financeReportService.getProfitShareSettlement(params);
        return AjaxResult.success(report);
    }
}