package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinExpense;
import com.junsong.finance.domain.FinProfitShareRecord;
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.domain.vo.*;
import com.junsong.finance.mapper.*;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.model.LoginUser;
import com.junsong.common.core.context.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FinanceOperationDashboardServiceImplTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.remove();
    }

    // ── helpers ──

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = FinanceReportServiceImpl.class.getDeclaredField(name);
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

    private static ReportQueryParams makeParams(List<Long> deptIds) {
        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(deptIds);
        params.setStartTime(new Date());
        params.setEndTime(new Date());
        return params;
    }

    private FinanceReportServiceImpl createService(FakeFinSaleRecordMapper saleMapper,
                                                     FakeFinExpenseMapper expenseMapper,
                                                     FakeFinProfitShareRecordMapper profitMapper,
                                                     FakeFinAccountingPeriodMapper periodMapper,
                                                     RemoteUserService remoteUserService) throws Exception {
        FinanceReportServiceImpl svc = new FinanceReportServiceImpl();
        setField(svc, "finSaleRecordMapper", saleMapper);
        setField(svc, "finExpenseMapper", expenseMapper);
        setField(svc, "finProfitShareRecordMapper", profitMapper);
        setField(svc, "finAccountingPeriodMapper", periodMapper);
        setField(svc, "remoteUserService", remoteUserService);
        setField(svc, "finProfitShareDetailMapper", new FakeFinProfitShareDetailMapper());
        return svc;
    }

    // ── Tests ──

    @Test
    void getOperationDashboard_nonAdminWithAuthorizedDepts_filtersToThoseDepts() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeFinSaleRecordMapper saleMapper = new FakeFinSaleRecordMapper();
        FakeFinExpenseMapper expenseMapper = new FakeFinExpenseMapper();
        FakeFinProfitShareRecordMapper profitMapper = new FakeFinProfitShareRecordMapper();
        FakeFinAccountingPeriodMapper periodMapper = new FakeFinAccountingPeriodMapper();
        FakeRemoteUserService remoteUserService = new FakeRemoteUserService(List.of(100L, 200L));

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        // request with deptIds that include unauthorized dept
        ReportQueryParams params = makeParams(List.of(100L, 200L, 999L));
        FinanceOperationDashboardVO vo = svc.getOperationDashboard(params);

        // After applyDataScope, only 100 and 200 should remain
        assertEquals(List.of(100L, 200L), params.getDeptIds());
        assertNotNull(vo);
    }

    @Test
    void getOperationDashboard_nonAdminNoDepts_sentinelDeptId() throws Exception {
        // non-admin with no deptId and no authorized depts -> sentinel -1L
        SecurityContextHolder.setUserId("3");
        SecurityContextHolder.setUserName("no-dept-user");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(3L);
        loginUser.setUsername("no-dept-user");
        // deptId is null
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);

        FakeFinSaleRecordMapper saleMapper = new FakeFinSaleRecordMapper();
        FakeFinExpenseMapper expenseMapper = new FakeFinExpenseMapper();
        FakeFinProfitShareRecordMapper profitMapper = new FakeFinProfitShareRecordMapper();
        FakeFinAccountingPeriodMapper periodMapper = new FakeFinAccountingPeriodMapper();
        FakeRemoteUserService remoteUserService = new FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        ReportQueryParams params = makeParams(null);
        FinanceOperationDashboardVO vo = svc.getOperationDashboard(params);

        // sentinel dept ID -1L should be used
        assertEquals(Collections.singletonList(-1L), params.getDeptIds());
        // zero metrics since nothing matches sentinel
        assertEquals(BigDecimal.ZERO, vo.getTodaySales());
        assertEquals(BigDecimal.ZERO, vo.getMonthSales());
        assertEquals(BigDecimal.ZERO, vo.getNetProfit());
    }

    @Test
    void getOperationDashboard_salesDrop_generatesWarning() throws Exception {
        setupAdmin();
        FakeFinSaleRecordMapper saleMapper = new FakeFinSaleRecordMapper();
        // Current month sales = 700, previous month = 1000 -> drop of 30% > 20%
        saleMapper.monthTotalSales = new BigDecimal("700.00");
        saleMapper.prevMonthTotalSales = new BigDecimal("1000.00");
        FakeFinExpenseMapper expenseMapper = new FakeFinExpenseMapper();
        FakeFinProfitShareRecordMapper profitMapper = new FakeFinProfitShareRecordMapper();
        FakeFinAccountingPeriodMapper periodMapper = new FakeFinAccountingPeriodMapper();
        FakeRemoteUserService remoteUserService = new FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        FinanceOperationDashboardVO vo = svc.getOperationDashboard(makeParams(List.of(100L)));

        assertNotNull(vo.getWarnings());
        assertTrue(vo.getWarnings().stream().anyMatch(w -> "SALES_DROP".equals(w.getWarningType())),
                "Sales drop > 20% should generate SALES_DROP warning");
    }

    @Test
    void getOperationDashboard_profitRateBelow5_generatesWarning() throws Exception {
        setupAdmin();
        FakeFinSaleRecordMapper saleMapper = new FakeFinSaleRecordMapper();
        // monthSales = 1000, monthExpense = 960 => netProfit = 40, profitRate = 4% < 5%
        saleMapper.monthTotalSales = new BigDecimal("1000.00");
        FakeFinExpenseMapper expenseMapper = new FakeFinExpenseMapper();
        expenseMapper.monthTotalExpense = new BigDecimal("960.00");
        FakeFinProfitShareRecordMapper profitMapper = new FakeFinProfitShareRecordMapper();
        FakeFinAccountingPeriodMapper periodMapper = new FakeFinAccountingPeriodMapper();
        FakeRemoteUserService remoteUserService = new FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        FinanceOperationDashboardVO vo = svc.getOperationDashboard(makeParams(List.of(100L)));

        assertNotNull(vo.getWarnings());
        assertTrue(vo.getWarnings().stream().anyMatch(w -> "PROFIT_RATE_DROP".equals(w.getWarningType())),
                "Profit rate < 5% should generate PROFIT_RATE_DROP warning");
    }

    @Test
    void getOperationDashboard_unverifiedExpenseCountAndAmount() throws Exception {
        setupAdmin();
        FakeFinSaleRecordMapper saleMapper = new FakeFinSaleRecordMapper();
        FakeFinExpenseMapper expenseMapper = new FakeFinExpenseMapper();
        expenseMapper.unverifiedCount = 5;
        expenseMapper.unverifiedAmount = new BigDecimal("1200.00");
        FakeFinProfitShareRecordMapper profitMapper = new FakeFinProfitShareRecordMapper();
        FakeFinAccountingPeriodMapper periodMapper = new FakeFinAccountingPeriodMapper();
        FakeRemoteUserService remoteUserService = new FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        FinanceOperationDashboardVO vo = svc.getOperationDashboard(makeParams(List.of(100L)));

        assertEquals(5, vo.getUnverifiedExpenseCount());
        assertEquals(new BigDecimal("1200.00"), vo.getUnverifiedExpenseAmount());
    }

    @Test
    void getOperationDashboard_unsettledProfitShareCount() throws Exception {
        setupAdmin();
        FakeFinSaleRecordMapper saleMapper = new FakeFinSaleRecordMapper();
        FakeFinExpenseMapper expenseMapper = new FakeFinExpenseMapper();
        FakeFinProfitShareRecordMapper profitMapper = new FakeFinProfitShareRecordMapper();
        profitMapper.unsettledCount = 3;
        FakeFinAccountingPeriodMapper periodMapper = new FakeFinAccountingPeriodMapper();
        FakeRemoteUserService remoteUserService = new FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        FinanceOperationDashboardVO vo = svc.getOperationDashboard(makeParams(List.of(100L)));

        assertEquals(3, vo.getUnsettledProfitShareCount());
    }

    @Test
    void getOperationDashboard_periodStatusActive() throws Exception {
        setupAdmin();
        FakeFinSaleRecordMapper saleMapper = new FakeFinSaleRecordMapper();
        FakeFinExpenseMapper expenseMapper = new FakeFinExpenseMapper();
        FakeFinProfitShareRecordMapper profitMapper = new FakeFinProfitShareRecordMapper();
        FakeFinAccountingPeriodMapper periodMapper = new FakeFinAccountingPeriodMapper();
        periodMapper.periodStatus = "0"; // ACTIVE
        FakeRemoteUserService remoteUserService = new FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        FinanceOperationDashboardVO vo = svc.getOperationDashboard(makeParams(List.of(100L)));

        assertEquals("0", vo.getCurrentPeriodStatus());
    }

    @Test
    void getOperationDashboard_periodStatusNull_defaultsToActive() throws Exception {
        setupAdmin();
        FakeFinSaleRecordMapper saleMapper = new FakeFinSaleRecordMapper();
        FakeFinExpenseMapper expenseMapper = new FakeFinExpenseMapper();
        FakeFinProfitShareRecordMapper profitMapper = new FakeFinProfitShareRecordMapper();
        FakeFinAccountingPeriodMapper periodMapper = new FakeFinAccountingPeriodMapper();
        periodMapper.periodStatus = null; // no period
        FakeRemoteUserService remoteUserService = new FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        FinanceOperationDashboardVO vo = svc.getOperationDashboard(makeParams(List.of(100L)));

        assertEquals("ACTIVE", vo.getCurrentPeriodStatus());
    }

    @Test
    void getOperationDashboard_netProfitCalculation() throws Exception {
        setupAdmin();
        FakeFinSaleRecordMapper saleMapper = new FakeFinSaleRecordMapper();
        saleMapper.monthTotalSales = new BigDecimal("5000.00");
        FakeFinExpenseMapper expenseMapper = new FakeFinExpenseMapper();
        expenseMapper.monthTotalExpense = new BigDecimal("2000.00");
        FakeFinProfitShareRecordMapper profitMapper = new FakeFinProfitShareRecordMapper();
        FakeFinAccountingPeriodMapper periodMapper = new FakeFinAccountingPeriodMapper();
        FakeRemoteUserService remoteUserService = new FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        FinanceOperationDashboardVO vo = svc.getOperationDashboard(makeParams(List.of(100L)));

        assertEquals(new BigDecimal("3000.00"), vo.getNetProfit(),
                "netProfit = monthSales - monthExpense = 5000 - 2000 = 3000");
        assertEquals(new BigDecimal("60.00"), vo.getProfitRate(),
                "profitRate = 3000 / 5000 * 100 = 60.00");
    }

    @Test
    void getOperationDashboard_salesTopStores() throws Exception {
        setupAdmin();
        FakeFinSaleRecordMapper saleMapper = new FakeFinSaleRecordMapper();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("deptId", 100L);
        row1.put("deptName", "Store A");
        row1.put("totalSales", new BigDecimal("3000.00"));
        Map<String, Object> row2 = new HashMap<>();
        row2.put("deptId", 200L);
        row2.put("deptName", "Store B");
        row2.put("totalSales", new BigDecimal("2000.00"));
        saleMapper.salesByDept = List.of(row1, row2);
        FakeFinExpenseMapper expenseMapper = new FakeFinExpenseMapper();
        FakeFinProfitShareRecordMapper profitMapper = new FakeFinProfitShareRecordMapper();
        FakeFinAccountingPeriodMapper periodMapper = new FakeFinAccountingPeriodMapper();
        FakeRemoteUserService remoteUserService = new FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        FinanceOperationDashboardVO vo = svc.getOperationDashboard(makeParams(List.of(100L, 200L)));

        assertNotNull(vo.getSalesTopStores());
        assertEquals(2, vo.getSalesTopStores().size());
        assertEquals(100L, vo.getSalesTopStores().get(0).getDeptId());
    }

    @Test
    void getOperationDashboard_receivableIndicators() throws Exception {
        setupAdmin();
        FakeFinSaleRecordMapper saleMapper = new FakeFinSaleRecordMapper();
        saleMapper.currentPeriodPayment = new BigDecimal("5000.00");
        saleMapper.historicalCollected = new BigDecimal("500.00");
        saleMapper.currentNewReceivable = new BigDecimal("800.00");
        saleMapper.endingBalance = new BigDecimal("1300.00");
        saleMapper.overdueCount = 2;
        FakeFinExpenseMapper expenseMapper = new FakeFinExpenseMapper();
        FakeFinProfitShareRecordMapper profitMapper = new FakeFinProfitShareRecordMapper();
        FakeFinAccountingPeriodMapper periodMapper = new FakeFinAccountingPeriodMapper();
        periodMapper.currentPeriod = new com.junsong.finance.domain.FinAccountingPeriod();
        periodMapper.currentPeriod.setPeriodId(42L);
        FakeRemoteUserService remoteUserService = new FakeRemoteUserService(Collections.emptyList());

        FinanceReportServiceImpl svc = createService(saleMapper, expenseMapper, profitMapper, periodMapper, remoteUserService);

        FinanceOperationDashboardVO vo = svc.getOperationDashboard(makeParams(List.of(100L)));

        assertEquals(new BigDecimal("5000.00"), vo.getCurrentPeriodPaymentAmount());
        assertEquals(new BigDecimal("500.00"), vo.getHistoricalReceivableCollectedAmount());
        assertEquals(new BigDecimal("800.00"), vo.getCurrentPeriodNewReceivableAmount());
        assertEquals(new BigDecimal("1300.00"), vo.getEndingReceivableAmount());
        assertEquals(2, vo.getOverdueReceivableCount());
    }

    // ── Fakes ──

    static class FakeRemoteUserService implements RemoteUserService {
        private final List<Long> deptIds;
        FakeRemoteUserService(List<Long> deptIds) { this.deptIds = deptIds; }
        @Override public R<LoginUser> getUserInfo(String username, String source) { return null; }
        @Override public R<Boolean> registerUserInfo(com.junsong.system.api.domain.SysUser user, String source) { return null; }
        @Override public R<Boolean> recordUserLogin(com.junsong.system.api.domain.SysUser user, String source) { return null; }
        @Override public R<List<SysDept>> getUserDeptList(String username, String source) {
            List<SysDept> list = deptIds.stream().map(id -> {
                SysDept d = new SysDept(); d.setDeptId(id); d.setDeptName("Store" + id); return d;
            }).collect(Collectors.toList());
            return R.ok(list);
        }
        @Override public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) { return null; }
    }

    static class FakeFinSaleRecordMapper implements FinSaleRecordMapper {
        BigDecimal todayTotalSales = BigDecimal.ZERO;
        BigDecimal monthTotalSales = BigDecimal.ZERO;
        BigDecimal prevMonthTotalSales = BigDecimal.ZERO;
        List<Map<String, Object>> salesByDept = Collections.emptyList();
        List<Map<String, Object>> saleTrendStats = Collections.emptyList();
        List<Map<String, Object>> productRank = Collections.emptyList();
        BigDecimal memberSales = BigDecimal.ZERO;
        BigDecimal seckillSales = BigDecimal.ZERO;
        BigDecimal currentPeriodPayment = BigDecimal.ZERO;
        BigDecimal historicalCollected = BigDecimal.ZERO;
        BigDecimal currentNewReceivable = BigDecimal.ZERO;
        BigDecimal endingBalance = BigDecimal.ZERO;
        int overdueCount = 0;

        @Override public FinSaleRecord selectFinSaleRecordBySaleId(Long saleId) { return null; }
        @Override public FinSaleRecord selectFinSaleRecordBySaleIdForUpdate(Long saleId) { return selectFinSaleRecordBySaleId(saleId); }
        @Override public List<FinSaleRecord> selectFinSaleRecordList(FinSaleRecord r) { return Collections.emptyList(); }
        @Override public int insertFinSaleRecord(FinSaleRecord r) { return 0; }
        @Override public int updateFinSaleRecord(FinSaleRecord r) { return 0; }
        @Override public int updatePaidAmountAndStatus(Long saleId, java.math.BigDecimal paidAmount, String status) { return 0; }
        @Override public java.util.List<FinSaleRecord> selectReceivableList(FinSaleRecord r) { return java.util.Collections.emptyList(); }
        @Override public int countReceivableByPeriodId(Long deptId, Long periodId) { return 0; }
        @Override public java.math.BigDecimal sumReceivableByPeriodId(Long deptId, Long periodId) { return java.math.BigDecimal.ZERO; }
        @Override public int deleteFinSaleRecordBySaleId(Long saleId) { return 0; }
        @Override public int deleteFinSaleRecordBySaleIds(Long[] saleIds) { return 0; }
        @Override public List<Map<String, Object>> selectSaleTrendStats(List<Long> deptIds, Date s, Date e) { return saleTrendStats; }
        @Override public int countSaleRecords(List<Long> deptIds, Date s, Date e) { return 0; }
        @Override public int sumSaleQuantity(List<Long> deptIds, Date s, Date e) { return 0; }
        @Override public FinSaleRecord checkSaleNoUnique(String saleNo) { return null; }
        @Override public int countTodaySales() { return 0; }
        @Override public int maxTodaySaleSeq() { return 0; }
        @Override public BigDecimal selectTodayTotalSales(List<Long> deptIds) { return todayTotalSales; }
        @Override public BigDecimal selectMonthTotalSales(List<Long> deptIds) { return monthTotalSales; }
        @Override public BigDecimal selectTodayTotalSalesForPrev(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalSalesForPrev(List<Long> deptIds) { return prevMonthTotalSales; }
        @Override public List<Map<String, Object>> selectSalesByDept(List<Long> deptIds, Date s, Date e) { return salesByDept; }
        @Override public List<Map<String, Object>> selectProductSalesRank(List<Long> deptIds, Date s, Date e) { return productRank; }
        @Override public BigDecimal selectMemberSales(List<Long> deptIds, Date s, Date e) { return memberSales; }
        @Override public BigDecimal selectSeckillSales(List<Long> deptIds, Date s, Date e) { return seckillSales; }
        @Override public BigDecimal selectCurrentPeriodPaymentTotal(List<Long> deptIds, Long periodId) { return currentPeriodPayment; }
        @Override public BigDecimal selectHistoricalReceivableCollected(List<Long> deptIds, Long periodId) { return historicalCollected; }
        @Override public BigDecimal selectCurrentPeriodNewReceivable(List<Long> deptIds, Long periodId) { return currentNewReceivable; }
        @Override public BigDecimal selectEndingReceivableBalance(List<Long> deptIds) { return endingBalance; }
        @Override public int countOverdueReceivable(List<Long> deptIds) { return overdueCount; }
    }

    static class FakeFinExpenseMapper implements FinExpenseMapper {
        @Override public List<FinExpense> selectFinExpenseByExpenseIdsScoped(List<Long> ids, Long tenantId, Long deptId) { return Collections.emptyList(); }
        @Override public int markExpenseVerified(Long id, Long advanceId, String by, Date time, Long tenantId, Long deptId) { return 1; }
        @Override public int restoreExpenseUnverified(Long id) { return 1; }
        BigDecimal todayTotalExpense = BigDecimal.ZERO;
        BigDecimal monthTotalExpense = BigDecimal.ZERO;
        BigDecimal expenseTotal = BigDecimal.ZERO;
        int unverifiedCount = 0;
        BigDecimal unverifiedAmount = BigDecimal.ZERO;
        List<Map<String, Object>> categoryStats = Collections.emptyList();
        List<Map<String, Object>> trendStats = Collections.emptyList();
        List<Map<String, Object>> deptStats = Collections.emptyList();
        List<Map<String, Object>> unverifiedList = Collections.emptyList();
        List<Map<String, Object>> ocrAnomalies = Collections.emptyList();

        @Override public FinExpense selectFinExpenseByExpenseId(Long id) { return null; }
        @Override public List<FinExpense> selectFinExpenseList(FinExpense e) { return Collections.emptyList(); }
        @Override public int insertFinExpense(FinExpense e) { return 0; }
        @Override public int updateFinExpense(FinExpense e) { return 0; }
        @Override public int deleteFinExpenseByExpenseId(Long id) { return 0; }
        @Override public int deleteFinExpenseByExpenseIds(Long[] ids) { return 0; }
        @Override public FinExpense checkExpenseNoUnique(String no) { return null; }
        @Override public int countTodayExpenses() { return 0; }
        @Override public int maxTodayExpenseSeq() { return 0; }
        @Override public BigDecimal sumUnverifiedExpenses() { return BigDecimal.ZERO; }
        @Override public BigDecimal sumUnverifiedExpensesByDeptId(Long deptId) { return BigDecimal.ZERO; }
        @Override public BigDecimal sumAllExpenses() { return BigDecimal.ZERO; }
        @Override public BigDecimal sumAllExpensesByDeptId(Long deptId) { return BigDecimal.ZERO; }
        @Override public BigDecimal sumAllExpensesByPeriodId(Long periodId) { return BigDecimal.ZERO; }
        @Override public List<FinExpense> selectFinExpenseByExpenseIds(Long[] ids) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectExpenseCategoryStats(Map<String, Object> p) { return categoryStats; }
        @Override public List<Map<String, Object>> selectExpenseTrendStats(Map<String, Object> p) { return trendStats; }
        @Override public List<Map<String, Object>> selectExpenseDeptStats(Map<String, Object> p) { return deptStats; }
        @Override public BigDecimal selectExpenseTotal(Map<String, Object> p) { return expenseTotal; }
        @Override public BigDecimal selectTodayTotalExpense(List<Long> deptIds) { return todayTotalExpense; }
        @Override public BigDecimal selectMonthTotalExpense(List<Long> deptIds) { return monthTotalExpense; }
        @Override
        public BigDecimal selectMonthTotalExpenseForPrev(List<Long> deptIds) {
            return BigDecimal.ZERO;
        }
        @Override public int countUnverifiedExpenses(List<Long> deptIds) { return unverifiedCount; }
        @Override public BigDecimal sumUnverifiedExpenseAmount(List<Long> deptIds) { return unverifiedAmount; }
        @Override public int countUnverifiedExpensesByPeriodId(List<Long> deptIds, Long periodId) { return 0; }
        @Override public BigDecimal sumUnverifiedExpenseAmountByPeriodId(List<Long> deptIds, Long periodId) { return unverifiedAmount; }
        @Override public List<Map<String, Object>> selectExpenseCategoryStatsWithPrev(List<Long> d, Date s, Date e, Date ps, Date pe) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectUnverifiedExpenseList(List<Long> deptIds) { return unverifiedList; }
        @Override public List<Map<String, Object>> selectOcrAnomalies(List<Long> deptIds) { return ocrAnomalies; }
    }

    static class FakeFinProfitShareRecordMapper implements FinProfitShareRecordMapper {
        int unsettledCount = 0;
        List<Map<String, Object>> settlementRows = Collections.emptyList();

        @Override public FinProfitShareRecord selectFinProfitShareRecordByShareId(Long id) { return null; }
        @Override public FinProfitShareRecord selectFinProfitShareRecordByPeriodId(Long id) { return null; }
        @Override public List<FinProfitShareRecord> selectFinProfitShareRecordList(FinProfitShareRecord r) { return Collections.emptyList(); }
        @Override public int insertFinProfitShareRecord(FinProfitShareRecord r) { return 0; }
        @Override public int updateFinProfitShareRecord(FinProfitShareRecord r) { return 0; }
        @Override public int deleteFinProfitShareRecordByShareId(Long id) { return 0; }
        @Override public int deleteFinProfitShareRecordByShareIds(Long[] ids) { return 0; }
        @Override public BigDecimal selectProfitShareTotal(Map<String, Object> p) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectManagerProfitTotal(Map<String, Object> p) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectInvestorProfitTotal(Map<String, Object> p) { return BigDecimal.ZERO; }
        @Override public List<Map<String, Object>> selectManagerProfitByDept(Map<String, Object> p) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectInvestorProfitByDept(Map<String, Object> p) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectProfitShareTrend(Map<String, Object> p) { return Collections.emptyList(); }
        @Override public int countUnsettledRecords(List<Long> deptIds) { return unsettledCount; }
        @Override public int countUnsettledRecordsByPeriodId(List<Long> deptIds, Long periodId) { return unsettledCount; }
        @Override public List<Map<String, Object>> selectSettlementByDept(List<Long> d, Date s, Date e) { return settlementRows; }
        @Override public BigDecimal selectPaidAmount(List<Long> d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public int updateShareTimeByPeriodId(Long periodId, Date shareTime, String updateBy, String remark) { return 0; }
    }

    static class FakeFinAccountingPeriodMapper implements FinAccountingPeriodMapper {
        public FinAccountingPeriod selectPeriodForUpdate(Long id, Long tenantId, Long deptId) { return selectFinAccountingPeriodByPeriodId(id); }
        String periodStatus = null;
        FinAccountingPeriod currentPeriod = null;

        @Override public FinAccountingPeriod selectFinAccountingPeriodByPeriodId(Long id) { return null; }
        @Override public FinAccountingPeriod selectCurrentPeriodByDeptId(Long deptId) { return currentPeriod; }
        @Override public FinAccountingPeriod selectLatestCarriedPeriodByDeptId(Long deptId) { return null; }
        @Override public List<FinAccountingPeriod> selectFinAccountingPeriodList(FinAccountingPeriod p) { return Collections.emptyList(); }
        @Override public int insertFinAccountingPeriod(FinAccountingPeriod p) { return 0; }
        @Override public int updateFinAccountingPeriod(FinAccountingPeriod p) { return 0; }
        @Override public int resetCarryForwardByPeriodId(Long id, String u) { return 0; }
        @Override public int deleteFinAccountingPeriodByPeriodId(Long id) { return 0; }
        @Override public int deleteFinAccountingPeriodByPeriodIds(Long[] ids) { return 0; }
        @Override public BigDecimal selectTotalVerifiedExpense(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalPurchase(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalSalePayment(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalSaleAmount(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalUnverifiedAdvance(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public String selectCurrentPeriodStatusByDeptIds(List<Long> deptIds) { return periodStatus; }
        @Override public FinAccountingPeriod selectPeriodById(Long id) { return null; }
        @Override public FinAccountingPeriod selectPreviousPeriod(Long deptId, Date startTime, Long periodId) { return null; }
        @Override public FinAccountingPeriod selectNextPeriod(Long deptId, Date startTime, Long periodId) { return null; }
        @Override public int updateStartTimeOnly(Long periodId, Date startTime, Date endTime, String updateBy, String remark) { return 0; }
    }

    static class FakeFinProfitShareDetailMapper implements FinProfitShareDetailMapper {
        @Override public List<com.junsong.finance.domain.FinProfitShareDetail> selectFinProfitShareDetailByShareId(Long id) { return Collections.emptyList(); }
        @Override public int insertFinProfitShareDetail(com.junsong.finance.domain.FinProfitShareDetail d) { return 0; }
        @Override public int updateFinProfitShareDetail(com.junsong.finance.domain.FinProfitShareDetail d) { return 0; }
        @Override public BigDecimal selectDetailSumByShareId(Long id) { return BigDecimal.ZERO; }
    }
}
