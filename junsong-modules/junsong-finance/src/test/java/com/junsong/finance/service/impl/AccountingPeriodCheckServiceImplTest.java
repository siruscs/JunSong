package com.junsong.finance.service.impl;

import com.junsong.finance.domain.*;
import com.junsong.finance.domain.vo.AccountingPeriodCheckItemVO;
import com.junsong.finance.domain.vo.AccountingPeriodCheckResultVO;
import com.junsong.finance.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AccountingPeriodCheckServiceImpl.checkBeforeLock()
 * Uses hand-written fakes (no Mockito).
 */
class AccountingPeriodCheckServiceImplTest
{
    private AccountingPeriodCheckServiceImpl service;
    private FakeExpenseMapper expenseMapper;
    private FakeAdvanceMapper advanceMapper;
    private FakeProfitShareRecordMapper profitShareMapper;
    private FakeInvestorPaymentMapper investorPaymentMapper;
    private FakeAccountingPeriodMapper periodMapper;
    private FakeSaleRecordMapper saleRecordMapper;

    @BeforeEach
    void setUp() throws Exception
    {
        service = new AccountingPeriodCheckServiceImpl();
        expenseMapper = new FakeExpenseMapper();
        advanceMapper = new FakeAdvanceMapper();
        profitShareMapper = new FakeProfitShareRecordMapper();
        investorPaymentMapper = new FakeInvestorPaymentMapper();
        periodMapper = new FakeAccountingPeriodMapper();
        saleRecordMapper = new FakeSaleRecordMapper();

        setField(service, "finExpenseMapper", expenseMapper);
        setField(service, "finAdvanceMapper", advanceMapper);
        setField(service, "finProfitShareRecordMapper", profitShareMapper);
        setField(service, "finInvestorPaymentMapper", investorPaymentMapper);
        setField(service, "finAccountingPeriodMapper", periodMapper);
        setField(service, "finSaleRecordMapper", saleRecordMapper);
    }

    private static void setField(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ── Test 1: BLOCK exists → canLock=false ──

    @Test
    void checkBeforeLock_blockItemExists_canLockFalse()
    {
        profitShareMapper.unsettledCount = 3;

        AccountingPeriodCheckResultVO result = service.checkBeforeLock(100L);

        assertFalse(result.isCanLock(), "Should not allow lock when BLOCK item exists");
        AccountingPeriodCheckItemVO blockItem = result.getItems().stream()
                .filter(i -> "UNSETTLED_PROFIT_SHARE".equals(i.getCheckType()))
                .findFirst().orElseThrow();
        assertEquals("BLOCK", blockItem.getLevel());
        assertEquals(3, blockItem.getCount());
    }

    // ── Test 2: Only WARNING → canLock=true, hasWarning=true ──

    @Test
    void checkBeforeLock_onlyWarning_canLockTrueHasWarningTrue()
    {
        expenseMapper.unverifiedCount = 2;
        expenseMapper.unverifiedAmount = new BigDecimal("1500.00");

        AccountingPeriodCheckResultVO result = service.checkBeforeLock(100L);

        assertTrue(result.isCanLock(), "Should allow lock when only WARNING items exist");
        assertTrue(result.isHasWarning(), "Should have warning flag set");

        AccountingPeriodCheckItemVO warningItem = result.getItems().stream()
                .filter(i -> "UNVERIFIED_EXPENSE".equals(i.getCheckType()))
                .findFirst().orElseThrow();
        assertEquals("WARNING", warningItem.getLevel());
        assertEquals(2, warningItem.getCount());
    }

    // ── Test 3: All clean → canLock=true, hasWarning=false ──

    @Test
    void checkBeforeLock_allClean_canLockTrueHasWarningFalse()
    {
        // All mappers return zero by default

        AccountingPeriodCheckResultVO result = service.checkBeforeLock(100L);

        assertTrue(result.isCanLock(), "Should allow lock when all clean");
        assertFalse(result.isHasWarning(), "Should not have warning flag");
        assertEquals(5, result.getItems().size(), "Should have 5 check items");

        // All items should have count = 0
        for (AccountingPeriodCheckItemVO item : result.getItems())
        {
            assertEquals(0, item.getCount(), "Item " + item.getCheckType() + " should have count 0");
        }
    }

    // ── Test 4: INFO only → canLock=true, hasWarning=false ──

    @Test
    void checkBeforeLock_infoOnly_canLockTrueHasWarningFalse()
    {
        FinAdvance advance = new FinAdvance();
        advance.setAdvanceAmount(new BigDecimal("500.00"));
        advanceMapper.advances = Collections.singletonList(advance);

        AccountingPeriodCheckResultVO result = service.checkBeforeLock(100L);

        assertTrue(result.isCanLock(), "Should allow lock when only INFO items exist");
        assertFalse(result.isHasWarning(), "Should not have warning flag for INFO items");

        AccountingPeriodCheckItemVO infoItem = result.getItems().stream()
                .filter(i -> "UNVERIFIED_ADVANCE".equals(i.getCheckType()))
                .findFirst().orElseThrow();
        assertEquals("INFO", infoItem.getLevel());
        assertEquals(1, infoItem.getCount());
        assertEquals(0, new BigDecimal("500.00").compareTo(infoItem.getAmount()));
    }

    // ── Test 5: BLOCK + WARNING → canLock=false, hasWarning=true ──

    @Test
    void checkBeforeLock_blockAndWarning_canLockFalseHasWarningTrue()
    {
        profitShareMapper.unsettledCount = 1;
        expenseMapper.unverifiedCount = 5;
        expenseMapper.unverifiedAmount = new BigDecimal("3000.00");

        AccountingPeriodCheckResultVO result = service.checkBeforeLock(100L);

        assertFalse(result.isCanLock(), "BLOCK should prevent lock");
        assertTrue(result.isHasWarning(), "WARNING should still be flagged");
    }

    // ── Test 6: Period info is populated ──

    @Test
    void checkBeforeLock_periodInfoPopulated()
    {
        FinAccountingPeriod period = new FinAccountingPeriod();
        period.setPeriodId(42L);
        period.setPeriodNo("AP20260701001");
        periodMapper.currentPeriod = period;

        AccountingPeriodCheckResultVO result = service.checkBeforeLock(100L);

        assertEquals(42L, result.getPeriodId());
        assertEquals("AP20260701001", result.getPeriodName());
        assertEquals(100L, result.getDeptId());
    }

    // ── Test 7: Unpaid investor payment is WARNING ──

    @Test
    void checkBeforeLock_unpaidInvestorPayment_isWarning()
    {
        FinInvestorPayment payment = new FinInvestorPayment();
        payment.setAmount(new BigDecimal("2000.00"));
        investorPaymentMapper.payments = Collections.singletonList(payment);

        AccountingPeriodCheckResultVO result = service.checkBeforeLock(100L);

        assertTrue(result.isCanLock(), "WARNING should still allow lock");
        assertTrue(result.isHasWarning(), "Unpaid investor should set warning");

        AccountingPeriodCheckItemVO item = result.getItems().stream()
                .filter(i -> "UNPAID_INVESTOR".equals(i.getCheckType()))
                .findFirst().orElseThrow();
        assertEquals("WARNING", item.getLevel());
        assertEquals(1, item.getCount());
    }

    // ── Test 7b: Unsettled sale receivable is WARNING (never blocks lock) ──

    @Test
    void checkBeforeLock_unsettledReceivable_isWarning()
    {
        FinAccountingPeriod currentPeriod = new FinAccountingPeriod();
        currentPeriod.setPeriodId(100L);
        currentPeriod.setPeriodNo("AP20260701100");
        periodMapper.currentPeriod = currentPeriod;

        saleRecordMapper.periodReceivableCount.put(100L, 3);
        saleRecordMapper.periodReceivableAmount.put(100L, new BigDecimal("2500.00"));

        AccountingPeriodCheckResultVO result = service.checkBeforeLock(100L);

        assertTrue(result.isCanLock(), "未缴清销售单只是 WARNING，不应阻断结转");
        assertTrue(result.isHasWarning(), "未缴清销售单应置 warning 标志");

        AccountingPeriodCheckItemVO item = result.getItems().stream()
                .filter(i -> "UNSETTLED_RECEIVABLE".equals(i.getCheckType()))
                .findFirst().orElseThrow();
        assertEquals("WARNING", item.getLevel());
        assertEquals(3, item.getCount());
        assertEquals(0, new BigDecimal("2500.00").compareTo(item.getAmount()));
    }

    @Test
    void checkBeforeLock_filtersByCurrentPeriodId()
    {
        // Current period has no unsettled profit share
        FinAccountingPeriod currentPeriod = new FinAccountingPeriod();
        currentPeriod.setPeriodId(100L);
        currentPeriod.setPeriodNo("AP20260701100");
        periodMapper.currentPeriod = currentPeriod;

        // Historical period has 5 unsettled records, but current period has 0
        profitShareMapper.periodUnsettled.put(100L, 0);   // current period: clean
        profitShareMapper.periodUnsettled.put(99L, 5);    // historical period: 5 unsettled
        profitShareMapper.unsettledCount = 5;             // across all periods: 5

        AccountingPeriodCheckResultVO result = service.checkBeforeLock(100L);

        assertTrue(result.isCanLock(), "Should allow lock when current period has no BLOCK items");
        AccountingPeriodCheckItemVO blockItem = result.getItems().stream()
                .filter(i -> "UNSETTLED_PROFIT_SHARE".equals(i.getCheckType()))
                .findFirst().orElseThrow();
        assertEquals(0, blockItem.getCount(), "BLOCK item count should be 0 for current period");
    }

    // ── Test 9: Expense filtered by current periodId (TRAE-R3-04) ──

    @Test
    void checkBeforeLock_expenseFilteredByCurrentPeriodId()
    {
        FinAccountingPeriod currentPeriod = new FinAccountingPeriod();
        currentPeriod.setPeriodId(77L);
        currentPeriod.setPeriodNo("AP202607077");
        periodMapper.currentPeriod = currentPeriod;

        // Dept-wide: 10 unverified expenses / 5000 amount
        expenseMapper.unverifiedCount = 10;
        expenseMapper.unverifiedAmount = new BigDecimal("5000.00");
        // Current period (77): 2 unverified expenses / 800 amount
        expenseMapper.periodUnverifiedCount.put(77L, 2);
        expenseMapper.periodUnverifiedAmount.put(77L, new BigDecimal("800.00"));

        AccountingPeriodCheckResultVO result = service.checkBeforeLock(100L);

        AccountingPeriodCheckItemVO item = result.getItems().stream()
                .filter(i -> "UNVERIFIED_EXPENSE".equals(i.getCheckType()))
                .findFirst().orElseThrow();
        assertEquals(2, item.getCount(), "Expense count should be filtered by current periodId");
        assertEquals(0, new BigDecimal("800.00").compareTo(item.getAmount()),
                "Expense amount should be filtered by current periodId");
    }

    // ── Test 10: Advance filtered by current periodId ──

    @Test
    void checkBeforeLock_advanceFilteredByCurrentPeriodId()
    {
        FinAccountingPeriod currentPeriod = new FinAccountingPeriod();
        currentPeriod.setPeriodId(88L);
        currentPeriod.setPeriodNo("AP202607088");
        periodMapper.currentPeriod = currentPeriod;

        // Two advances: one in current period (88), one in historical period (99)
        FinAdvance currentAdvance = new FinAdvance();
        currentAdvance.setAdvanceAmount(new BigDecimal("300.00"));
        currentAdvance.setPeriodId(88L);

        FinAdvance historicalAdvance = new FinAdvance();
        historicalAdvance.setAdvanceAmount(new BigDecimal("700.00"));
        historicalAdvance.setPeriodId(99L);

        advanceMapper.advances = Arrays.asList(currentAdvance, historicalAdvance);

        AccountingPeriodCheckResultVO result = service.checkBeforeLock(100L);

        AccountingPeriodCheckItemVO item = result.getItems().stream()
                .filter(i -> "UNVERIFIED_ADVANCE".equals(i.getCheckType()))
                .findFirst().orElseThrow();
        assertEquals(1, item.getCount(), "Advance count should be filtered by current periodId");
        assertEquals(0, new BigDecimal("300.00").compareTo(item.getAmount()),
                "Advance amount should only include current-period records");
    }

    // ── Test 11: Investor payment filtered by current periodId ──

    @Test
    void checkBeforeLock_investorPaymentFilteredByCurrentPeriodId()
    {
        FinAccountingPeriod currentPeriod = new FinAccountingPeriod();
        currentPeriod.setPeriodId(66L);
        currentPeriod.setPeriodNo("AP202607066");
        periodMapper.currentPeriod = currentPeriod;

        // Two payments: one in current period (66), one in historical period (55)
        FinInvestorPayment currentPayment = new FinInvestorPayment();
        currentPayment.setAmount(new BigDecimal("2000.00"));
        currentPayment.setPeriodId(66L);

        FinInvestorPayment historicalPayment = new FinInvestorPayment();
        historicalPayment.setAmount(new BigDecimal("5000.00"));
        historicalPayment.setPeriodId(55L);

        investorPaymentMapper.payments = Arrays.asList(currentPayment, historicalPayment);

        AccountingPeriodCheckResultVO result = service.checkBeforeLock(100L);

        AccountingPeriodCheckItemVO item = result.getItems().stream()
                .filter(i -> "UNPAID_INVESTOR".equals(i.getCheckType()))
                .findFirst().orElseThrow();
        assertEquals(1, item.getCount(), "Investor payment count should be filtered by current periodId");
        assertEquals(0, new BigDecimal("2000.00").compareTo(item.getAmount()),
                "Investor payment amount should only include current-period records");
    }

    // ── Test 12: No current period → fall back to dept-wide queries ──

    @Test
    void checkBeforeLock_noCurrentPeriod_fallsBackToDeptWide()
    {
        // No current period set (periodMapper.currentPeriod = null by default)
        expenseMapper.unverifiedCount = 7;
        expenseMapper.unverifiedAmount = new BigDecimal("3500.00");

        FinAdvance advance = new FinAdvance();
        advance.setAdvanceAmount(new BigDecimal("400.00"));
        advanceMapper.advances = Collections.singletonList(advance);

        FinInvestorPayment payment = new FinInvestorPayment();
        payment.setAmount(new BigDecimal("1000.00"));
        investorPaymentMapper.payments = Collections.singletonList(payment);

        AccountingPeriodCheckResultVO result = service.checkBeforeLock(100L);

        // Without a current period, checks should use dept-wide (all-history) queries
        AccountingPeriodCheckItemVO expenseItem = result.getItems().stream()
                .filter(i -> "UNVERIFIED_EXPENSE".equals(i.getCheckType()))
                .findFirst().orElseThrow();
        assertEquals(7, expenseItem.getCount(), "Without period, expense count should be dept-wide");

        AccountingPeriodCheckItemVO advanceItem = result.getItems().stream()
                .filter(i -> "UNVERIFIED_ADVANCE".equals(i.getCheckType()))
                .findFirst().orElseThrow();
        assertEquals(1, advanceItem.getCount(), "Without period, advance count should be dept-wide");

        AccountingPeriodCheckItemVO paymentItem = result.getItems().stream()
                .filter(i -> "UNPAID_INVESTOR".equals(i.getCheckType()))
                .findFirst().orElseThrow();
        assertEquals(1, paymentItem.getCount(), "Without period, investor payment count should be dept-wide");
    }

    // ── Fake mappers ──

    static class FakeExpenseMapper implements FinExpenseMapper
    {
        @Override public List<FinExpense> selectFinExpenseByExpenseIdsScoped(List<Long> ids, Long tenantId, Long deptId) { return Collections.emptyList(); }
        @Override public int markExpenseVerified(Long id, Long advanceId, String by, Date time, Long tenantId, Long deptId) { return 1; }
        @Override public int restoreExpenseUnverified(Long id) { return 1; }
        int unverifiedCount = 0;
        BigDecimal unverifiedAmount = BigDecimal.ZERO;
        Map<Long, Integer> periodUnverifiedCount = new HashMap<>();
        Map<Long, BigDecimal> periodUnverifiedAmount = new HashMap<>();

        @Override public int countUnverifiedExpenses(List<Long> deptIds) { return unverifiedCount; }
        @Override public BigDecimal sumUnverifiedExpenseAmount(List<Long> deptIds) { return unverifiedAmount; }
        @Override public int countUnverifiedExpensesByPeriodId(List<Long> deptIds, Long periodId) {
            Integer c = periodUnverifiedCount.get(periodId);
            return c != null ? c : unverifiedCount;
        }
        @Override public BigDecimal sumUnverifiedExpenseAmountByPeriodId(List<Long> deptIds, Long periodId) {
            BigDecimal a = periodUnverifiedAmount.get(periodId);
            return a != null ? a : unverifiedAmount;
        }

        // Unused methods — stubs
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
        @Override public List<Map<String, Object>> selectExpenseCategoryStats(Map<String, Object> p) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectExpenseTrendStats(Map<String, Object> p) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectExpenseDeptStats(Map<String, Object> p) { return Collections.emptyList(); }
        @Override public BigDecimal selectExpenseTotal(Map<String, Object> p) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTodayTotalExpense(List<Long> d) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalExpense(List<Long> d) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalExpenseForPrev(List<Long> d) { return BigDecimal.ZERO; }
        @Override public List<Map<String, Object>> selectExpenseCategoryStatsWithPrev(List<Long> d, Date s, Date e, Date ps, Date pe) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectUnverifiedExpenseList(List<Long> d) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectOcrAnomalies(List<Long> d) { return Collections.emptyList(); }
    }

    static class FakeAdvanceMapper implements FinAdvanceMapper
    {
        @Override public List<FinAdvance> selectFinAdvanceByAdvanceIdsScoped(List<Long> ids, Long tenantId, Long deptId) { return Collections.emptyList(); }
        @Override public int markAdvanceVerified(Long id, String by, Date time, Long tenantId, Long deptId) { return 1; }
        @Override public int restoreAdvanceStatus(Long id, String status, String by, Date time) { return 1; }
        List<FinAdvance> advances = Collections.emptyList();

        @Override public List<FinAdvance> selectFinAdvanceList(FinAdvance q) {
            if (q.getPeriodId() != null) {
                return advances.stream()
                        .filter(a -> q.getPeriodId().equals(a.getPeriodId()))
                        .collect(Collectors.toList());
            }
            return advances;
        }

        // Unused methods — stubs
        @Override public FinAdvance selectFinAdvanceByAdvanceId(Long id) { return null; }
        @Override public int insertFinAdvance(FinAdvance a) { return 0; }
        @Override public int updateFinAdvance(FinAdvance a) { return 0; }
        @Override public int deleteFinAdvanceByAdvanceId(Long id) { return 0; }
        @Override public int deleteFinAdvanceByAdvanceIds(Long[] ids) { return 0; }
        @Override public FinAdvance checkAdvanceNoUnique(String no) { return null; }
        @Override public int countTodayAdvances() { return 0; }
        @Override public BigDecimal sumUnverifiedAdvances() { return BigDecimal.ZERO; }
        @Override public BigDecimal sumUnverifiedAdvancesByDeptId(Long deptId) { return BigDecimal.ZERO; }
        @Override public List<FinAdvance> selectFinAdvanceByAdvanceIds(Long[] ids) { return Collections.emptyList(); }
    }

    static class FakeProfitShareRecordMapper implements FinProfitShareRecordMapper
    {
        int unsettledCount = 0;
        Map<Long, Integer> periodUnsettled = new HashMap<>();

        @Override public int countUnsettledRecords(List<Long> deptIds) { return unsettledCount; }
        @Override public int countUnsettledRecordsByPeriodId(List<Long> deptIds, Long periodId) {
            Integer count = periodUnsettled.get(periodId);
            return count != null ? count : unsettledCount;
        }

        // Unused methods — stubs
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
        @Override public List<Map<String, Object>> selectSettlementByDept(List<Long> d, Date s, Date e) { return Collections.emptyList(); }
        @Override public BigDecimal selectPaidAmount(List<Long> d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public int updateShareTimeByPeriodId(Long periodId, Date shareTime, String updateBy, String remark) { return 0; }
    }

    static class FakeInvestorPaymentMapper implements FinInvestorPaymentMapper
    {
        List<FinInvestorPayment> payments = Collections.emptyList();

        @Override public List<FinInvestorPayment> selectFinInvestorPaymentList(FinInvestorPayment q) {
            if (q.getPeriodId() != null) {
                return payments.stream()
                        .filter(p -> q.getPeriodId().equals(p.getPeriodId()))
                        .collect(Collectors.toList());
            }
            return payments;
        }

        // Unused methods — stubs
        @Override public FinInvestorPayment selectFinInvestorPaymentByPaymentId(Long id) { return null; }
        @Override public int insertFinInvestorPayment(FinInvestorPayment p) { return 0; }
        @Override public int updateFinInvestorPayment(FinInvestorPayment p) { return 0; }
        @Override public int deleteFinInvestorPaymentByPaymentId(Long id) { return 0; }
        @Override public int deleteFinInvestorPaymentByPaymentIds(Long[] ids) { return 0; }
        @Override public int deleteAutoInvestorPaymentByShareId(Long shareId) { return 0; }
        @Override public FinInvestorPayment checkPaymentNoUnique(String no) { return null; }
        @Override public int countTodayInvestorPayments() { return 0; }
        @Override public BigDecimal sumReserveFund() { return BigDecimal.ZERO; }
        @Override public BigDecimal sumVerifiedExpenses() { return BigDecimal.ZERO; }
        @Override public BigDecimal sumVerifiedExpensesByDeptId(Long deptId) { return BigDecimal.ZERO; }
        @Override public BigDecimal sumTotalInvestAmount() { return BigDecimal.ZERO; }
        @Override public BigDecimal sumTotalReturnAmount() { return BigDecimal.ZERO; }
        @Override public BigDecimal sumTotalReturnAmountByDeptId(Long deptId) { return BigDecimal.ZERO; }
    }

    static class FakeAccountingPeriodMapper implements FinAccountingPeriodMapper
    {
        public FinAccountingPeriod selectPeriodForUpdate(Long id, Long tenantId, Long deptId) { return selectFinAccountingPeriodByPeriodId(id); }
        FinAccountingPeriod currentPeriod = null;

        @Override public FinAccountingPeriod selectCurrentPeriodByDeptId(Long deptId) { return currentPeriod; }

        // Unused methods — stubs
        @Override public FinAccountingPeriod selectFinAccountingPeriodByPeriodId(Long id) { return null; }
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
        @Override public String selectCurrentPeriodStatusByDeptIds(List<Long> deptIds) { return null; }
        @Override public FinAccountingPeriod selectPeriodById(Long id) { return null; }
        @Override public FinAccountingPeriod selectPreviousPeriod(Long deptId, Date startTime, Long periodId) { return null; }
        @Override public FinAccountingPeriod selectNextPeriod(Long deptId, Date startTime, Long periodId) { return null; }
        @Override public int updateStartTimeOnly(Long periodId, Date startTime, Date endTime, String updateBy, String remark) { return 0; }

    @Override
    public com.junsong.finance.domain.FinAccountingPeriod selectCurrentPeriodByDeptIdForUpdate(Long tenantId, Long deptId) {
        return selectCurrentPeriodByDeptId(deptId);
    }

}

    static class FakeSaleRecordMapper implements FinSaleRecordMapper
    {
        Map<Long, Integer> periodReceivableCount = new HashMap<>();
        Map<Long, BigDecimal> periodReceivableAmount = new HashMap<>();

        @Override public int countReceivableByPeriodId(Long deptId, Long periodId) {
            Integer c = periodReceivableCount.get(periodId);
            return c != null ? c : 0;
        }
        @Override public BigDecimal sumReceivableByPeriodId(Long deptId, Long periodId) {
            BigDecimal a = periodReceivableAmount.get(periodId);
            return a != null ? a : BigDecimal.ZERO;
        }

        // Unused methods — stubs
        @Override public FinSaleRecord selectFinSaleRecordBySaleId(Long saleId) { return null; }
        @Override public FinSaleRecord selectFinSaleRecordBySaleIdForUpdate(Long saleId) { return selectFinSaleRecordBySaleId(saleId); }
        @Override public List<FinSaleRecord> selectFinSaleRecordList(FinSaleRecord r) { return Collections.emptyList(); }
        @Override public List<FinSaleRecord> selectReceivableList(FinSaleRecord r) { return Collections.emptyList(); }
        @Override public int insertFinSaleRecord(FinSaleRecord r) { return 0; }
        @Override public int updateFinSaleRecord(FinSaleRecord r) { return 0; }
        @Override public int updatePaidAmountAndStatus(Long saleId, BigDecimal paidAmount, String status) { return 0; }
        @Override public int deleteFinSaleRecordBySaleId(Long saleId) { return 0; }
        @Override public int deleteFinSaleRecordBySaleIds(Long[] saleIds) { return 0; }
        @Override public List<Map<String, Object>> selectSaleTrendStats(List<Long> d, Date s, Date e) { return Collections.emptyList(); }
        @Override public int countSaleRecords(List<Long> d, Date s, Date e) { return 0; }
        @Override public int sumSaleQuantity(List<Long> d, Date s, Date e) { return 0; }
        @Override public FinSaleRecord checkSaleNoUnique(String no) { return null; }
        @Override public int countTodaySales() { return 0; }
        @Override public int maxTodaySaleSeq() { return 0; }
        @Override public BigDecimal selectTodayTotalSales(List<Long> d) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalSales(List<Long> d) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTodayTotalSalesForPrev(List<Long> d) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalSalesForPrev(List<Long> d) { return BigDecimal.ZERO; }
        @Override public List<Map<String, Object>> selectSalesByDept(List<Long> d, Date s, Date e) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectProductSalesRank(List<Long> d, Date s, Date e) { return Collections.emptyList(); }
        @Override public BigDecimal selectMemberSales(List<Long> d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectSeckillSales(List<Long> d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectCurrentPeriodPaymentTotal(List<Long> deptIds, Long periodId) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectHistoricalReceivableCollected(List<Long> deptIds, Long periodId) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectCurrentPeriodNewReceivable(List<Long> deptIds, Long periodId) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectEndingReceivableBalance(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public int countOverdueReceivable(List<Long> deptIds) { return 0; }
    }
}
