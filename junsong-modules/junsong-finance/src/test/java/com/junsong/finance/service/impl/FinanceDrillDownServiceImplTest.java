package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.finance.domain.FinExpense;
import com.junsong.finance.domain.FinProfitShareRecord;
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.domain.vo.*;
import com.junsong.finance.mapper.FinExpenseMapper;
import com.junsong.finance.mapper.FinProfitShareRecordMapper;
import com.junsong.finance.mapper.FinSaleRecordMapper;
import com.junsong.finance.service.impl.FinanceDrillDownServiceImpl;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FinanceDrillDownServiceImplTest
{
    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.remove();
    }

    // -- helpers --

    private static void setField(Object target, String name, Object value) throws Exception
    {
        Field field = FinanceDrillDownServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setupAdmin()
    {
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(1L);
        loginUser.setUsername("admin");
        loginUser.setDeptId(100L);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static void setupNonAdmin(String username, Long deptId)
    {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName(username);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(2L);
        loginUser.setUsername(username);
        loginUser.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static SysDept makeDept(Long deptId)
    {
        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        return dept;
    }

    private static FinanceDrillDownServiceImpl createService(
            FinSaleRecordMapper saleMapper,
            FinExpenseMapper expenseMapper,
            FinProfitShareRecordMapper profitShareMapper,
            RemoteUserService remoteUserService) throws Exception
    {
        FinanceDrillDownServiceImpl service = new FinanceDrillDownServiceImpl();
        setField(service, "remoteUserService", remoteUserService);
        setField(service, "finSaleRecordMapper", saleMapper);
        setField(service, "finExpenseMapper", expenseMapper);
        setField(service, "finProfitShareRecordMapper", profitShareMapper);
        return service;
    }

    // -- test 1: getSalesDetail filters unauthorized deptIds for non-admin --

    @Test
    void getSalesDetail_nonAdmin_filtersUnauthorizedDeptIds() throws Exception
    {
        setupNonAdmin("store-mgr", 10L);

        ConfigurableRemoteUserService remoteService = new ConfigurableRemoteUserService();
        remoteService.deptListResponse = R.ok(Collections.singletonList(makeDept(10L)));

        FinanceDrillDownServiceImpl service = createService(
                new NoOpSaleRecordMapper(),
                new NoOpExpenseMapper(),
                new NoOpProfitShareRecordMapper(),
                remoteService);

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(new ArrayList<>(Arrays.asList(10L, 20L, 30L)));

        service.getSalesDetail(params);

        assertEquals(Collections.singletonList(10L), params.getDeptIds(),
                "Non-admin requesting [10,20,30] with allowed [10] should be filtered to [10]");
    }

    // -- test 2: getExpensesDetail filters unauthorized deptIds for non-admin --

    @Test
    void getExpensesDetail_nonAdmin_filtersUnauthorizedDeptIds() throws Exception
    {
        setupNonAdmin("store-mgr", 10L);

        ConfigurableRemoteUserService remoteService = new ConfigurableRemoteUserService();
        remoteService.deptListResponse = R.ok(Collections.singletonList(makeDept(10L)));

        FinanceDrillDownServiceImpl service = createService(
                new NoOpSaleRecordMapper(),
                new NoOpExpenseMapper(),
                new NoOpProfitShareRecordMapper(),
                remoteService);

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(new ArrayList<>(Arrays.asList(10L, 99L)));

        service.getExpensesDetail(params);

        assertEquals(Collections.singletonList(10L), params.getDeptIds(),
                "Non-admin requesting [10,99] with allowed [10] should be filtered to [10]");
    }

    // -- test 3: getProfitShareDetail filters unauthorized deptIds for non-admin --

    @Test
    void getProfitShareDetail_nonAdmin_filtersUnauthorizedDeptIds() throws Exception
    {
        setupNonAdmin("store-mgr", 10L);

        ConfigurableRemoteUserService remoteService = new ConfigurableRemoteUserService();
        remoteService.deptListResponse = R.ok(Collections.singletonList(makeDept(10L)));

        FinanceDrillDownServiceImpl service = createService(
                new NoOpSaleRecordMapper(),
                new NoOpExpenseMapper(),
                new NoOpProfitShareRecordMapper(),
                remoteService);

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(new ArrayList<>(Arrays.asList(10L, 88L)));

        service.getProfitShareDetail(params);

        assertEquals(Collections.singletonList(10L), params.getDeptIds(),
                "Non-admin requesting [10,88] with allowed [10] should be filtered to [10]");
    }

    // -- test 4: getSalesDetail filterSummary contains date and deptCount for admin --

    @Test
    void getDrilldown_filterSummary_containsDateAndDeptCount() throws Exception
    {
        setupAdmin();

        FinanceDrillDownServiceImpl service = createService(
                new NoOpSaleRecordMapper(),
                new NoOpExpenseMapper(),
                new NoOpProfitShareRecordMapper(),
                new ConfigurableRemoteUserService());

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(new ArrayList<>(Arrays.asList(10L, 20L, 30L)));
        Date startTime = new GregorianCalendar(2026, Calendar.JUNE, 1).getTime();
        Date endTime = new GregorianCalendar(2026, Calendar.JUNE, 30).getTime();
        params.setStartTime(startTime);
        params.setEndTime(endTime);

        DrillDownDetailVO vo = service.getSalesDetail(params);

        Map<String, Object> filterSummary = vo.getFilterSummary();
        assertNotNull(filterSummary, "filterSummary should not be null");
        assertEquals(3, filterSummary.get("deptCount"), "deptCount should be 3");
        assertEquals(startTime, filterSummary.get("startTime"), "startTime should match");
        assertEquals(endTime, filterSummary.get("endTime"), "endTime should match");
        assertTrue(filterSummary.containsKey("timeType"), "timeType key should be present in filterSummary");
    }

    // ========== Fake implementations ==========

    // -- ConfigurableRemoteUserService --

    static class ConfigurableRemoteUserService implements RemoteUserService
    {
        @Override public R<Boolean> isWechatLoginEnabled(Long tenantId, String source) { return R.ok(false); }
        R<List<SysDept>> deptListResponse = R.ok(Collections.emptyList());

        @Override
        public R<LoginUser> getUserInfo(String username, String source) { return R.fail(); }

        @Override
        public R<Boolean> registerUserInfo(SysUser sysUser, String source) { return R.fail(); }

        @Override
        public R<Boolean> recordUserLogin(SysUser sysUser, String source) { return R.fail(); }

        @Override
        public R<List<SysDept>> getUserDeptList(String username, String source)
        {
            return deptListResponse;
        }

        @Override
        public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) { return R.fail(); }
    }

    // -- NoOpSaleRecordMapper --

    static class NoOpSaleRecordMapper implements FinSaleRecordMapper
    {
        @Override
        public FinSaleRecord selectFinSaleRecordBySaleId(Long saleId) { return null; }

        @Override
        public FinSaleRecord selectFinSaleRecordBySaleIdForUpdate(Long saleId) { return selectFinSaleRecordBySaleId(saleId); }

        @Override
        public List<FinSaleRecord> selectFinSaleRecordList(FinSaleRecord finSaleRecord) { return Collections.emptyList(); }

        @Override
        public int insertFinSaleRecord(FinSaleRecord finSaleRecord) { return 0; }

        @Override
        public int updateFinSaleRecord(FinSaleRecord finSaleRecord) { return 0; }
        @Override public int updatePaidAmountAndStatus(Long saleId, java.math.BigDecimal paidAmount, String status) { return 0; }
        @Override public java.util.List<FinSaleRecord> selectReceivableList(FinSaleRecord r) { return java.util.Collections.emptyList(); }
        @Override public int countReceivableByPeriodId(Long deptId, Long periodId) { return 0; }
        @Override public java.math.BigDecimal sumReceivableByPeriodId(Long deptId, Long periodId) { return java.math.BigDecimal.ZERO; }

        @Override
        public int deleteFinSaleRecordBySaleId(Long saleId) { return 0; }

        @Override
        public int deleteFinSaleRecordBySaleIds(Long[] saleIds) { return 0; }

        @Override
        public List<Map<String, Object>> selectSaleTrendStats(List<Long> deptIds, Date startTime, Date endTime)
        {
            return Collections.emptyList();
        }

        @Override
        public int countSaleRecords(List<Long> deptIds, Date startTime, Date endTime) { return 0; }

        @Override
        public int sumSaleQuantity(List<Long> deptIds, Date startTime, Date endTime) { return 0; }

        @Override
        public FinSaleRecord checkSaleNoUnique(String saleNo) { return null; }

        @Override
        public int countTodaySales() { return 0; }
        public int maxTodaySaleSeq() { return 0; }

        @Override
        public BigDecimal selectTodayTotalSales(List<Long> deptIds) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectMonthTotalSales(List<Long> deptIds) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectTodayTotalSalesForPrev(List<Long> deptIds) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectMonthTotalSalesForPrev(List<Long> deptIds) { return BigDecimal.ZERO; }

        @Override
        public List<Map<String, Object>> selectSalesByDept(List<Long> deptIds, Date startTime, Date endTime)
        {
            return Collections.emptyList();
        }

        @Override
        public List<Map<String, Object>> selectProductSalesRank(List<Long> deptIds, Date startTime, Date endTime)
        {
            return Collections.emptyList();
        }

        @Override
        public BigDecimal selectMemberSales(List<Long> deptIds, Date startTime, Date endTime) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectSeckillSales(List<Long> deptIds, Date startTime, Date endTime) { return BigDecimal.ZERO; }

        @Override public BigDecimal selectCurrentPeriodPaymentTotal(List<Long> deptIds, Long periodId) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectHistoricalReceivableCollected(List<Long> deptIds, Long periodId) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectCurrentPeriodNewReceivable(List<Long> deptIds, Long periodId) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectEndingReceivableBalance(List<Long> deptIds) { return BigDecimal.ZERO; }

        @Override
        public int countOverdueReceivable(List<Long> deptIds) { return 0; }
    }

    static class NoOpExpenseMapper implements FinExpenseMapper
    {
        @Override public List<FinExpense> selectFinExpenseByExpenseIdsScoped(List<Long> ids, Long tenantId, Long deptId) { return Collections.emptyList(); }
        @Override public int markExpenseVerified(Long id, Long advanceId, String by, Date time, Long tenantId, Long deptId) { return 1; }
        @Override public int restoreExpenseUnverified(Long id) { return 1; }
        @Override
        public FinExpense selectFinExpenseByExpenseId(Long expenseId) { return null; }

        @Override
        public List<FinExpense> selectFinExpenseList(FinExpense finExpense) { return Collections.emptyList(); }

        @Override
        public int insertFinExpense(FinExpense finExpense) { return 0; }

        @Override
        public int updateFinExpense(FinExpense finExpense) { return 0; }

        @Override
        public int deleteFinExpenseByExpenseId(Long expenseId) { return 0; }

        @Override
        public int deleteFinExpenseByExpenseIds(Long[] expenseIds) { return 0; }

        @Override
        public FinExpense checkExpenseNoUnique(String expenseNo) { return null; }

        @Override
        public int countTodayExpenses() { return 0; }

        @Override
        public int maxTodayExpenseSeq() { return 0; }

        @Override
        public BigDecimal sumUnverifiedExpenses() { return BigDecimal.ZERO; }

        @Override
        public BigDecimal sumUnverifiedExpensesByDeptId(Long deptId) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal sumAllExpenses() { return BigDecimal.ZERO; }

        @Override
        public BigDecimal sumAllExpensesByDeptId(Long deptId) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal sumAllExpensesByPeriodId(Long periodId) { return BigDecimal.ZERO; }

        @Override
        public List<FinExpense> selectFinExpenseByExpenseIds(Long[] expenseIds) { return Collections.emptyList(); }

        @Override
        public List<Map<String, Object>> selectExpenseCategoryStats(Map<String, Object> params)
        {
            return Collections.emptyList();
        }

        @Override
        public List<Map<String, Object>> selectExpenseTrendStats(Map<String, Object> params)
        {
            return Collections.emptyList();
        }

        @Override
        public List<Map<String, Object>> selectExpenseDeptStats(Map<String, Object> params)
        {
            return Collections.emptyList();
        }

        @Override
        public BigDecimal selectExpenseTotal(Map<String, Object> params) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectTodayTotalExpense(List<Long> deptIds) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectMonthTotalExpense(List<Long> deptIds) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectMonthTotalExpenseForPrev(List<Long> deptIds) { return BigDecimal.ZERO; }

        @Override
        public int countUnverifiedExpenses(List<Long> deptIds) { return 0; }

        @Override
        public BigDecimal sumUnverifiedExpenseAmount(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public int countUnverifiedExpensesByPeriodId(List<Long> deptIds, Long periodId) { return 0; }
        @Override public BigDecimal sumUnverifiedExpenseAmountByPeriodId(List<Long> deptIds, Long periodId) { return BigDecimal.ZERO; }

        @Override
        public List<Map<String, Object>> selectExpenseCategoryStatsWithPrev(List<Long> deptIds, Date startTime, Date endTime, Date prevStartTime, Date prevEndTime)
        {
            return Collections.emptyList();
        }

        @Override
        public List<Map<String, Object>> selectUnverifiedExpenseList(List<Long> deptIds) { return Collections.emptyList(); }

        @Override
        public List<Map<String, Object>> selectOcrAnomalies(List<Long> deptIds) { return Collections.emptyList(); }
    }

    // -- NoOpProfitShareRecordMapper --

    static class NoOpProfitShareRecordMapper implements FinProfitShareRecordMapper
    {
        @Override
        public FinProfitShareRecord selectFinProfitShareRecordByShareId(Long shareId) { return null; }

        @Override
        public FinProfitShareRecord selectFinProfitShareRecordByPeriodId(Long periodId) { return null; }

        @Override
        public List<FinProfitShareRecord> selectFinProfitShareRecordList(FinProfitShareRecord finProfitShareRecord)
        {
            return Collections.emptyList();
        }

        @Override
        public int insertFinProfitShareRecord(FinProfitShareRecord finProfitShareRecord) { return 0; }

        @Override
        public int updateFinProfitShareRecord(FinProfitShareRecord finProfitShareRecord) { return 0; }

        @Override
        public int deleteFinProfitShareRecordByShareId(Long shareId) { return 0; }

        @Override
        public int deleteFinProfitShareRecordByShareIds(Long[] shareIds) { return 0; }

        @Override
        public BigDecimal selectProfitShareTotal(Map<String, Object> params) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectManagerProfitTotal(Map<String, Object> params) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectInvestorProfitTotal(Map<String, Object> params) { return BigDecimal.ZERO; }

        @Override
        public List<Map<String, Object>> selectManagerProfitByDept(Map<String, Object> params)
        {
            return Collections.emptyList();
        }

        @Override
        public List<Map<String, Object>> selectInvestorProfitByDept(Map<String, Object> params)
        {
            return Collections.emptyList();
        }

        @Override
        public List<Map<String, Object>> selectProfitShareTrend(Map<String, Object> params)
        {
            return Collections.emptyList();
        }

        @Override
        public int countUnsettledRecords(List<Long> deptIds) { return 0; }
        @Override public int countUnsettledRecordsByPeriodId(List<Long> deptIds, Long periodId) { return 0; }

        @Override
        public List<Map<String, Object>> selectSettlementByDept(List<Long> deptIds, Date startTime, Date endTime)
        {
            return Collections.emptyList();
        }

        @Override
        public BigDecimal selectPaidAmount(List<Long> deptIds, Date startTime, Date endTime) { return BigDecimal.ZERO; }
        @Override public int updateShareTimeByPeriodId(Long periodId, Date shareTime, String updateBy, String remark) { return 0; }
    }
}
