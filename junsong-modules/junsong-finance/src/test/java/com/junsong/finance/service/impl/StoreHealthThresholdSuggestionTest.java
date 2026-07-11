package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.finance.domain.vo.AuthorizedStoreRowVO;
import com.junsong.finance.domain.vo.HealthRuleThresholdSuggestionVO;
import com.junsong.finance.domain.vo.StoreExpenseCategoryVO;
import com.junsong.finance.domain.vo.StorePendingItemVO;
import com.junsong.finance.domain.vo.StoreReportQueryParams;
import com.junsong.finance.domain.vo.StoreTrendRowVO;
import com.junsong.finance.mapper.StoreFinanceReportMapper;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class StoreHealthThresholdSuggestionTest {

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

    private static StoreFinanceReportServiceImpl createService(FakeMapper mapper,
                                                                RemoteUserService remoteUserService) throws Exception {
        StoreFinanceReportServiceImpl service = new StoreFinanceReportServiceImpl();
        setField(service, "storeFinanceReportMapper", mapper);
        setField(service, "remoteUserService", remoteUserService);
        return service;
    }

    // ── R12-C: 无授权门店返回空列表 ──

    @Test
    void getHealthRuleThresholdSuggestions_noAuthorizedStores_returnsEmpty() throws Exception {
        // Non-admin with no deptId and no authorized depts
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("no-store-user");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(2L);
        loginUser.setUsername("no-store-user");
        // deptId is null
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);

        FakeMapper mapper = new FakeMapper();
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(Collections.emptyList()));

        List<HealthRuleThresholdSuggestionVO> result = service.getHealthRuleThresholdSuggestions(90);

        assertNotNull(result, "结果不应为 null");
        assertTrue(result.isEmpty(), "无授权门店应返回空列表");
    }

    // ── R12-C: 分析天数不足14天返回 INSUFFICIENT_DATA ──

    @Test
    void getHealthRuleThresholdSuggestions_daysLessThan14_returnsInsufficientData() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeMapper mapper = new FakeMapper();
        mapper.storeRows = List.of(makeRow(100L, "门店A", "5000", "1000"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        List<HealthRuleThresholdSuggestionVO> result = service.getHealthRuleThresholdSuggestions(7);

        assertNotNull(result, "结果不应为 null");
        assertFalse(result.isEmpty(), "应返回规则建议列表");
        for (HealthRuleThresholdSuggestionVO vo : result) {
            assertEquals("INSUFFICIENT_DATA", vo.getSuggestionType(),
                    "天数不足14天时所有规则应返回 INSUFFICIENT_DATA");
            assertEquals(7, vo.getSampleDays());
        }
    }

    // ── R12-C: 正常触发率返回 KEEP ──

    @Test
    void getHealthRuleThresholdSuggestions_normalTriggerRate_returnsKeep() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeMapper mapper = new FakeMapper();

        // Create 10 stores: 2 with low profit (trigger PROFIT_MARGIN_LOW), 8 healthy
        List<AuthorizedStoreRowVO> rows = new ArrayList<>();
        // 2 low-profit stores: sales=1000, expense=980 → profitRate=2% < 5%
        rows.add(makeRow(100L, "低利润门店1", "1000", "980"));
        rows.add(makeRow(200L, "低利润门店2", "1000", "980"));
        // 8 healthy stores: sales=5000, expense=500 → profitRate=90%
        for (int i = 3; i <= 10; i++) {
            rows.add(makeRow((long) i * 100, "健康门店" + i, "5000", "500"));
        }
        mapper.storeRows = rows;

        List<Long> deptIds = rows.stream().map(AuthorizedStoreRowVO::getDeptId).collect(Collectors.toList());
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(deptIds));

        List<HealthRuleThresholdSuggestionVO> result = service.getHealthRuleThresholdSuggestions(90);

        assertNotNull(result, "结果不应为 null");
        assertFalse(result.isEmpty(), "应返回规则建议列表");

        // Find the PROFIT_MARGIN_LOW suggestion
        HealthRuleThresholdSuggestionVO profitSuggestion = result.stream()
                .filter(v -> "PROFIT_MARGIN_LOW".equals(v.getRuleCode()))
                .findFirst().orElse(null);
        assertNotNull(profitSuggestion, "应包含 PROFIT_MARGIN_LOW 规则建议");
        // 2/10 = 20% trigger rate → between 5% and 60% → KEEP
        assertEquals("KEEP", profitSuggestion.getSuggestionType(),
                "20% 触发率应返回 KEEP");
        assertEquals(2, profitSuggestion.getAffectedStoreCount(),
                "应有 2 家门店触发 PROFIT_MARGIN_LOW");
        assertEquals(90, profitSuggestion.getSampleDays());
        assertNotNull(profitSuggestion.getCurrentThreshold());
        assertNotNull(profitSuggestion.getP50());
        assertNotNull(profitSuggestion.getP75());
        assertNotNull(profitSuggestion.getP90());
    }

    // ── R12-C: 高触发率返回 RELAX ──

    @Test
    void getHealthRuleThresholdSuggestions_highTriggerRate_returnsRelax() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeMapper mapper = new FakeMapper();

        // 10 stores all with low profit → 100% trigger → RELAX
        List<AuthorizedStoreRowVO> rows = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            rows.add(makeRow((long) i * 100, "低利润门店" + i, "1000", "980"));
        }
        mapper.storeRows = rows;

        List<Long> deptIds = rows.stream().map(AuthorizedStoreRowVO::getDeptId).collect(Collectors.toList());
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(deptIds));

        List<HealthRuleThresholdSuggestionVO> result = service.getHealthRuleThresholdSuggestions(90);

        HealthRuleThresholdSuggestionVO profitSuggestion = result.stream()
                .filter(v -> "PROFIT_MARGIN_LOW".equals(v.getRuleCode()))
                .findFirst().orElse(null);
        assertNotNull(profitSuggestion);
        assertEquals("RELAX", profitSuggestion.getSuggestionType(),
                "100% 触发率应返回 RELAX");
    }

    // ── R12-C: 低触发率返回 TIGHTEN ──

    @Test
    void getHealthRuleThresholdSuggestions_zeroTriggerRate_returnsTighten() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeMapper mapper = new FakeMapper();

        // 20 stores all very healthy → 0% trigger → TIGHTEN (< 5%)
        List<AuthorizedStoreRowVO> rows = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            rows.add(makeRow((long) i * 100, "优秀门店" + i, "10000", "500"));
        }
        mapper.storeRows = rows;

        List<Long> deptIds = rows.stream().map(AuthorizedStoreRowVO::getDeptId).collect(Collectors.toList());
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(deptIds));

        List<HealthRuleThresholdSuggestionVO> result = service.getHealthRuleThresholdSuggestions(90);

        // PROFIT_MARGIN_LOW should be TIGHTEN since 0% < 5%
        HealthRuleThresholdSuggestionVO profitSuggestion = result.stream()
                .filter(v -> "PROFIT_MARGIN_LOW".equals(v.getRuleCode()))
                .findFirst().orElse(null);
        assertNotNull(profitSuggestion);
        assertEquals("TIGHTEN", profitSuggestion.getSuggestionType(),
                "0% 触发率应返回 TIGHTEN");
        assertEquals(0, profitSuggestion.getAffectedStoreCount());
    }

    // ── R12-C: 返回所有 5 个规则 ──

    @Test
    void getHealthRuleThresholdSuggestions_returnsAllFiveRules() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeMapper mapper = new FakeMapper();
        mapper.storeRows = List.of(makeRow(100L, "门店A", "5000", "1000"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        List<HealthRuleThresholdSuggestionVO> result = service.getHealthRuleThresholdSuggestions(90);

        assertNotNull(result);
        assertEquals(5, result.size(), "应返回 5 个规则的建议");

        List<String> ruleCodes = result.stream()
                .map(HealthRuleThresholdSuggestionVO::getRuleCode)
                .collect(Collectors.toList());
        assertTrue(ruleCodes.contains("SALES_DECLINE"));
        assertTrue(ruleCodes.contains("EXPENSE_SURGE"));
        assertTrue(ruleCodes.contains("PROFIT_MARGIN_LOW"));
        assertTrue(ruleCodes.contains("PENDING_TASKS"));
        assertTrue(ruleCodes.contains("STORE_REVIEW_SCORE_LOW"));
    }

    // ── R12-C: null days defaults to 90 ──

    @Test
    void getHealthRuleThresholdSuggestions_nullDays_defaultsTo90() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeMapper mapper = new FakeMapper();
        mapper.storeRows = List.of(makeRow(100L, "门店A", "5000", "1000"));
        StoreFinanceReportServiceImpl service = createService(mapper,
                new FakeRemoteUserService(List.of(100L)));

        List<HealthRuleThresholdSuggestionVO> result = service.getHealthRuleThresholdSuggestions(null);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(90, result.get(0).getSampleDays(), "null days 应默认为 90");
    }

    // ── Helper: build a store row ──

    private static AuthorizedStoreRowVO makeRow(Long deptId, String name, String sales, String expense) {
        AuthorizedStoreRowVO row = new AuthorizedStoreRowVO();
        row.setDeptId(deptId);
        row.setDeptName(name);
        row.setTotalSales(new BigDecimal(sales));
        row.setTotalExpense(new BigDecimal(expense));
        row.setSaleCount(10);
        row.setSaleQuantity(20);
        row.setUnverifiedAmount(BigDecimal.ZERO);
        row.setCashInAmount(new BigDecimal(sales));
        row.setNetCashflowAmount(new BigDecimal(sales).subtract(new BigDecimal(expense)));
        row.setHighRiskCount(0);
        return row;
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

    static class FakeMapper implements StoreFinanceReportMapper {
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

        @Override
        public List<Map<String, Object>> selectHighRiskTaskCountByDeptsAndPeriod(List<Long> deptIds, Date startTime, Date endTime, String groupBy) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> healthTrendRows = Collections.emptyList();

        @Override
        public List<Map<String, Object>> selectHealthTrendByDepts(List<Long> deptIds, Date startTime, Date endTime, String groupBy) {
            return healthTrendRows;
        }
    }
}
