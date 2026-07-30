package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.vo.AuthorizedStorePortfolioVO;
import com.junsong.finance.domain.vo.AuthorizedStoreReportQueryParams;
import com.junsong.finance.domain.vo.AuthorizedStoreRowVO;
import com.junsong.finance.domain.vo.StoreExpenseCategoryVO;
import com.junsong.finance.domain.vo.StoreHealthTrendQueryParams;
import com.junsong.finance.domain.vo.StorePendingItemVO;
import com.junsong.finance.domain.vo.StoreReportQueryParams;
import com.junsong.finance.domain.vo.StoreReviewTaskVO;
import com.junsong.finance.domain.vo.StoreTrendRowVO;
import com.junsong.finance.mapper.StoreFinanceReportMapper;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizedStoreFinanceReportServiceImplTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.remove();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = StoreFinanceReportServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setupNonAdmin(String username, Long deptId) {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName(username);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(2L);
        loginUser.setUsername(username);
        loginUser.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static void setupAdmin() {
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(1L);
        loginUser.setUsername("admin");
        loginUser.setDeptId(100L);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static StoreFinanceReportServiceImpl createService(FakeStoreFinanceReportMapper mapper,
                                                                RemoteUserService remoteUserService) throws Exception {
        StoreFinanceReportServiceImpl service = new StoreFinanceReportServiceImpl();
        setField(service, "storeFinanceReportMapper", mapper);
        setField(service, "remoteUserService", remoteUserService);
        return service;
    }

    private static AuthorizedStoreReportQueryParams makePortfolioParams(List<Long> deptIds) {
        AuthorizedStoreReportQueryParams params = new AuthorizedStoreReportQueryParams();
        params.setDeptIds(deptIds);
        params.setStartTime(new Date());
        params.setEndTime(new Date());
        params.setTimeType("day");
        return params;
    }

    private static AuthorizedStoreRowVO row(Long deptId, String name, String sales, String expense) {
        AuthorizedStoreRowVO row = new AuthorizedStoreRowVO();
        row.setDeptId(deptId);
        row.setDeptName(name);
        row.setTotalSales(new BigDecimal(sales));
        row.setTotalExpense(new BigDecimal(expense));
        row.setSaleCount(10);
        row.setSaleQuantity(20);
        row.setUnverifiedAmount(BigDecimal.ZERO);
        row.setCashInAmount(new BigDecimal(sales)); // 默认实收=销售额
        row.setNetCashflowAmount(new BigDecimal(sales).subtract(new BigDecimal(expense)));
        row.setHighRiskCount(0);
        return row;
    }

    private static Map<String, Object> cashInRow(Long deptId, String amount) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("deptId", deptId);
        m.put("cashInAmount", new BigDecimal(amount));
        return m;
    }

    private static Map<String, Object> verifiedExpenseRow(Long deptId, String amount) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("deptId", deptId);
        m.put("verifiedExpenseAmount", new BigDecimal(amount));
        return m;
    }

    private static Map<String, Object> highRiskRow(Long deptId, int count, String reason) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("deptId", deptId);
        m.put("highRiskCount", count);
        m.put("primaryReason", reason);
        return m;
    }

    private static Map<String, Object> memberSalesRow(Long deptId, String amount) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("deptId", deptId);
        m.put("memberSales", new BigDecimal(amount));
        return m;
    }

    // ── R8: 非 admin 过滤请求门店到授权范围 ──

    @Test
    void getAuthorizedPortfolio_nonAdminFiltersRequestedDeptIdsToAllowedOnly() throws Exception {
        setupNonAdmin("multi-store-user", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.storeRows = List.of(row(100L, "门店A", "1000.00", "300.00"),
                row(200L, "门店B", "800.00", "500.00"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L, 200L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L, 200L, 300L)));

        assertEquals(List.of(100L, 200L), mapper.lastDeptIds);
        assertEquals(List.of(100L, 200L), vo.getSelectedDeptIds());
        assertEquals(2, vo.getStoreCount());
    }

    // ── R8: 请求空 deptIds 时使用全部授权门店 ──

    @Test
    void getAuthorizedPortfolio_emptyRequest_usesAllAllowedDepts() throws Exception {
        setupNonAdmin("multi-store-user", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.storeRows = List.of(row(100L, "门店A", "1000.00", "300.00"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L, 200L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(makePortfolioParams(null));

        assertEquals(List.of(100L, 200L), vo.getSelectedDeptIds());
    }

    // ── R8: 无可查看授权门店时抛异常 ──

    @Test
    void getAuthorizedPortfolio_noAllowedDepts_throws() throws Exception {
        // 非 admin，SecurityUtils.getDeptId() 为 null
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("no-store-user");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(2L);
        loginUser.setUsername("no-store-user");
        // deptId 为 null
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);

        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(Collections.emptyList()));

        AuthorizedStoreReportQueryParams params = new AuthorizedStoreReportQueryParams();
        params.setDeptIds(List.of(100L));
        params.setTimeType("day");
        assertThrows(ServiceException.class, () -> service.getAuthorizedPortfolio(params));
    }

    // ── R9: 健康度评分和复盘任务 ──

    @Test
    void getAuthorizedPortfolio_buildsHealthScoreAndReviewTasks() throws Exception {
        setupNonAdmin("multi-store-user", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.storeRows = List.of(
                row(100L, "利润为负门店", "1000.00", "1600.00"),
                row(200L, "健康门店", "3000.00", "500.00")
        );
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L, 200L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L, 200L)));

        AuthorizedStoreRowVO risk = vo.getStores().stream()
                .filter(s -> s.getDeptId().equals(100L)).findFirst().orElseThrow();
        assertEquals("RISK", risk.getHealthLevel());
        assertTrue(risk.getHealthScore() < 60);
        assertTrue(vo.getReviewTasks().stream().anyMatch(t -> "NEGATIVE_PROFIT".equals(t.getTaskType())));

        AuthorizedStoreRowVO healthy = vo.getStores().stream()
                .filter(s -> s.getDeptId().equals(200L)).findFirst().orElseThrow();
        assertEquals("GOOD", healthy.getHealthLevel());
        assertTrue(healthy.getHealthScore() >= 80);
    }

    // ── R9: Portfolio 建议 ──

    @Test
    void getAuthorizedPortfolio_riskStores_generatesSuggestion() throws Exception {
        setupNonAdmin("multi-store-user", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.storeRows = List.of(row(100L, "风险门店", "500.00", "2000.00"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        assertNotNull(vo.getSuggestions());
        assertTrue(vo.getSuggestions().stream().anyMatch(s -> s.contains("风险门店")));
    }

    @Test
    void getAuthorizedPortfolio_allHealthy_generatesPositiveSuggestion() throws Exception {
        setupNonAdmin("multi-store-user", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.storeRows = List.of(row(100L, "优秀门店", "5000.00", "500.00"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        assertNotNull(vo.getSuggestions());
        assertTrue(vo.getSuggestions().stream().anyMatch(s -> s.contains("稳定")));
    }

    // ── FIN-G1: 5 维度健康分拆解 ──

    @Test
    void getAuthorizedPortfolio_healthBreakdown_negativeProfit() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.storeRows = List.of(row(100L, "亏损门店", "1000.00", "1500.00"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        AuthorizedStoreRowVO store = vo.getStores().get(0);
        assertNotNull(store.getHealthBreakdown(), "healthBreakdown 不应为 null");
        assertTrue(store.getHealthBreakdown().get("profitMargin") < 0,
            "利润为负时 profitMargin 维度应扣分");
        assertTrue(store.getReviewReasons().contains("NEGATIVE_PROFIT"));
    }

    @Test
    void getAuthorizedPortfolio_healthBreakdown_expenseSpike() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        // 费用 > 70% 销售额 → expenseControl 扣 15 分
        mapper.storeRows = List.of(row(100L, "费用高门店", "1000.00", "800.00"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        AuthorizedStoreRowVO store = vo.getStores().get(0);
        assertEquals(-15, store.getHealthBreakdown().get("expenseControl"),
            "费用超过销售额 70% 时 expenseControl 应扣 15 分");
        assertTrue(store.getReviewReasons().contains("EXPENSE_SPIKE"));
    }

    @Test
    void getAuthorizedPortfolio_healthBreakdown_unverifiedHigh() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        AuthorizedStoreRowVO r = row(100L, "未核销门店", "5000.00", "500.00");
        r.setUnverifiedAmount(new BigDecimal("2000.00"));
        mapper.storeRows = List.of(r);
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        AuthorizedStoreRowVO store = vo.getStores().get(0);
        assertEquals(-15, store.getHealthBreakdown().get("verificationTimeliness"),
            "未核销金额超过 1000 时 verificationTimeliness 应扣 15 分");
        assertTrue(store.getReviewReasons().contains("UNVERIFIED_HIGH"));
    }

    @Test
    void getAuthorizedPortfolio_healthBreakdown_memberContributionLow() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.storeRows = List.of(row(100L, "会员低门店", "5000.00", "500.00"));
        // 会员销售仅 100/5000 = 2% < 20%
        List<Map<String, Object>> memberSales = new java.util.ArrayList<>();
        Map<String, Object> ms = new java.util.HashMap<>();
        ms.put("deptId", 100L);
        ms.put("memberSales", new BigDecimal("100.00"));
        memberSales.add(ms);
        mapper.memberSalesRows = memberSales;
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        AuthorizedStoreRowVO store = vo.getStores().get(0);
        assertEquals(-10, store.getHealthBreakdown().get("memberContribution"),
            "会员销售占比低于 20% 时 memberContribution 应扣 10 分");
        assertTrue(store.getReviewReasons().contains("MEMBER_LOW"));
        assertNotNull(store.getMemberSalesRatio());
        assertTrue(store.getMemberSalesRatio().compareTo(new BigDecimal("20")) < 0);
    }

    @Test
    void getAuthorizedPortfolio_portfolioAverageMetrics() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.storeRows = List.of(
                row(100L, "门店A", "3000.00", "1000.00"),
                row(200L, "门店B", "5000.00", "2000.00")
        );
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L, 200L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L, 200L)));

        // totalSales = 8000, avgSales = 4000
        assertEquals(new BigDecimal("4000.00"), vo.getAvgSales());
        // totalExpense = 3000, avgExpense = 1500
        assertEquals(new BigDecimal("1500.00"), vo.getAvgExpense());
        // totalProfit = 5000, avgProfit = 2500
        assertEquals(new BigDecimal("2500.00"), vo.getAvgProfit());
    }

    @Test
    void getAuthorizedPortfolio_reviewTaskCountPerStore() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.storeRows = List.of(
                row(100L, "多问题门店", "1000.00", "1500.00"),
                row(200L, "健康门店", "5000.00", "500.00")
        );
        // 为健康门店提供足够的会员销售数据（2000/5000 = 40% > 20%）
        List<Map<String, Object>> memberSales = new java.util.ArrayList<>();
        Map<String, Object> ms = new java.util.HashMap<>();
        ms.put("deptId", 200L);
        ms.put("memberSales", new BigDecimal("2000.00"));
        memberSales.add(ms);
        mapper.memberSalesRows = memberSales;

        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L, 200L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L, 200L)));

        AuthorizedStoreRowVO risk = vo.getStores().stream()
                .filter(s -> s.getDeptId().equals(100L)).findFirst().orElseThrow();
        assertTrue(risk.getReviewTaskCount() > 0, "风险门店应有复盘任务");
        assertTrue(risk.getAlertCount() > 0, "风险门店应有预警");

        AuthorizedStoreRowVO healthy = vo.getStores().stream()
                .filter(s -> s.getDeptId().equals(200L)).findFirst().orElseThrow();
        assertEquals(0, healthy.getReviewTaskCount(), "健康门店复盘任务数应为 0");
    }

    // ── R8-C: cashInAmount 使用 payment_date 口径 ──

    @Test
    void getAuthorizedPortfolio_cashInAmountUsesPaymentDate() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.storeRows = List.of(row(100L, "门店A", "5000.00", "1000.00"));
        // 实收现金 8000 来自 fin_sale_payment.payment_date
        mapper.cashInRows = List.of(cashInRow(100L, "8000.00"));
        mapper.verifiedExpenseRows = List.of(verifiedExpenseRow(100L, "1000.00"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        AuthorizedStoreRowVO store = vo.getStores().get(0);
        assertEquals(new BigDecimal("8000.00"), store.getCashInAmount(),
                "cashInAmount 应来自 fin_sale_payment.payment_date 而非 sale_amount");
    }

    // ── R8-C: netCashflowAmount = cashInAmount - verifiedExpense ──

    @Test
    void getAuthorizedPortfolio_netCashflowSubtractsVerifiedExpense() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.storeRows = List.of(row(100L, "门店A", "5000.00", "1000.00"));
        mapper.cashInRows = List.of(cashInRow(100L, "8000.00"));
        mapper.verifiedExpenseRows = List.of(verifiedExpenseRow(100L, "3000.00"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        AuthorizedStoreRowVO store = vo.getStores().get(0);
        assertEquals(new BigDecimal("5000.00"), store.getNetCashflowAmount(),
                "netCashflowAmount = 8000(实收) - 3000(已核销费用) = 5000");
    }

    // ── R8-C: HIGH 风险门店排在健康门店之前 ──

    @Test
    void getAuthorizedPortfolio_highRiskStoreRanksBeforeHealthy() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.storeRows = List.of(
                row(100L, "健康门店", "5000.00", "500.00"),
                row(200L, "高风险门店", "3000.00", "800.00")
        );
        // 门店 200 有 HIGH 复盘任务
        mapper.highRiskRows = List.of(highRiskRow(200L, 3, "费用超标"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L, 200L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L, 200L)));

        // 高风险门店应排在第一位
        assertEquals(200L, vo.getStores().get(0).getDeptId(),
                "HIGH 风险门店应排在健康门店之前");
        assertEquals(3, vo.getStores().get(0).getHighRiskCount());
        assertEquals(100L, vo.getStores().get(1).getDeptId());
        assertEquals(0, vo.getStores().get(1).getHighRiskCount());
    }

    // ── R8-C: primaryRisk 优先使用复盘任务原因 ──

    @Test
    void getAuthorizedPortfolio_primaryRiskChoosesReviewTaskReasonFirst() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        // 门店有负现金流，但同时有 HIGH 复盘任务 → primaryRisk 应为复盘任务原因
        mapper.storeRows = List.of(row(100L, "门店A", "1000.00", "2000.00"));
        mapper.cashInRows = List.of(cashInRow(100L, "500.00"));
        mapper.verifiedExpenseRows = List.of(verifiedExpenseRow(100L, "2000.00")); // 净现金流 = -1500
        mapper.highRiskRows = List.of(highRiskRow(100L, 2, "未核销费用超过阈值"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        AuthorizedStoreRowVO store = vo.getStores().get(0);
        assertEquals("未核销费用超过阈值", store.getPrimaryRisk(),
                "primaryRisk 应优先使用复盘任务原因，而非现金流为负");
    }

    // ── R8-C: 无数据门店返回零值且 nextAction = 继续观察 ──

    @Test
    void getAuthorizedPortfolio_noDataStoreReturnsZeroAndContinueObservation() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        // 门店有销售和费用数据（为健康门店），无 HIGH 风险，无现金流问题
        mapper.storeRows = List.of(row(100L, "健康门店", "5000.00", "500.00"));
        mapper.cashInRows = List.of(cashInRow(100L, "5000.00"));
        mapper.verifiedExpenseRows = List.of(verifiedExpenseRow(100L, "500.00"));
        // 无 highRiskRows → highRiskCount = 0
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        AuthorizedStoreRowVO store = vo.getStores().get(0);
        assertEquals(0, store.getHighRiskCount(), "无风险门店 highRiskCount 应为 0");
        assertNull(store.getPrimaryRisk(), "健康门店 primaryRisk 应为 null");
        assertEquals("继续观察", store.getNextAction(),
                "无风险门店 nextAction 应为 '继续观察'");
        assertEquals(new BigDecimal("4500.00"), store.getNetCashflowAmount(),
                "净现金流 = 5000 - 500 = 4500");
    }

    // ── R11-B: 健康分 V2 因子列表 ──

    @Test
    void getAuthorizedPortfolio_healthV2_factorsListed() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        // 亏损门店: profit=-600, profitRate=-60%
        AuthorizedStoreRowVO r = row(100L, "亏损门店", "1000.00", "1600.00");
        mapper.storeRows = List.of(r);
        // cashIn=500, verifiedExpense=1100 → netCashflow = -600
        mapper.cashInRows = List.of(cashInRow(100L, "500"));
        mapper.verifiedExpenseRows = List.of(verifiedExpenseRow(100L, "1100"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        AuthorizedStoreRowVO store = vo.getStores().get(0);
        assertEquals("R11_V2", store.getHealthScoreVersion());
        assertNotNull(store.getHealthFactors(), "healthFactors 不应为 null");
        assertTrue(store.getHealthFactors().size() >= 2,
                "亏损且现金流为负应至少有 2 个扣分因子");
        assertTrue(store.getHealthFactors().stream().anyMatch(f -> "STORE_PROFIT_RATE_LOW".equals(f.getFactorCode())));
        assertTrue(store.getHealthFactors().stream().anyMatch(f -> "STORE_CASHFLOW_NEGATIVE".equals(f.getFactorCode())));
        // HIGH 排在 MEDIUM 前面
        if (store.getHealthFactors().size() >= 2) {
            String first = store.getHealthFactors().get(0).getSeverity();
            assertEquals("HIGH", first, "HIGH 严重度因子应排在最前");
        }
    }

    @Test
    void getAuthorizedPortfolio_healthV2_healthyStore() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        // 健康门店: sales=5000, expense=1000, profitRate=80%, netCashflow=4000
        mapper.storeRows = List.of(row(100L, "优秀门店", "5000.00", "1000.00"));
        // 提供足够会员销售: 2000/5000 = 40% > 20%
        mapper.memberSalesRows = List.of(memberSalesRow(100L, "2000.00"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        AuthorizedStoreRowVO store = vo.getStores().get(0);
        assertEquals(100, store.getHealthScore());
        assertEquals("GOOD", store.getHealthLevel());
        assertEquals("R11_V2", store.getHealthScoreVersion());
        assertTrue(store.getHealthFactors().isEmpty(), "健康门店应无扣分因子");
        assertNotNull(store.getHealthSummary());
        assertTrue(store.getHealthSummary().contains("良好"));
    }

    @Test
    void getAuthorizedPortfolio_healthV2_levelThresholds() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        // 门店: sales=5000, expense=4000, profitRate=20% (above 5% threshold)
        // expenseRate=80% (above 35%) → -15
        // memberRatio: provide 1500/5000=30% > 20% → no deduction
        // netCashflow: row sets to 5000-4000=1000, but portfolio flow overrides with cashIn-verifiedExpense
        mapper.storeRows = List.of(row(100L, "边界门店", "5000.00", "4000.00"));
        mapper.memberSalesRows = List.of(memberSalesRow(100L, "1500.00"));
        mapper.cashInRows = List.of(cashInRow(100L, "5000"));
        mapper.verifiedExpenseRows = List.of(verifiedExpenseRow(100L, "4000"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        AuthorizedStoreRowVO store = vo.getStores().get(0);
        // score = 100 - 15 (expenseRate) = 85 → GOOD
        assertEquals("GOOD", store.getHealthLevel(), "score=85 应为 GOOD");
    }

    @Test
    void getAuthorizedPortfolio_healthV2_authorizedAverageComparison() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.storeRows = List.of(
                row(100L, "门店A", "3000.00", "1000.00"),
                row(200L, "门店B", "5000.00", "2000.00")
        );
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L, 200L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L, 200L)));

        AuthorizedStoreRowVO storeA = vo.getStores().stream()
                .filter(s -> s.getDeptId().equals(100L)).findFirst().orElseThrow();
        // avgSales = 4000, storeA sales = 3000 → salesVsAvg = (3000-4000)/4000*100 = -25%
        assertNotNull(storeA.getAuthorizedAverageSales());
        assertEquals(0, new BigDecimal("4000.00").compareTo(storeA.getAuthorizedAverageSales()));
        assertNotNull(storeA.getSalesVsAuthorizedAverageRate());
        assertTrue(storeA.getSalesVsAuthorizedAverageRate().compareTo(BigDecimal.ZERO) < 0,
                "门店A销售低于均值应为负值");
        assertNotNull(storeA.getProfitRateVsAuthorizedAverage());
    }

    @Test
    void getAuthorizedPortfolio_healthV2_salesDropFactor() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        AuthorizedStoreRowVO r = row(100L, "下滑门店", "5000.00", "1000.00");
        r.setSalesChangeRate(new BigDecimal("-25")); // 下降25%，超过-20%阈值
        mapper.storeRows = List.of(r);
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        AuthorizedStoreRowVO store = vo.getStores().get(0);
        assertTrue(store.getHealthFactors().stream().anyMatch(f -> "STORE_SALES_DROP_RATE".equals(f.getFactorCode())),
                "销售下滑超过阈值应生成 STORE_SALES_DROP_RATE 因子");
    }

    @Test
    void getAuthorizedPortfolio_healthV2_pendingAmountFactor() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        AuthorizedStoreRowVO r = row(100L, "未核销门店", "5000.00", "1000.00");
        r.setUnverifiedAmount(new BigDecimal("2000")); // 超过1000阈值
        mapper.storeRows = List.of(r);
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        AuthorizedStorePortfolioVO vo = service.getAuthorizedPortfolio(
                makePortfolioParams(List.of(100L)));

        AuthorizedStoreRowVO store = vo.getStores().get(0);
        assertTrue(store.getHealthFactors().stream().anyMatch(f -> "STORE_PENDING_AMOUNT_HIGH".equals(f.getFactorCode())),
                "未核销金额超过阈值应生成 STORE_PENDING_AMOUNT_HIGH 因子");
    }

    // ── R11-C: 健康分趋势 ──

    @Test
    void getAuthorizedHealthTrend_nonAdminUnauthorizedDept_throws() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        StoreHealthTrendQueryParams params = new StoreHealthTrendQueryParams();
        params.setDeptIds(List.of(999L));
        params.setStartTime(new Date());
        params.setEndTime(new Date());
        assertThrows(ServiceException.class, () -> service.getAuthorizedHealthTrend(params));
    }

    @Test
    void getAuthorizedHealthTrend_invalidTimeType_defaultsToWeek() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.healthTrendRows = List.of(trendRow(100L, "门店A", "2026-W26", "5000", "1000"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        StoreHealthTrendQueryParams params = new StoreHealthTrendQueryParams();
        params.setDeptIds(List.of(100L));
        params.setStartTime(new Date());
        params.setEndTime(new Date());
        params.setTimeType("invalid");
        var result = service.getAuthorizedHealthTrend(params);
        assertFalse(result.isEmpty());
        assertNotNull(result.get(0).getHealthScore());
        assertNotNull(result.get(0).getHealthLevel());
    }

    @Test
    void getAuthorizedHealthTrend_emptyDataReturnsEmptyList() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.healthTrendRows = Collections.emptyList();
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        StoreHealthTrendQueryParams params = new StoreHealthTrendQueryParams();
        params.setDeptIds(List.of(100L));
        params.setStartTime(new Date());
        params.setEndTime(new Date());
        var result = service.getAuthorizedHealthTrend(params);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAuthorizedHealthTrend_includesHealthScoreAndLevel() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.healthTrendRows = List.of(
                trendRow(100L, "门店A", "2026-W25", "5000", "1000"),
                trendRow(100L, "门店A", "2026-W26", "3000", "2800")
        );
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        StoreHealthTrendQueryParams params = new StoreHealthTrendQueryParams();
        params.setDeptIds(List.of(100L));
        params.setStartTime(new Date());
        params.setEndTime(new Date());
        params.setTimeType("week");
        var result = service.getAuthorizedHealthTrend(params);
        assertEquals(2, result.size());
        // W25: profitRate=80%, score=100, GOOD
        assertEquals(100, result.get(0).getHealthScore());
        assertEquals("GOOD", result.get(0).getHealthLevel());
        // W26: profitRate=6.67%, score=70, WATCH
        assertTrue(result.get(1).getHealthScore() >= 70);
    }

    private static Map<String, Object> trendRow(Long deptId, String deptName, String periodLabel, String sales, String expense) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("deptId", deptId);
        m.put("deptName", deptName);
        m.put("periodLabel", periodLabel);
        m.put("totalSales", new BigDecimal(sales));
        m.put("totalExpense", new BigDecimal(expense));
        m.put("highRiskCount", 0);
        return m;
    }

    // ── Fake RemoteUserService ──

    static class FakeRemoteUserService implements RemoteUserService {
        @Override public R<Boolean> isWechatLoginEnabled(Long tenantId, String source) { return R.ok(false); }
        private final List<Long> deptIds;

        FakeRemoteUserService(List<Long> deptIds) {
            this.deptIds = deptIds;
        }

        @Override
        public R<LoginUser> getUserInfo(String username, String source) { return null; }

        @Override
        public R<Boolean> registerUserInfo(com.junsong.system.api.domain.SysUser user, String source) { return null; }

        @Override
        public R<Boolean> recordUserLogin(com.junsong.system.api.domain.SysUser user, String source) { return null; }

        @Override
        public R<List<SysDept>> getUserDeptList(String username, String source) {
            List<SysDept> list = deptIds.stream().map(id -> {
                SysDept dept = new SysDept();
                dept.setDeptId(id);
                dept.setDeptName("门店" + id);
                return dept;
            }).collect(Collectors.toList());
            return R.ok(list);
        }

        @Override
        public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) { return null; }
    }

    // ── Fake Mapper ──

    static class FakeStoreFinanceReportMapper implements StoreFinanceReportMapper {
        List<AuthorizedStoreRowVO> storeRows = Collections.emptyList();
        List<Long> lastDeptIds;
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal previousSales = BigDecimal.ZERO;
        BigDecimal previousExpense = BigDecimal.ZERO;

        @Override
        public BigDecimal selectStoreTotalSales(StoreReportQueryParams params) { return totalSales; }

        @Override
        public Integer countStoreSaleRecords(StoreReportQueryParams params) { return 0; }

        @Override
        public Integer sumStoreSaleQuantity(StoreReportQueryParams params) { return 0; }

        @Override
        public BigDecimal selectStoreTotalExpense(StoreReportQueryParams params) { return totalExpense; }

        @Override
        public BigDecimal selectStoreUnverifiedExpense(StoreReportQueryParams params) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectStoreUnverifiedAdvance(StoreReportQueryParams params) { return BigDecimal.ZERO; }

        @Override
        public String selectCurrentAccountingPeriodStatus(Long deptId) { return null; }

        @Override
        public List<StoreTrendRowVO> selectStoreSalesTrend(StoreReportQueryParams params) { return Collections.emptyList(); }

        @Override
        public List<StoreTrendRowVO> selectStoreExpenseTrend(StoreReportQueryParams params) { return Collections.emptyList(); }

        @Override
        public List<StoreExpenseCategoryVO> selectStoreExpenseCategories(StoreReportQueryParams params) { return Collections.emptyList(); }

        @Override
        public List<StorePendingItemVO> selectStoreUnverifiedExpenses(StoreReportQueryParams params) { return Collections.emptyList(); }

        @Override
        public List<StorePendingItemVO> selectStoreUnverifiedAdvances(StoreReportQueryParams params) { return Collections.emptyList(); }

        @Override
        public BigDecimal selectStoreTotalSalesForRange(Long deptId, Date startTime, Date endTime) { return previousSales; }

        @Override
        public BigDecimal selectStoreTotalExpenseForRange(Long deptId, Date startTime, Date endTime) { return previousExpense; }

        @Override
        public List<AuthorizedStoreRowVO> selectAuthorizedStoreRows(List<Long> deptIds, Date startTime, Date endTime) {
            this.lastDeptIds = deptIds;
            return storeRows;
        }

        List<Map<String, Object>> memberSalesRows = Collections.emptyList();

        @Override
        public List<Map<String, Object>> selectMemberSalesByDepts(List<Long> deptIds, Date startTime, Date endTime) {
            return memberSalesRows;
        }

        // R8-C: 新增可行动字段
        List<Map<String, Object>> cashInRows = Collections.emptyList();
        List<Map<String, Object>> verifiedExpenseRows = Collections.emptyList();
        List<Map<String, Object>> highRiskRows = Collections.emptyList();

        @Override
        public List<Map<String, Object>> selectCashInByDepts(List<Long> deptIds, Date startTime, Date endTime) {
            return cashInRows;
        }

        @Override
        public List<Map<String, Object>> selectVerifiedExpenseByDepts(List<Long> deptIds, Date startTime, Date endTime) {
            return verifiedExpenseRows;
        }

        @Override
        public List<Map<String, Object>> selectHighRiskTaskCountByDepts(List<Long> deptIds) {
            return highRiskRows;
        }

        // R11-FIX-C: 按周期统计高风险任务数
        List<Map<String, Object>> highRiskPeriodRows = Collections.emptyList();

        @Override
        public List<Map<String, Object>> selectHighRiskTaskCountByDeptsAndPeriod(List<Long> deptIds, Date startTime, Date endTime, String groupBy) {
            return highRiskPeriodRows;
        }

        // R11-C: 健康趋势
        List<Map<String, Object>> healthTrendRows = Collections.emptyList();

        @Override
        public List<Map<String, Object>> selectHealthTrendByDepts(List<Long> deptIds, Date startTime, Date endTime, String groupBy) {
            return healthTrendRows;
        }
    }
}
