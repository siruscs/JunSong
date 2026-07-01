package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.vo.AuthorizedStorePortfolioVO;
import com.junsong.finance.domain.vo.AuthorizedStoreReportQueryParams;
import com.junsong.finance.domain.vo.AuthorizedStoreRowVO;
import com.junsong.finance.domain.vo.StoreExpenseCategoryVO;
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
        return row;
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

    // ── Fake RemoteUserService ──

    static class FakeRemoteUserService implements RemoteUserService {
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
    }
}
