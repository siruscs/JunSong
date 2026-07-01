package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.vo.AuthorizedStoreRowVO;
import com.junsong.finance.domain.vo.StoreExpenseCategoryVO;
import com.junsong.finance.domain.vo.StoreOperationSummaryVO;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class StoreFinanceReportServiceImplTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.remove();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = StoreFinanceReportServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
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

    private static void setupNonAdmin(String username, Long deptId) {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName(username);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(2L);
        loginUser.setUsername(username);
        loginUser.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static StoreFinanceReportServiceImpl createService(FakeStoreFinanceReportMapper mapper) throws Exception {
        StoreFinanceReportServiceImpl service = new StoreFinanceReportServiceImpl();
        setField(service, "storeFinanceReportMapper", mapper);
        setField(service, "remoteUserService", new FakeRemoteUserService(Collections.emptyList()));
        return service;
    }

    private static StoreFinanceReportServiceImpl createService(FakeStoreFinanceReportMapper mapper, RemoteUserService remoteUserService) throws Exception {
        StoreFinanceReportServiceImpl service = new StoreFinanceReportServiceImpl();
        setField(service, "storeFinanceReportMapper", mapper);
        setField(service, "remoteUserService", remoteUserService);
        return service;
    }

    private static StoreReportQueryParams makeParams(Long deptId) {
        StoreReportQueryParams params = new StoreReportQueryParams();
        params.setDeptId(deptId);
        params.setStartTime(new Date());
        params.setEndTime(new Date());
        params.setTimeType("day");
        return params;
    }

    // ── R5: 非 admin 可查授权门店（第二个门店） ──

    @Test
    void getSummary_nonAdminAssignedSecondDept_returnsVO() throws Exception {
        setupNonAdmin("multi-store-user", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = new BigDecimal("800.00");
        StoreFinanceReportServiceImpl service = createService(mapper,
            new FakeRemoteUserService(List.of(100L, 200L)));

        StoreOperationSummaryVO vo = service.getSummary(makeParams(200L));

        assertEquals(200L, vo.getDeptId());
        assertEquals(new BigDecimal("800.00"), vo.getTotalSales());
    }

    // ── R5: timeType 规范化 ──

    @Test
    void getSummary_invalidTimeType_defaultsToDay() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreReportQueryParams params = makeParams(100L);
        params.setTimeType("quarter");
        service.getSummary(params);

        assertEquals("day", mapper.lastParams.getTimeType());
    }

    @Test
    void getSummary_nullTimeType_defaultsToDay() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreReportQueryParams params = makeParams(100L);
        params.setTimeType(null);
        service.getSummary(params);

        assertEquals("day", mapper.lastParams.getTimeType());
    }

    @Test
    void getSummary_validTimeType_preserved() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreReportQueryParams params = makeParams(100L);
        params.setTimeType("month");
        service.getSummary(params);

        assertEquals("month", mapper.lastParams.getTimeType());
    }

    // ── R5: 非 admin 请求未授权门店拒绝 ──

    @Test
    void getSummary_nonAdminUnassignedDept_throwsServiceException() throws Exception {
        setupNonAdmin("store-user", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        StoreFinanceReportServiceImpl service = createService(mapper,
            new FakeRemoteUserService(List.of(100L)));

        StoreReportQueryParams params = makeParams(999L);

        assertThrows(ServiceException.class, () -> service.getSummary(params),
            "非 admin 请求未授权门店应拒绝");
    }

    // ── deptId 为空时抛业务异常 ──

    @Test
    void getSummary_deptIdNull_throwsServiceException() throws Exception {
        setupAdmin();
        StoreFinanceReportServiceImpl service = createService(new FakeStoreFinanceReportMapper());

        StoreReportQueryParams params = new StoreReportQueryParams();

        assertThrows(ServiceException.class, () -> service.getSummary(params),
            "deptId 为空时应抛出 ServiceException");
    }

    // ── 非 admin 无授权门店时拒绝 ──

    @Test
    void getSummary_nonAdminUnauthorizedDept_throwsServiceException() throws Exception {
        setupNonAdmin("store-user", 100L);
        StoreFinanceReportServiceImpl service = createService(new FakeStoreFinanceReportMapper());

        StoreReportQueryParams params = makeParams(200L);

        assertThrows(ServiceException.class, () -> service.getSummary(params),
            "非 admin 请求非授权门店应拒绝");
    }

    // ── 非 admin 访问自己门店应正常 ──

    @Test
    void getSummary_nonAdminOwnDept_returnsVO() throws Exception {
        setupNonAdmin("store-user", 100L);
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = new BigDecimal("500.00");
        mapper.saleCount = 5;
        mapper.saleQuantity = 10;
        mapper.totalExpense = new BigDecimal("200.00");
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreReportQueryParams params = makeParams(100L);
        StoreOperationSummaryVO vo = service.getSummary(params);

        assertNotNull(vo, "非 admin 访问自己门店应返回数据");
        assertEquals(new BigDecimal("500.00"), vo.getTotalSales());
    }

    // ── 销售额 1000、费用 300 时，经营利润 700，利润率 70.00 ──

    @Test
    void getSummary_calculatesProfitAndRate() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = new BigDecimal("1000.00");
        mapper.saleCount = 10;
        mapper.saleQuantity = 50;
        mapper.totalExpense = new BigDecimal("300.00");
        mapper.unverifiedExpense = BigDecimal.ZERO;
        mapper.unverifiedAdvance = BigDecimal.ZERO;
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreOperationSummaryVO vo = service.getSummary(makeParams(100L));

        assertEquals(new BigDecimal("1000.00"), vo.getTotalSales(), "销售额应为 1000.00");
        assertEquals(new BigDecimal("300.00"), vo.getTotalExpense(), "费用应为 300.00");
        assertEquals(new BigDecimal("700.00"), vo.getOperatingProfit(),
            "经营利润 = 1000 - 300 = 700");
        assertEquals(new BigDecimal("70.00"), vo.getOperatingProfitRate(),
            "利润率 = 700 / 1000 * 100 = 70.00");
    }

    // ── 销售额为 0 时利润率为 0 ──

    @Test
    void getSummary_salesZero_profitRateIsZero() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = BigDecimal.ZERO;
        mapper.saleCount = 0;
        mapper.saleQuantity = 0;
        mapper.totalExpense = new BigDecimal("100.00");
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreOperationSummaryVO vo = service.getSummary(makeParams(100L));

        assertEquals(BigDecimal.ZERO, vo.getTotalSales(), "销售额为 0");
        assertEquals(BigDecimal.ZERO, vo.getOperatingProfitRate(),
            "销售额为 0 时利润率应为 0，不应除零");
    }

    // ── 未核销费用或未核销借支大于 0 时 alerts 包含提醒 ──

    @Test
    void getSummary_unverifiedItems_generatesAlerts() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = new BigDecimal("2000.00");
        mapper.saleCount = 20;
        mapper.saleQuantity = 40;
        mapper.totalExpense = new BigDecimal("500.00");
        mapper.unverifiedExpense = new BigDecimal("150.00");
        mapper.unverifiedAdvance = new BigDecimal("80.00");
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreOperationSummaryVO vo = service.getSummary(makeParams(100L));

        assertEquals(new BigDecimal("150.00"), vo.getUnverifiedExpense(), "未核销费用应为 150");
        assertEquals(new BigDecimal("80.00"), vo.getUnverifiedAdvance(), "未核销借支应为 80");
        assertNotNull(vo.getAlerts(), "alerts 不应为 null");
        assertTrue(vo.getAlerts().stream().anyMatch(a -> a.contains("未核销费用")),
            "alerts 应包含未核销费用提醒");
        assertTrue(vo.getAlerts().stream().anyMatch(a -> a.contains("未核销借支")),
            "alerts 应包含未核销借支提醒");
    }

    // ── 利润为负时 alerts 包含提醒 ──

    @Test
    void getSummary_negativeProfit_generatesAlert() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = new BigDecimal("100.00");
        mapper.saleCount = 2;
        mapper.saleQuantity = 2;
        mapper.totalExpense = new BigDecimal("500.00");
        mapper.unverifiedExpense = BigDecimal.ZERO;
        mapper.unverifiedAdvance = BigDecimal.ZERO;
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreOperationSummaryVO vo = service.getSummary(makeParams(100L));

        assertTrue(vo.getOperatingProfit().compareTo(BigDecimal.ZERO) < 0, "利润应为负");
        assertTrue(vo.getAlerts().stream().anyMatch(a -> a.contains("利润为负")),
            "alerts 应包含利润为负提醒");
    }

    // ── 销售为 0 时 alerts 包含提醒 ──

    @Test
    void getSummary_noSales_generatesAlert() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = BigDecimal.ZERO;
        mapper.saleCount = 0;
        mapper.saleQuantity = 0;
        mapper.totalExpense = BigDecimal.ZERO;
        mapper.unverifiedExpense = BigDecimal.ZERO;
        mapper.unverifiedAdvance = BigDecimal.ZERO;
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreOperationSummaryVO vo = service.getSummary(makeParams(100L));

        assertTrue(vo.getAlerts().stream().anyMatch(a -> a.contains("暂无销售")),
            "alerts 应包含无销售提醒");
    }

    // ── avgOrderAmount 计算 ──

    @Test
    void getSummary_calculatesAvgOrderAmount() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = new BigDecimal("3000.00");
        mapper.saleCount = 10;
        mapper.saleQuantity = 30;
        mapper.totalExpense = BigDecimal.ZERO;
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreOperationSummaryVO vo = service.getSummary(makeParams(100L));

        assertEquals(new BigDecimal("300.00"), vo.getAvgOrderAmount(),
            "均单价 = 3000 / 10 = 300.00");
    }

    // ── R2: 趋势合并测试 ──

    @Test
    void getSummary_mergesSalesAndExpenseTrends_sameDate() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = new BigDecimal("1000.00");
        mapper.saleCount = 5;
        mapper.saleQuantity = 10;
        mapper.totalExpense = new BigDecimal("400.00");

        StoreTrendRowVO saleRow = new StoreTrendRowVO();
        saleRow.setDateStr("2026-06-25");
        saleRow.setSalesAmount(new BigDecimal("1000.00"));
        mapper.salesTrend = List.of(saleRow);

        StoreTrendRowVO expRow = new StoreTrendRowVO();
        expRow.setDateStr("2026-06-25");
        expRow.setExpenseAmount(new BigDecimal("400.00"));
        mapper.expenseTrend = List.of(expRow);

        StoreFinanceReportServiceImpl service = createService(mapper);
        StoreOperationSummaryVO vo = service.getSummary(makeParams(100L));

        assertNotNull(vo.getTrendRows());
        assertEquals(1, vo.getTrendRows().size(), "同一天应合并为一行");
        StoreTrendRowVO row = vo.getTrendRows().get(0);
        assertEquals("2026-06-25", row.getDateStr());
        assertEquals(new BigDecimal("1000.00"), row.getSalesAmount());
        assertEquals(new BigDecimal("400.00"), row.getExpenseAmount());
        assertEquals(new BigDecimal("600.00"), row.getOperatingProfit(),
            "经营利润 = 1000 - 400 = 600");
    }

    @Test
    void getSummary_mergesTrends_differentDates() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = new BigDecimal("1000.00");
        mapper.saleCount = 5;
        mapper.saleQuantity = 10;
        mapper.totalExpense = new BigDecimal("300.00");

        StoreTrendRowVO saleRow = new StoreTrendRowVO();
        saleRow.setDateStr("2026-06-25");
        saleRow.setSalesAmount(new BigDecimal("1000.00"));
        mapper.salesTrend = List.of(saleRow);

        StoreTrendRowVO expRow = new StoreTrendRowVO();
        expRow.setDateStr("2026-06-26");
        expRow.setExpenseAmount(new BigDecimal("300.00"));
        mapper.expenseTrend = List.of(expRow);

        StoreFinanceReportServiceImpl service = createService(mapper);
        StoreOperationSummaryVO vo = service.getSummary(makeParams(100L));

        assertNotNull(vo.getTrendRows());
        assertEquals(2, vo.getTrendRows().size(), "不同日期应产生两行");
        // 第一行：只有销售
        assertEquals("2026-06-25", vo.getTrendRows().get(0).getDateStr());
        assertEquals(new BigDecimal("1000.00"), vo.getTrendRows().get(0).getSalesAmount());
        assertEquals(BigDecimal.ZERO, vo.getTrendRows().get(0).getExpenseAmount());
        // 第二行：只有费用
        assertEquals("2026-06-26", vo.getTrendRows().get(1).getDateStr());
        assertEquals(BigDecimal.ZERO, vo.getTrendRows().get(1).getSalesAmount());
        assertEquals(new BigDecimal("300.00"), vo.getTrendRows().get(1).getExpenseAmount());
        assertEquals(new BigDecimal("-300.00"), vo.getTrendRows().get(1).getOperatingProfit(),
            "只有费用没有销售时利润应为 -300");
    }

    // ── R2: 费用分类百分比测试 ──

    @Test
    void getSummary_expenseCategories_percentSumTo100() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = new BigDecimal("2000.00");
        mapper.saleCount = 10;
        mapper.saleQuantity = 20;
        mapper.totalExpense = new BigDecimal("1000.00");

        StoreExpenseCategoryVO cat1 = new StoreExpenseCategoryVO();
        cat1.setCategoryName("办公费");
        cat1.setAmount(new BigDecimal("600.00"));

        StoreExpenseCategoryVO cat2 = new StoreExpenseCategoryVO();
        cat2.setCategoryName("差旅费");
        cat2.setAmount(new BigDecimal("400.00"));

        mapper.expenseCategories = List.of(cat1, cat2);

        StoreFinanceReportServiceImpl service = createService(mapper);
        StoreOperationSummaryVO vo = service.getSummary(makeParams(100L));

        assertNotNull(vo.getExpenseCategories());
        assertEquals(2, vo.getExpenseCategories().size());
        // 600/1000*100 = 60.00
        assertEquals(new BigDecimal("60.00"), vo.getExpenseCategories().get(0).getPercent(),
            "办公费占比应为 60.00%");
        // 400/1000*100 = 40.00
        assertEquals(new BigDecimal("40.00"), vo.getExpenseCategories().get(1).getPercent(),
            "差旅费占比应为 40.00%");
    }

    // ── R2: 未核销清单测试 ──

    @Test
    void getSummary_pendingItems_combinesExpensesAndAdvances() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = new BigDecimal("5000.00");
        mapper.saleCount = 20;
        mapper.saleQuantity = 50;
        mapper.totalExpense = new BigDecimal("1000.00");
        mapper.unverifiedExpense = new BigDecimal("200.00");
        mapper.unverifiedAdvance = new BigDecimal("150.00");

        StorePendingItemVO pendingExpense = new StorePendingItemVO();
        pendingExpense.setItemType("EXPENSE");
        pendingExpense.setItemId(1L);
        pendingExpense.setItemNo("FY202606010001");
        pendingExpense.setAmount(new BigDecimal("200.00"));
        pendingExpense.setStatus("0");
        mapper.unverifiedExpenses = List.of(pendingExpense);

        StorePendingItemVO pendingAdvance = new StorePendingItemVO();
        pendingAdvance.setItemType("ADVANCE");
        pendingAdvance.setItemId(2L);
        pendingAdvance.setItemNo("JZ202606010001");
        pendingAdvance.setAmount(new BigDecimal("150.00"));
        pendingAdvance.setStatus("0");
        mapper.unverifiedAdvances = List.of(pendingAdvance);

        StoreFinanceReportServiceImpl service = createService(mapper);
        StoreOperationSummaryVO vo = service.getSummary(makeParams(100L));

        assertNotNull(vo.getPendingItems());
        assertEquals(2, vo.getPendingItems().size(), "应合并费用和借支共 2 条");
        assertTrue(vo.getPendingItems().stream().anyMatch(i -> "EXPENSE".equals(i.getItemType())),
            "应包含 EXPENSE 类型");
        assertTrue(vo.getPendingItems().stream().anyMatch(i -> "ADVANCE".equals(i.getItemType())),
            "应包含 ADVANCE 类型");
    }

    // ── R6: 本期/上期对比 ──

    @Test
    void getSummary_withDateRange_calculatesPreviousPeriodAndChangeRate() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = new BigDecimal("2000.00");
        mapper.totalExpense = new BigDecimal("500.00");
        mapper.previousSales = new BigDecimal("1000.00");
        mapper.previousExpense = new BigDecimal("400.00");
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreOperationSummaryVO vo = service.getSummary(makeParams(100L));

        assertEquals(new BigDecimal("1000.00"), vo.getPreviousTotalSales());
        assertEquals(new BigDecimal("400.00"), vo.getPreviousTotalExpense());
        assertEquals(new BigDecimal("600.00"), vo.getPreviousOperatingProfit());
        assertEquals(new BigDecimal("100.00"), vo.getSalesChangeRate());
        assertEquals(new BigDecimal("25.00"), vo.getExpenseChangeRate());
    }

    @Test
    void getSummary_noDateRange_returnsZeroComparison() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreReportQueryParams params = new StoreReportQueryParams();
        params.setDeptId(100L);
        // startTime and endTime are null
        StoreOperationSummaryVO vo = service.getSummary(params);

        assertEquals(BigDecimal.ZERO, vo.getPreviousTotalSales());
        assertEquals(BigDecimal.ZERO, vo.getSalesChangeRate());
    }

    // ── R7: 经营建议测试 ──

    @Test
    void getSummary_negativeProfit_generatesSuggestion() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = new BigDecimal("100.00");
        mapper.totalExpense = new BigDecimal("500.00");
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreOperationSummaryVO vo = service.getSummary(makeParams(100L));

        assertNotNull(vo.getSuggestions());
        assertTrue(vo.getSuggestions().stream().anyMatch(s -> s.contains("经营利润为负")),
            "应包含利润为负建议");
    }

    @Test
    void getSummary_highUnverified_generatesSuggestion() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = new BigDecimal("5000.00");
        mapper.totalExpense = new BigDecimal("1000.00");
        mapper.unverifiedExpense = new BigDecimal("800.00");
        mapper.unverifiedAdvance = new BigDecimal("500.00");
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreOperationSummaryVO vo = service.getSummary(makeParams(100L));

        assertNotNull(vo.getSuggestions());
        assertTrue(vo.getSuggestions().stream().anyMatch(s -> s.contains("未核销金额较高")),
            "应包含未核销金额较高建议");
    }

    @Test
    void getSummary_salesDrop_generatesSuggestion() throws Exception {
        setupAdmin();
        FakeStoreFinanceReportMapper mapper = new FakeStoreFinanceReportMapper();
        mapper.totalSales = new BigDecimal("700.00");
        mapper.totalExpense = new BigDecimal("200.00");
        mapper.previousSales = new BigDecimal("1000.00");
        mapper.previousExpense = new BigDecimal("200.00");
        StoreFinanceReportServiceImpl service = createService(mapper);

        StoreOperationSummaryVO vo = service.getSummary(makeParams(100L));

        assertNotNull(vo.getSuggestions());
        assertTrue(vo.getSuggestions().stream().anyMatch(s -> s.contains("销售额较上期下降")),
            "应包含销售下降建议");
    }

    // ── Fake RemoteUserService ──

    static class FakeRemoteUserService implements RemoteUserService {
        private final List<Long> deptIds;

        FakeRemoteUserService(List<Long> deptIds) {
            this.deptIds = deptIds;
        }

        @Override
        public R<LoginUser> getUserInfo(String username, String source) {
            return null;
        }

        @Override
        public R<Boolean> registerUserInfo(com.junsong.system.api.domain.SysUser user, String source) {
            return null;
        }

        @Override
        public R<Boolean> recordUserLogin(com.junsong.system.api.domain.SysUser user, String source) {
            return null;
        }

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
        public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) {
            return null;
        }
    }

    // ── Fake Mapper ──

    static class FakeStoreFinanceReportMapper implements StoreFinanceReportMapper {
        BigDecimal totalSales = BigDecimal.ZERO;
        int saleCount = 0;
        int saleQuantity = 0;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal unverifiedExpense = BigDecimal.ZERO;
        BigDecimal unverifiedAdvance = BigDecimal.ZERO;
        BigDecimal previousSales = BigDecimal.ZERO;
        BigDecimal previousExpense = BigDecimal.ZERO;
        String periodStatus = null;

        List<StoreTrendRowVO> salesTrend = Collections.emptyList();
        List<StoreTrendRowVO> expenseTrend = Collections.emptyList();
        List<StoreExpenseCategoryVO> expenseCategories = Collections.emptyList();
        List<StorePendingItemVO> unverifiedExpenses = Collections.emptyList();
        List<StorePendingItemVO> unverifiedAdvances = Collections.emptyList();

        StoreReportQueryParams lastParams;

        @Override
        public BigDecimal selectStoreTotalSales(StoreReportQueryParams params) {
            this.lastParams = params;
            return totalSales;
        }

        @Override
        public Integer countStoreSaleRecords(StoreReportQueryParams params) {
            this.lastParams = params;
            return saleCount;
        }

        @Override
        public Integer sumStoreSaleQuantity(StoreReportQueryParams params) {
            this.lastParams = params;
            return saleQuantity;
        }

        @Override
        public BigDecimal selectStoreTotalExpense(StoreReportQueryParams params) {
            this.lastParams = params;
            return totalExpense;
        }

        @Override
        public BigDecimal selectStoreUnverifiedExpense(StoreReportQueryParams params) {
            this.lastParams = params;
            return unverifiedExpense;
        }

        @Override
        public BigDecimal selectStoreUnverifiedAdvance(StoreReportQueryParams params) {
            this.lastParams = params;
            return unverifiedAdvance;
        }

        @Override
        public String selectCurrentAccountingPeriodStatus(Long deptId) {
            return periodStatus;
        }

        @Override
        public List<StoreTrendRowVO> selectStoreSalesTrend(StoreReportQueryParams params) {
            this.lastParams = params;
            return salesTrend;
        }

        @Override
        public List<StoreTrendRowVO> selectStoreExpenseTrend(StoreReportQueryParams params) {
            this.lastParams = params;
            return expenseTrend;
        }

        @Override
        public List<StoreExpenseCategoryVO> selectStoreExpenseCategories(StoreReportQueryParams params) {
            this.lastParams = params;
            return expenseCategories;
        }

        @Override
        public List<StorePendingItemVO> selectStoreUnverifiedExpenses(StoreReportQueryParams params) {
            this.lastParams = params;
            return unverifiedExpenses;
        }

        @Override
        public List<StorePendingItemVO> selectStoreUnverifiedAdvances(StoreReportQueryParams params) {
            this.lastParams = params;
            return unverifiedAdvances;
        }

        @Override
        public BigDecimal selectStoreTotalSalesForRange(Long deptId, Date startTime, Date endTime) {
            return previousSales;
        }

        @Override
        public BigDecimal selectStoreTotalExpenseForRange(Long deptId, Date startTime, Date endTime) {
            return previousExpense;
        }

        @Override
        public List<AuthorizedStoreRowVO> selectAuthorizedStoreRows(List<Long> deptIds, Date startTime, Date endTime) {
            return Collections.emptyList();
        }

        @Override
        public List<Map<String, Object>> selectMemberSalesByDepts(List<Long> deptIds, Date startTime, Date endTime) {
            return Collections.emptyList();
        }
    }
}
