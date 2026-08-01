package com.junsong.finance.service.impl;


import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.vo.*;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.mapper.FinAccountingPeriodMapper;
import com.junsong.finance.mapper.FinExpenseMapper;
import com.junsong.finance.mapper.FinProfitShareDetailMapper;
import com.junsong.finance.mapper.FinProfitShareRecordMapper;
import com.junsong.finance.mapper.FinSaleRecordMapper;
import com.junsong.finance.mapper.StockReportMapper;
import com.junsong.finance.service.IFinanceReportService;
import com.junsong.finance.service.IStockCostService;
import com.junsong.finance.service.IStockHealthService;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisContext;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisResult;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisRule;
import com.junsong.finance.service.diagnosis.FinanceDiagnosisRuleEngine;
import com.junsong.finance.service.diagnosis.rules.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinanceReportServiceImpl implements IFinanceReportService {

    @Autowired
    private FinExpenseMapper finExpenseMapper;

    @Autowired
    private FinProfitShareRecordMapper finProfitShareRecordMapper;

    @Autowired
    private FinProfitShareDetailMapper finProfitShareDetailMapper;

    @Autowired
    private FinSaleRecordMapper finSaleRecordMapper;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private FinAccountingPeriodMapper finAccountingPeriodMapper;

    @Autowired
    private StockReportMapper stockReportMapper;

    @Autowired
    private IStockHealthService stockHealthService;

    /**
     * 库存成本计价服务（第二期）。使用 required=false 保证第一期的测试和未启用计价的部署仍可运行。
     */
    @Autowired(required = false)
    private IStockCostService stockCostService;

    /**
     * Diagnosis rule engine — NIGHT-P1-C refactoring.
     * Alerts and review tasks now share the same diagnosis source.
     */
    private final FinanceDiagnosisRuleEngine diagnosisEngine = createDefaultEngine();

    private static FinanceDiagnosisRuleEngine createDefaultEngine() {
        List<FinanceDiagnosisRule> rules = Arrays.asList(
                new SalesDropRule(),
                new ExpenseSpikeRule(),
                new ProfitRateLowRule(),
                new PendingVerifyHighRule(),
                new ProfitShareExceptionRule(),
                new MemberContributionDropRule()
        );
        return new FinanceDiagnosisRuleEngine(rules);
    }

    @Override
    public ExpenseReportVO getExpenseReport(ReportQueryParams params) {
        ExpenseReportVO vo = new ExpenseReportVO();

        Map<String, Object> queryParams = buildQueryParams(params);
        vo.setTotalExpense(finExpenseMapper.selectExpenseTotal(queryParams));
        vo.setCategoryStats(finExpenseMapper.selectExpenseCategoryStats(queryParams));
        vo.setTrendStats(finExpenseMapper.selectExpenseTrendStats(queryParams));
        vo.setDeptStats(finExpenseMapper.selectExpenseDeptStats(queryParams));

        return vo;
    }

    @Override
    public CostReportVO getCostReport(ReportQueryParams params) {
        CostReportVO vo = new CostReportVO();

        Map<String, Object> queryParams = buildQueryParams(params);
        vo.setTotalCost(finExpenseMapper.selectExpenseTotal(queryParams));
        vo.setCategoryStats(finExpenseMapper.selectExpenseCategoryStats(queryParams));
        vo.setTrendStats(buildCostTrendStats(finExpenseMapper.selectExpenseTrendStats(queryParams)));
        vo.setDeptStats(finExpenseMapper.selectExpenseDeptStats(queryParams));

        return vo;
    }

    @Override
    public ProfitShareReportVO getProfitShareReport(ReportQueryParams params) {
        ProfitShareReportVO vo = new ProfitShareReportVO();

        Map<String, Object> queryParams = buildQueryParams(params);

        vo.setTotalManagerProfit(finProfitShareRecordMapper.selectManagerProfitTotal(queryParams));
        vo.setTotalInvestorProfit(finProfitShareRecordMapper.selectInvestorProfitTotal(queryParams));
        vo.setTotalProfitShare(finProfitShareRecordMapper.selectProfitShareTotal(queryParams));
        vo.setManagerStats(finProfitShareRecordMapper.selectManagerProfitByDept(queryParams));
        vo.setInvestorStats(finProfitShareRecordMapper.selectInvestorProfitByDept(queryParams));
        vo.setTrendStats(finProfitShareRecordMapper.selectProfitShareTrend(queryParams));

        return vo;
    }

    @Override
    public SaleReportVO getSaleReport(ReportQueryParams params) {
        SaleReportVO vo = new SaleReportVO();

        applyDataScope(params);
        List<Map<String, Object>> trendStats = finSaleRecordMapper.selectSaleTrendStats(params.getDeptIds(), params.getStartTime(), params.getEndTime());
        BigDecimal totalSales = sumField(trendStats, "totalSales");
        int totalCount = finSaleRecordMapper.countSaleRecords(params.getDeptIds(), params.getStartTime(), params.getEndTime());
        BigDecimal totalQuantity = nzBig(finSaleRecordMapper.sumSaleQuantity(params.getDeptIds(), params.getStartTime(), params.getEndTime()));
        vo.setTotalSales(totalSales);
        vo.setTotalCount(totalCount);
        vo.setAvgPrice(totalCount > 0
                ? totalSales.divide(new BigDecimal(totalCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        vo.setDeptStats(buildDeptTotalStats(trendStats, "totalSales"));
        vo.setRankStats(buildDeptTotalStats(trendStats, "totalSales"));
        vo.setTrendStats(trendStats);

        return vo;
    }

    @Override
    public ProfitReportVO getProfitReport(ReportQueryParams params) {
        ProfitReportVO vo = new ProfitReportVO();

        applyDataScope(params);
        Map<String, Object> queryParams = buildQueryParams(params);
        List<Map<String, Object>> salesTrend = finSaleRecordMapper.selectSaleTrendStats(params.getDeptIds(), params.getStartTime(), params.getEndTime());
        List<Map<String, Object>> costTrend = buildCostTrendStats(finExpenseMapper.selectExpenseTrendStats(queryParams));
        List<Map<String, Object>> profitTrend = buildProfitTrendStats(salesTrend, costTrend);
        BigDecimal totalProfit = sumField(profitTrend, "profit");
        BigDecimal totalSales = sumField(salesTrend, "totalSales");
        BigDecimal totalCost = sumField(costTrend, "totalAmount");

        vo.setTotalProfit(totalProfit);
        // 利润率 = 利润 / 销售额 * 100
        vo.setProfitRate(totalSales.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.multiply(new BigDecimal("100")).divide(totalSales, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        // 回本进度 = 利润 / 成本 * 100
        vo.setRecoveryRate(totalCost.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.multiply(new BigDecimal("100")).divide(totalCost, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        vo.setDeptStats(buildDeptTotalStats(profitTrend, "profit"));
        vo.setRecoveryStats(buildDeptTotalStats(profitTrend, "profit"));
        vo.setTrendStats(profitTrend);

        return vo;
    }

    @Override
    public StockReportVO getStockReport(StockReportQuery query) {
        StockReportSummaryVO summary = getStockReportSummary(query);
        List<StockReportItemVO> items = getStockReportPage(query);
        long total = stockReportMapper.countStockReportItems(TenantContext.getTenantId(), query);

        StockReportVO vo = new StockReportVO();
        vo.setSummary(summary);
        vo.setItems(items);
        vo.setTotal(total);
        vo.setPageNum(query.getPageNum() != null ? query.getPageNum() : 1);
        vo.setPageSize(query.getPageSize() != null ? query.getPageSize() : 20);
        return vo;
    }

    @Override
    public StockReportSummaryVO getStockReportSummary(StockReportQuery query) {
        Long tenantId = TenantContext.getTenantId();
        applyStockDataScope(query);
        validateStockReportRequest(tenantId, query);
        return stockReportMapper.selectStockReportSummary(tenantId, query);
    }

    @Override
    public List<StockReportItemVO> getStockReportPage(StockReportQuery query) {
        Long tenantId = TenantContext.getTenantId();
        applyStockDataScope(query);
        validateStockReportRequest(tenantId, query);
        return stockReportMapper.selectStockReportItems(tenantId, query);
    }

    @Override
    public Map<String, Object> getStockLedgerPage(Long deptId, Long productId,
                                                      LocalDate startDate, LocalDate endDate,
                                                      Integer pageNum, Integer pageSize) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止查询库存流水");
        }
        if (deptId == null || productId == null) {
            throw new ServiceException("门店ID和商品ID不能为空");
        }
        // 授权门店校验：非 admin 必须确认 deptId 在授权范围内
        assertDeptAuthorized(deptId);

        int page = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 200);

        List<StockLedgerRowVO> allRows = stockReportMapper.selectStockLedgerRows(
                tenantId, deptId, productId, startDate, endDate);
        int total = allRows == null ? 0 : allRows.size();
        List<StockLedgerRowVO> pageRows;
        if (total == 0) {
            pageRows = Collections.emptyList();
        } else {
            int start = (page - 1) * size;
            if (start >= total) {
                pageRows = Collections.emptyList();
            } else {
                int end = Math.min(start + size, total);
                pageRows = new ArrayList<>(allRows.subList(start, end));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", pageRows);
        result.put("total", total);
        result.put("pageNum", page);
        result.put("pageSize", size);
        return result;
    }

    @Override
    public List<StockReportItemVO> exportStockReport(StockReportQuery query) {
        Long tenantId = TenantContext.getTenantId();
        applyStockDataScope(query);
        validateStockReportRequest(tenantId, query);
        return stockReportMapper.selectAllStockReportItems(tenantId, query);
    }

    @Override
    public StockReconciliationResultVO getStockReconciliation(StockReportQuery query) {
        Long tenantId = TenantContext.getTenantId();
        applyStockDataScope(query);
        validateStockReportRequest(tenantId, query);
        return stockHealthService.reconcileStock(tenantId, query.getDeptIds());
    }

    @Override
    public StockValueReportVO getStockValueReport(StockReportQuery query) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止查询库存价值报表");
        }
        applyStockDataScope(query);
        validateStockReportRequest(tenantId, query);

        StockValueReportVO vo = new StockValueReportVO();
        vo.setCostReady(false);
        vo.setPeriodStatus(resolvePeriodStatus(query.getDeptIds()));
        vo.setOpeningAmount(BigDecimal.ZERO);
        vo.setInboundAmount(BigDecimal.ZERO);
        vo.setSaleCost(BigDecimal.ZERO);
        vo.setAdjustmentAmount(BigDecimal.ZERO);
        vo.setClosingAmount(BigDecimal.ZERO);
        vo.setSaleRevenue(BigDecimal.ZERO);
        vo.setGrossProfit(BigDecimal.ZERO);
        vo.setGrossProfitRate(BigDecimal.ZERO);
        vo.setItems(Collections.emptyList());

        // costReady 门禁：
        // 1. 必须存在至少一条成本层记录（防止未初始化）
        // 2. 授权范围内所有有库存流水的商品都必须有成本层（防止部分初始化导致报表遗漏）
        boolean hasAnyCostLayer = stockReportMapper.existsCostLayerForTenant(tenantId, query.getDeptIds());
        int missingCount = stockReportMapper.countStockProductsWithoutCostLayer(tenantId, query.getDeptIds());
        boolean costReady = hasAnyCostLayer && missingCount == 0;
        if (!costReady) {
            return vo;
        }
        vo.setCostReady(true);

        StockValueReportVO summary = stockReportMapper.selectStockValueSummary(tenantId, query);
        if (summary != null) {
            vo.setOpeningAmount(nullSafe(summary.getOpeningAmount()));
            vo.setInboundAmount(nullSafe(summary.getInboundAmount()));
            vo.setSaleCost(nullSafe(summary.getSaleCost()));
            vo.setAdjustmentAmount(nullSafe(summary.getAdjustmentAmount()));
            vo.setClosingAmount(nullSafe(summary.getClosingAmount()));
            vo.setSaleRevenue(nullSafe(summary.getSaleRevenue()));
            BigDecimal grossProfit = nullSafe(summary.getSaleRevenue())
                    .subtract(nullSafe(summary.getSaleCost()))
                    .subtract(nullSafe(summary.getAdjustmentAmount()));
            vo.setGrossProfit(grossProfit);
            if (nullSafe(summary.getSaleRevenue()).compareTo(BigDecimal.ZERO) > 0) {
                vo.setGrossProfitRate(grossProfit.multiply(new BigDecimal("100"))
                        .divide(nullSafe(summary.getSaleRevenue()), 2, java.math.RoundingMode.HALF_UP));
            }
        }
        List<StockValueReportItemVO> items = stockReportMapper.selectStockValueItems(tenantId, query);
        vo.setItems(items != null ? items : Collections.emptyList());
        return vo;
    }

    @Override
    public void createCostAdjustment(StockReportQuery query, Long productId, BigDecimal amount, String reason) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止成本调整");
        }
        if (productId == null) {
            throw new ServiceException("成本调整缺少商品上下文");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new ServiceException("成本调整必须填写原因");
        }
        if (amount == null || amount.signum() == 0) {
            throw new ServiceException("成本调整金额不能为零");
        }
        applyStockDataScope(query);
        validateStockReportRequest(tenantId, query);

        // 期间控制：LOCKED(1) 或 CARRIED_FORWARD(2) 期间拒绝回写
        String periodStatus = resolvePeriodStatus(query.getDeptIds());
        if (!"ACTIVE".equals(periodStatus)) {
            throw new ServiceException("当前会计期间为 " + periodStatus + "（LOCKED/CARRIED_FORWARD），拒绝回写成本调整");
        }

        // 调整必须在授权门店范围内执行；取第一个授权门店作为调整目标
        if (query.getDeptIds() == null || query.getDeptIds().isEmpty()) {
            throw new ServiceException("成本调整缺少门店上下文");
        }
        Long deptId = query.getDeptIds().get(0);
        assertDeptAuthorized(deptId);

        if (stockCostService == null) {
            throw new ServiceException("成本计价服务未启用，拒绝成本调整");
        }
        String operator = SecurityUtils.getUsername();
        stockCostService.applyCostAdjustment(tenantId, deptId, productId, amount, reason, operator);
    }

    /**
     * 解析当前会计期间状态为标准化标签。
     * '0' -> ACTIVE, '1' -> LOCKED, '2' -> CARRIED_FORWARD, null/其他 -> ACTIVE（默认开放）。
     */
    private String resolvePeriodStatus(List<Long> deptIds) {
        String raw = finAccountingPeriodMapper.selectCurrentPeriodStatusByDeptIds(deptIds);
        if (raw == null) {
            return "ACTIVE";
        }
        switch (raw) {
            case "0": return "ACTIVE";
            case "1": return "LOCKED";
            case "2": return "CARRIED_FORWARD";
            default: return "ACTIVE";
        }
    }

    /**
     * 库存报表数据权限：admin 可见全部门店；非 admin 取请求部门与授权部门的交集，
     * 交集为空时 fail-closed 抛出 ServiceException。
     */
    private void applyStockDataScope(StockReportQuery query) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            Long currentDeptId = SecurityUtils.getDeptId();
            if (currentDeptId != null) {
                allowed = Collections.singletonList(currentDeptId);
            } else {
                throw new ServiceException("当前用户无授权门店，禁止查询库存报表");
            }
        }
        List<Long> requested = query.getDeptIds();
        if (requested == null || requested.isEmpty()) {
            query.setDeptIds(new ArrayList<>(allowed));
            return;
        }
        List<Long> finalAllowed = allowed;
        List<Long> filtered = requested.stream()
                .filter(finalAllowed::contains)
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            throw new ServiceException("请求的门店均不在授权范围内");
        }
        query.setDeptIds(filtered);
    }

    /**
     * 校验库存报表请求：租户上下文必填、查询参数必填、pageSize 1..200、日期区间最长 366 天。
     * 将 IllegalArgumentException 转为 ServiceException 以返回安全业务提示。
     */
    static void validateStockReportRequest(Long tenantId, StockReportQuery query) {
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止查询库存报表");
        }
        if (query == null) {
            throw new ServiceException("查询参数不能为空");
        }
        Integer pageSize = query.getPageSize();
        if (pageSize != null && (pageSize < 1 || pageSize > 200)) {
            throw new ServiceException("每页大小必须在1-200之间");
        }
        try {
            query.validate();
        } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 校验单个门店是否在当前用户授权范围内。
     * admin 可访问全部门店；非 admin 必须在 loadAllowedDeptIds 返回的列表中。
     * 交集为空时 fail-closed 抛出 ServiceException。
     */
    private void assertDeptAuthorized(Long deptId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            Long currentDeptId = SecurityUtils.getDeptId();
            if (currentDeptId != null) {
                allowed = Collections.singletonList(currentDeptId);
            } else {
                throw new ServiceException("当前用户无授权门店，禁止查询库存流水");
            }
        }
        if (!allowed.contains(deptId)) {
            throw new ServiceException("该门店不在授权范围内，禁止查询库存流水");
        }
    }

    @Override
    public FinanceOperationDashboardVO getOperationDashboard(ReportQueryParams params) {
        FinanceOperationDashboardVO vo = new FinanceOperationDashboardVO();
        applyDataScope(params);
        List<Long> deptIds = params.getDeptIds();

        vo.setTodaySales(nullSafe(finSaleRecordMapper.selectTodayTotalSales(deptIds)));
        vo.setMonthSales(nullSafe(finSaleRecordMapper.selectMonthTotalSales(deptIds)));
        vo.setTodayExpense(nullSafe(finExpenseMapper.selectTodayTotalExpense(deptIds)));
        vo.setMonthExpense(nullSafe(finExpenseMapper.selectMonthTotalExpense(deptIds)));
        if (deptIds != null && !deptIds.isEmpty()) {
            FinAccountingPeriod period = finAccountingPeriodMapper.selectCurrentPeriodByDeptId(deptIds.get(0));
            if (period != null && period.getStartTime() != null) {
                Date periodEnd = period.getEndTime() != null ? period.getEndTime() : new Date();
                vo.setCurrentPeriodSales(nullSafe(finSaleRecordMapper.selectPeriodTotalSales(deptIds, period.getStartTime(), periodEnd)));
                vo.setCurrentPeriodExpense(nullSafe(finExpenseMapper.selectPeriodTotalExpense(deptIds, period.getStartTime(), periodEnd)));
            }
        }

        BigDecimal monthSales = vo.getMonthSales();
        BigDecimal monthExpense = vo.getMonthExpense();
        vo.setNetProfit(monthSales.subtract(monthExpense));
        vo.setGrossProfit(monthSales.subtract(monthExpense));
        vo.setProfitRate(monthSales.compareTo(BigDecimal.ZERO) > 0
                ? vo.getNetProfit().multiply(new BigDecimal("100")).divide(monthSales, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        List<Map<String, Object>> salesByDept = finSaleRecordMapper.selectSalesByDept(deptIds, params.getStartTime(), params.getEndTime());
        vo.setSalesTopStores(buildStoreRankRows(salesByDept, "totalSales", 5));
        vo.setProfitTopStores(buildStoreRankRows(salesByDept, "totalSales", 5));

        vo.setUnverifiedExpenseCount(finExpenseMapper.countUnverifiedExpenses(deptIds));
        vo.setUnverifiedExpenseAmount(nullSafe(finExpenseMapper.sumUnverifiedExpenseAmount(deptIds)));
        vo.setUnsettledProfitShareCount(finProfitShareRecordMapper.countUnsettledRecords(deptIds));

        String periodStatus = finAccountingPeriodMapper.selectCurrentPeriodStatusByDeptIds(deptIds);
        vo.setCurrentPeriodStatus(periodStatus != null ? periodStatus : "ACTIVE");

        // R13-D: 应收指标
        Long currentPeriodId = null;
        if (deptIds != null && !deptIds.isEmpty()) {
            FinAccountingPeriod cp = finAccountingPeriodMapper.selectCurrentPeriodByDeptId(deptIds.get(0));
            if (cp != null) { currentPeriodId = cp.getPeriodId(); }
        }
        vo.setCurrentPeriodPaymentAmount(nullSafe(finSaleRecordMapper.selectCurrentPeriodPaymentTotal(deptIds, currentPeriodId)));
        vo.setHistoricalReceivableCollectedAmount(nullSafe(finSaleRecordMapper.selectHistoricalReceivableCollected(deptIds, currentPeriodId)));
        vo.setCurrentPeriodNewReceivableAmount(nullSafe(finSaleRecordMapper.selectCurrentPeriodNewReceivable(deptIds, currentPeriodId)));
        vo.setEndingReceivableAmount(nullSafe(finSaleRecordMapper.selectEndingReceivableBalance(deptIds)));
        vo.setOverdueReceivableCount(finSaleRecordMapper.countOverdueReceivable(deptIds));

        List<FinanceWarningVO> warnings = new ArrayList<>();
        BigDecimal prevMonthSales = nullSafe(finSaleRecordMapper.selectMonthTotalSalesForPrev(deptIds));
        if (prevMonthSales.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal salesChange = monthSales.subtract(prevMonthSales).multiply(new BigDecimal("100")).divide(prevMonthSales, 2, java.math.RoundingMode.HALF_UP);
            if (salesChange.compareTo(new BigDecimal("-20")) < 0) {
                FinanceWarningVO w = new FinanceWarningVO();
                w.setWarningType("SALES_DROP");
                w.setSeverity("HIGH");
                w.setTitle("销售下滑预警");
                w.setMessage("本月销售额较上月下降 " + salesChange.abs() + "%");
                warnings.add(w);
            }
        }
        if (vo.getProfitRate().compareTo(new BigDecimal("5")) < 0 && monthSales.compareTo(BigDecimal.ZERO) > 0) {
            FinanceWarningVO w = new FinanceWarningVO();
            w.setWarningType("PROFIT_RATE_DROP");
            w.setSeverity("MEDIUM");
            w.setTitle("利润率偏低预警");
            w.setMessage("当前利润率 " + vo.getProfitRate() + "% 低于 5%");
            warnings.add(w);
        }
        vo.setWarnings(warnings);
        return vo;
    }

    @Override
    public OperatingProfitReportVO getOperatingProfitReport(ReportQueryParams params) {
        OperatingProfitReportVO vo = new OperatingProfitReportVO();
        applyDataScope(params);
        List<Long> deptIds = params.getDeptIds();
        Map<String, Object> queryParams = buildQueryParams(params);

        List<Map<String, Object>> salesTrend = finSaleRecordMapper.selectSaleTrendStats(deptIds, params.getStartTime(), params.getEndTime());
        BigDecimal salesSum = sumField(salesTrend, "totalSales");
        BigDecimal expenseSum = nullSafe(finExpenseMapper.selectExpenseTotal(queryParams));

        vo.setTotalIncome(salesSum);
        vo.setOperatingExpense(expenseSum.abs());
        vo.setProductCost(BigDecimal.ZERO);
        vo.setCostReliable(false);
        vo.setCostNote("成本口径待核算确认");
        vo.setGrossProfit(salesSum);
        vo.setNetProfit(salesSum.subtract(expenseSum.abs()));
        vo.setProfitRate(salesSum.compareTo(BigDecimal.ZERO) > 0
                ? vo.getNetProfit().multiply(new BigDecimal("100")).divide(salesSum, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        vo.setRecoveryRate(BigDecimal.ZERO);

        List<Map<String, Object>> salesByDept = finSaleRecordMapper.selectSalesByDept(deptIds, params.getStartTime(), params.getEndTime());
        vo.setStoreProfitRank(salesByDept);
        vo.setStoreProfitRateRank(salesByDept);
        vo.setTrendStats(buildProfitTrendStats(salesTrend, buildCostTrendStats(finExpenseMapper.selectExpenseTrendStats(queryParams))));
        return vo;
    }

    @Override
    public ExpenseAnomalyReportVO getExpenseAnomalyReport(ReportQueryParams params) {
        ExpenseAnomalyReportVO vo = new ExpenseAnomalyReportVO();
        applyDataScope(params);
        List<Long> deptIds = params.getDeptIds();
        Map<String, Object> queryParams = buildQueryParams(params);

        vo.setTotalExpense(nullSafe(finExpenseMapper.selectExpenseTotal(queryParams)));
        vo.setCategoryBreakdown(finExpenseMapper.selectExpenseCategoryStats(queryParams));
        vo.setStoreExpenseRank(finExpenseMapper.selectExpenseDeptStats(queryParams));

        List<Map<String, Object>> unverified = finExpenseMapper.selectUnverifiedExpenseList(deptIds);
        List<ExpenseAnomalyRowVO> unverifiedRows = new ArrayList<>();
        if (unverified != null) {
            for (Map<String, Object> row : unverified) {
                ExpenseAnomalyRowVO ar = new ExpenseAnomalyRowVO();
                ar.setAnomalyType("UNVERIFIED");
                ar.setLabel(String.valueOf(row.getOrDefault("label", "")));
                ar.setExpenseId(toLong(row.get("expenseId")));
                ar.setExpenseNo(String.valueOf(row.getOrDefault("expenseNo", "")));
                ar.setDeptId(toLong(row.get("deptId")));
                ar.setDeptName(String.valueOf(row.getOrDefault("deptName", "")));
                ar.setCurrentAmount(toBigDecimal(row.get("currentAmount")));
                unverifiedRows.add(ar);
            }
        }
        vo.setUnverifiedList(unverifiedRows);

        List<Map<String, Object>> ocrRows = finExpenseMapper.selectOcrAnomalies(deptIds);
        List<ExpenseAnomalyRowVO> ocrAnomalies = new ArrayList<>();
        if (ocrRows != null) {
            for (Map<String, Object> row : ocrRows) {
                ExpenseAnomalyRowVO ar = new ExpenseAnomalyRowVO();
                ar.setAnomalyType(String.valueOf(row.getOrDefault("anomalyType", "OTHER")));
                ar.setLabel(String.valueOf(row.getOrDefault("label", "")));
                ar.setExpenseId(toLong(row.get("expenseId")));
                ar.setExpenseNo(String.valueOf(row.getOrDefault("expenseNo", "")));
                ar.setDeptId(toLong(row.get("deptId")));
                ar.setDeptName(String.valueOf(row.getOrDefault("deptName", "")));
                ar.setCurrentAmount(toBigDecimal(row.get("currentAmount")));
                ocrAnomalies.add(ar);
            }
        }
        vo.setOcrAnomalies(ocrAnomalies);
        vo.setCategorySpikes(Collections.emptyList());
        vo.setStoreSpikes(Collections.emptyList());
        return vo;
    }

    @Override
    public SalesOperationReportVO getSalesOperationReport(ReportQueryParams params) {
        SalesOperationReportVO vo = new SalesOperationReportVO();
        applyDataScope(params);
        List<Long> deptIds = params.getDeptIds();

        List<Map<String, Object>> trendStats = finSaleRecordMapper.selectSaleTrendStats(deptIds, params.getStartTime(), params.getEndTime());
        BigDecimal totalSales = sumField(trendStats, "totalSales");
        int orderCount = finSaleRecordMapper.countSaleRecords(deptIds, params.getStartTime(), params.getEndTime());
        BigDecimal totalQuantity = nzBig(finSaleRecordMapper.sumSaleQuantity(deptIds, params.getStartTime(), params.getEndTime()));

        vo.setTotalSales(totalSales);
        vo.setOrderCount(orderCount);
        vo.setTotalQuantity(totalQuantity);
        vo.setAvgOrderAmount(orderCount > 0
                ? totalSales.divide(new BigDecimal(orderCount), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        vo.setAvgItemAmount(nzBig(totalQuantity).signum() > 0
                ? totalSales.divide(nzBig(totalQuantity), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);

        BigDecimal memberSales = nullSafe(finSaleRecordMapper.selectMemberSales(deptIds, params.getStartTime(), params.getEndTime()));
        vo.setMemberSales(memberSales);
        vo.setNonMemberSales(totalSales.subtract(memberSales));

        BigDecimal seckillSales = nullSafe(finSaleRecordMapper.selectSeckillSales(deptIds, params.getStartTime(), params.getEndTime()));
        vo.setSeckillSales(seckillSales);
        vo.setNormalSales(totalSales.subtract(seckillSales));

        List<Map<String, Object>> salesByDept = finSaleRecordMapper.selectSalesByDept(deptIds, params.getStartTime(), params.getEndTime());
        List<SalesRankRowVO> storeRank = new ArrayList<>();
        if (salesByDept != null) {
            for (Map<String, Object> row : salesByDept) {
                SalesRankRowVO sr = new SalesRankRowVO();
                sr.setDeptId(toLong(row.get("deptId")));
                sr.setDeptName(String.valueOf(row.getOrDefault("deptName", "")));
                sr.setAmount(toBigDecimal(row.get("totalSales")));
                sr.setQuantity(toBigDecimal(row.get("orderCount")));
                storeRank.add(sr);
            }
        }
        vo.setStoreRank(storeRank);

        List<Map<String, Object>> productRankData = finSaleRecordMapper.selectProductSalesRank(deptIds, params.getStartTime(), params.getEndTime());
        List<SalesRankRowVO> productRank = new ArrayList<>();
        if (productRankData != null) {
            for (Map<String, Object> row : productRankData) {
                SalesRankRowVO sr = new SalesRankRowVO();
                sr.setId(toLong(row.get("productId")));
                sr.setName(String.valueOf(row.getOrDefault("productName", "")));
                sr.setAmount(toBigDecimal(row.get("totalSales")));
                sr.setQuantity(toBigDecimal(row.get("totalQuantity")));
                sr.setDeptId(toLong(row.get("deptId")));
                sr.setDeptName(String.valueOf(row.getOrDefault("deptName", "")));
                productRank.add(sr);
            }
        }
        vo.setProductRank(productRank);
        vo.setTrendStats(trendStats);

        List<FinanceWarningVO> warnings = new ArrayList<>();
        BigDecimal prevSales = nullSafe(finSaleRecordMapper.selectMonthTotalSalesForPrev(deptIds));
        if (prevSales.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal changeRate = totalSales.subtract(prevSales).multiply(new BigDecimal("100")).divide(prevSales, 2, java.math.RoundingMode.HALF_UP);
            if (changeRate.compareTo(new BigDecimal("-20")) < 0) {
                FinanceWarningVO w = new FinanceWarningVO();
                w.setWarningType("SALES_DROP");
                w.setSeverity("HIGH");
                w.setTitle("销售下滑预警");
                w.setMessage("销售额较上期下降 " + changeRate.abs() + "%");
                warnings.add(w);
            }
        }
        vo.setWarnings(warnings);
        return vo;
    }

    @Override
    public ProfitShareSettlementDashboardVO getProfitShareSettlement(ReportQueryParams params) {
        ProfitShareSettlementDashboardVO vo = new ProfitShareSettlementDashboardVO();
        applyDataScope(params);
        List<Long> deptIds = params.getDeptIds();

        List<Map<String, Object>> settlementRows = finProfitShareRecordMapper.selectSettlementByDept(deptIds, params.getStartTime(), params.getEndTime());
        BigDecimal payable = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal managerTotal = BigDecimal.ZERO;
        BigDecimal investorTotal = BigDecimal.ZERO;
        if (settlementRows != null) {
            for (Map<String, Object> row : settlementRows) {
                payable = payable.add(toBigDecimal(row.get("payableAmount")));
                paid = paid.add(toBigDecimal(row.get("paidAmount")));
                managerTotal = managerTotal.add(toBigDecimal(row.get("managerShare")));
                investorTotal = investorTotal.add(toBigDecimal(row.get("investorShare")));
            }
        }
        vo.setPayableAmount(payable);
        vo.setPaidAmount(paid);
        vo.setPendingAmount(payable.subtract(paid));
        vo.setManagerShare(managerTotal);
        vo.setInvestorShare(investorTotal);

        Map<String, Object> queryParams = buildQueryParams(params);
        List<Map<String, Object>> salesTrend = finSaleRecordMapper.selectSaleTrendStats(deptIds, params.getStartTime(), params.getEndTime());
        BigDecimal salesSum = sumField(salesTrend, "totalSales");
        BigDecimal expenseSum = nullSafe(finExpenseMapper.selectExpenseTotal(queryParams));
        vo.setTotalSales(salesSum);
        vo.setTotalExpense(expenseSum.abs());
        vo.setNetProfit(salesSum.subtract(expenseSum.abs()));

        vo.setDeptSettlementRows(settlementRows != null ? settlementRows : Collections.emptyList());

        List<ProfitShareExceptionVO> exceptions = new ArrayList<>();
        if (vo.getNetProfit().compareTo(BigDecimal.ZERO) < 0) {
            ProfitShareExceptionVO ex = new ProfitShareExceptionVO();
            ex.setExceptionType("NEGATIVE_PROFIT");
            ex.setMessage("当前期间净利润为负");
            ex.setActualAmount(vo.getNetProfit());
            exceptions.add(ex);
        }
        vo.setExceptions(exceptions);
        return vo;
    }

    @Override
    public List<FinanceAlertVO> getAlerts(ReportQueryParams params) {
        applyDataScope(params);
        List<Long> deptIds = params.getDeptIds();

        // Build diagnosis context from mapper data
        FinanceDiagnosisContext ctx = buildDiagnosisContext(deptIds, params);

        // Run the rule engine
        List<FinanceDiagnosisResult> results = diagnosisEngine.runAll(ctx);

        // Convert results to FinanceAlertVO (preserving frontend contract)
        List<FinanceAlertVO> alerts = new ArrayList<>();
        int seq = 1;
        for (FinanceDiagnosisResult r : results) {
            FinanceAlertVO a = new FinanceAlertVO();
            a.setAlertId("ALERT-" + (seq++));
            a.setAlertLevel(r.getAlertLevel());
            a.setAlertType(r.getRuleId());
            a.setDeptId(r.getDeptId());
            a.setDeptName(r.getDeptName());
            a.setTitle(r.getTitle());
            a.setReason(r.getReason());
            a.setMetricValue(r.getMetricValue());
            a.setCompareValue(r.getCompareValue());
            a.setImpactAmount(r.getImpactAmount());
            a.setSuggestedAction(r.getSuggestedAction());
            a.setTargetRoute(r.getTargetRoute());
            a.setTargetParams(r.getTargetParams());
            a.setOccurTime(r.getOccurTime() != null ? r.getOccurTime() : new java.util.Date());
            alerts.add(a);
        }
        return alerts;
    }

    /**
     * Build a {@link FinanceDiagnosisContext} by querying all required mapper data.
     */
    private FinanceDiagnosisContext buildDiagnosisContext(List<Long> deptIds, ReportQueryParams params) {
        FinanceDiagnosisContext ctx = new FinanceDiagnosisContext();

        // Sales metrics
        ctx.setMonthSales(nullSafe(finSaleRecordMapper.selectMonthTotalSales(deptIds)));
        ctx.setPrevMonthSales(nullSafe(finSaleRecordMapper.selectMonthTotalSalesForPrev(deptIds)));

        // Expense metrics
        ctx.setMonthExpense(nullSafe(finExpenseMapper.selectMonthTotalExpense(deptIds)));
        ctx.setPrevMonthExpense(nullSafe(finExpenseMapper.selectMonthTotalExpenseForPrev(deptIds)));

        // Derived profit metrics
        BigDecimal netProfit = ctx.getMonthSales().subtract(ctx.getMonthExpense());
        ctx.setNetProfit(netProfit);
        ctx.setProfitRate(ctx.getMonthSales().compareTo(BigDecimal.ZERO) > 0
                ? netProfit.multiply(new BigDecimal("100")).divide(ctx.getMonthSales(), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // Verification metrics
        ctx.setUnverifiedExpenseCount(finExpenseMapper.countUnverifiedExpenses(deptIds));
        ctx.setUnverifiedExpenseAmount(nullSafe(finExpenseMapper.sumUnverifiedExpenseAmount(deptIds)));

        // Profit share metrics
        ctx.setUnsettledProfitShareCount(finProfitShareRecordMapper.countUnsettledRecords(deptIds));

        // Member metrics
        BigDecimal memberSales = nullSafe(finSaleRecordMapper.selectMemberSales(deptIds, params.getStartTime(), params.getEndTime()));
        ctx.setMemberSales(memberSales);
        if (ctx.getMonthSales().compareTo(BigDecimal.ZERO) > 0) {
            ctx.setMemberSalesRatio(memberSales.multiply(new BigDecimal("100"))
                    .divide(ctx.getMonthSales(), 2, java.math.RoundingMode.HALF_UP));
        }

        return ctx;
    }

    @Override
    public List<FinanceReviewTaskVO> getReviewTasks(ReportQueryParams params) {
        List<FinanceAlertVO> alerts = this.getAlerts(params);
        List<FinanceReviewTaskVO> tasks = new ArrayList<>();

        for (FinanceAlertVO alert : alerts) {
            FinanceReviewTaskVO task = new FinanceReviewTaskVO();
            task.setTaskId(alert.getAlertId());
            task.setTaskType(alert.getAlertType());
            task.setTaskTitle(alert.getTitle());
            task.setPriority(alert.getAlertLevel());
            task.setDeptId(alert.getDeptId());
            task.setDeptName(alert.getDeptName());
            task.setReason(alert.getReason());
            task.setMetricValue(alert.getMetricValue());
            task.setCompareValue(alert.getCompareValue());
            task.setImpactAmount(alert.getImpactAmount());
            task.setSuggestedAction(alert.getSuggestedAction());
            task.setTargetRoute(alert.getTargetRoute());
            task.setTargetParams(alert.getTargetParams());
            tasks.add(task);
        }

        if (tasks.isEmpty()) {
            FinanceReviewTaskVO healthy = new FinanceReviewTaskVO();
            healthy.setTaskId("HEALTHY");
            healthy.setTaskTitle("经营健康");
            healthy.setReason("当前无经营异常，继续保持");
            healthy.setPriority("INFO");
            healthy.setSuggestedAction("可查看各门店经营详情");
            tasks.add(healthy);
        }

        // 按优先级排序：HIGH > MEDIUM > LOW > INFO，同级别按影响金额降序
        tasks.sort(Comparator.<FinanceReviewTaskVO, Integer>comparing(
                t -> {
                    switch (t.getPriority()) {
                        case "HIGH": return 0;
                        case "MEDIUM": return 1;
                        case "LOW": return 2;
                        default: return 3;
                    }
                })
                .thenComparing(Comparator.comparing(
                        (FinanceReviewTaskVO t) -> t.getImpactAmount() != null ? t.getImpactAmount() : BigDecimal.ZERO)
                        .reversed()));
        return tasks;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal nzBig(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        try { return Long.valueOf(String.valueOf(value)); } catch (Exception e) { return null; }
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return 0; }
    }

    private List<FinanceStoreRankRowVO> buildStoreRankRows(List<Map<String, Object>> rows, String field, int limit) {
        List<FinanceStoreRankRowVO> result = new ArrayList<>();
        if (rows == null) return result;
        for (int i = 0; i < Math.min(rows.size(), limit); i++) {
            Map<String, Object> row = rows.get(i);
            FinanceStoreRankRowVO r = new FinanceStoreRankRowVO();
            r.setDeptId(toLong(row.get("deptId")));
            r.setDeptName(String.valueOf(row.getOrDefault("deptName", "")));
            r.setAmount(toBigDecimal(row.get(field)));
            result.add(r);
        }
        return result;
    }

    private List<Map<String, Object>> buildCostTrendStats(List<Map<String, Object>> expenseTrendStats) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : expenseTrendStats) {
            Map<String, Object> row = new HashMap<>();
            row.put("dateStr", item.get("dateStr"));
            row.put("deptId", item.get("deptId"));
            row.put("deptName", item.get("deptName"));
            row.put("totalAmount", toBigDecimal(item.get("expenseAmount")));
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> buildProfitTrendStats(List<Map<String, Object>> salesTrendStats, List<Map<String, Object>> costTrendStats) {
        Map<String, Map<String, Object>> resultMap = new HashMap<>();
        for (Map<String, Object> item : salesTrendStats) {
            String key = buildTrendKey(item);
            Map<String, Object> row = resultMap.computeIfAbsent(key, k -> buildTrendRow(item));
            row.put("profit", toBigDecimal(row.get("profit")).add(toBigDecimal(item.get("totalSales"))));
        }
        for (Map<String, Object> item : costTrendStats) {
            String key = buildTrendKey(item);
            Map<String, Object> row = resultMap.computeIfAbsent(key, k -> buildTrendRow(item));
            row.put("profit", toBigDecimal(row.get("profit")).subtract(toBigDecimal(item.get("totalAmount"))));
        }
        return resultMap.values().stream()
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("dateStr")) + String.valueOf(item.get("deptId"))))
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildTrendRow(Map<String, Object> source) {
        Map<String, Object> row = new HashMap<>();
        row.put("dateStr", source.get("dateStr"));
        row.put("deptId", source.get("deptId"));
        row.put("deptName", source.get("deptName"));
        row.put("profit", BigDecimal.ZERO);
        return row;
    }

    private String buildTrendKey(Map<String, Object> item) {
        return String.valueOf(item.get("dateStr")) + "_" + String.valueOf(item.get("deptId"));
    }

    private List<Map<String, Object>> buildDeptTotalStats(List<Map<String, Object>> rows, String fieldName) {
        Map<String, Map<String, Object>> resultMap = new LinkedHashMap<>();
        for (Map<String, Object> item : rows) {
            String deptId = String.valueOf(item.get("deptId"));
            Map<String, Object> row = resultMap.computeIfAbsent(deptId, key -> {
                Map<String, Object> value = new HashMap<>();
                value.put("deptId", item.get("deptId"));
                value.put("deptName", item.get("deptName"));
                value.put(fieldName, BigDecimal.ZERO);
                return value;
            });
            row.put(fieldName, toBigDecimal(row.get(fieldName)).add(toBigDecimal(item.get(fieldName))));
        }
        return new ArrayList<>(resultMap.values());
    }

    private BigDecimal sumField(List<Map<String, Object>> rows, String fieldName) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            total = total.add(toBigDecimal(row.get(fieldName)));
        }
        return total;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(String.valueOf(value));
    }

    /**
     * 哨兵部门 ID：非 admin 且无任何授权部门时使用，
     * 保证 Mapper 的 IN (-1) 永远匹配不到真实数据，避免全量泄露。
     */
    private static final List<Long> SENTINEL_DEPT_IDS = Collections.singletonList(-1L);

    private Map<String, Object> buildQueryParams(ReportQueryParams params) {
        applyDataScope(params);
        Map<String, Object> map = new HashMap<>();
        // 始终传递 deptIds，确保 Mapper 能生成部门过滤条件（即使为空也会走 IN (-1) 逻辑）
        map.put("deptIds", params.getDeptIds());
        if (params.getStartTime() != null) {
            map.put("startTime", params.getStartTime());
        }
        if (params.getEndTime() != null) {
            map.put("endTime", params.getEndTime());
        }
        return map;
    }

    private void applyDataScope(ReportQueryParams params) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            Long currentDeptId = SecurityUtils.getDeptId();
            allowed = currentDeptId != null ? Collections.singletonList(currentDeptId) : SENTINEL_DEPT_IDS;
        }
        List<Long> requested = params.getDeptIds();
        if (requested == null || requested.isEmpty()) {
            params.setDeptIds(new ArrayList<>(allowed));
            return;
        }
        List<Long> finalAllowed = allowed;
        List<Long> filtered = requested.stream()
                .filter(finalAllowed::contains)
                .collect(Collectors.toList());
        params.setDeptIds(filtered.isEmpty() ? new ArrayList<>(allowed) : filtered);
    }

    private List<Long> loadAllowedDeptIds() {
        String username = SecurityUtils.getUsername();
        if (username == null || username.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            R<List<SysDept>> response = remoteUserService.getUserDeptList(username, SecurityConstants.INNER);
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData().stream()
                    .map(SysDept::getDeptId)
                    .filter(deptId -> deptId != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
