package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.constant.PeriodStatus;
import com.junsong.finance.constant.VerifyStatus;
import com.junsong.finance.domain.*;
import com.junsong.finance.domain.vo.AccountingPeriodCheckItemVO;
import com.junsong.finance.domain.vo.AccountingPeriodCheckResultVO;
import com.junsong.finance.mapper.*;
import com.junsong.finance.service.IAccountingPeriodCheckService;
import com.junsong.finance.service.IFinAccountingPeriodService;
import com.junsong.finance.service.IFinProfitShareRecordService;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the accounting period lock guard (assertPeriodEditable)
 * prevents modifications on CARRIED periods and allows them on ACTIVE periods.
 */
class AccountingPeriodLockGuardTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.remove();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setFieldInClass(Object target, Class<?> clazz, String name, Object value) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setupAdmin() {
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
        LoginUser lu = new LoginUser();
        lu.setUserid(1L); lu.setUsername("admin"); lu.setDeptId(100L);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, lu);
    }

    // ── Tests: assertPeriodEditable on CARRIED period throws ──

    @Test
    void assertPeriodEditable_carriedPeriod_throwsServiceException() {
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        FakeAccountingPeriodMapperForLock mapper = new FakeAccountingPeriodMapperForLock();
        FinAccountingPeriod carriedPeriod = new FinAccountingPeriod();
        carriedPeriod.setPeriodId(1L);
        carriedPeriod.setStatus(PeriodStatus.CARRIED);
        mapper.periods.put(1L, carriedPeriod);

        try {
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", mapper);
        } catch (Exception e) {
            fail("Failed to inject mapper: " + e.getMessage());
        }

        assertThrows(ServiceException.class, () -> periodService.assertPeriodEditable(1L),
                "CARRIED period should throw ServiceException");
    }

    @Test
    void assertPeriodEditable_activePeriod_allowsOperation() {
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        FakeAccountingPeriodMapperForLock mapper = new FakeAccountingPeriodMapperForLock();
        FinAccountingPeriod activePeriod = new FinAccountingPeriod();
        activePeriod.setPeriodId(2L);
        activePeriod.setStatus(PeriodStatus.ACTIVE);
        mapper.periods.put(2L, activePeriod);

        try {
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", mapper);
        } catch (Exception e) {
            fail("Failed to inject mapper: " + e.getMessage());
        }

        // Should NOT throw
        assertDoesNotThrow(() -> periodService.assertPeriodEditable(2L),
                "ACTIVE period should allow operations");
    }

    @Test
    void assertPeriodEditable_nullPeriodId_allowsOperation() {
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        FakeAccountingPeriodMapperForLock mapper = new FakeAccountingPeriodMapperForLock();

        try {
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", mapper);
        } catch (Exception e) {
            fail("Failed to inject mapper: " + e.getMessage());
        }

        assertDoesNotThrow(() -> periodService.assertPeriodEditable(null),
                "null periodId should allow operations (skip check)");
    }

    @Test
    void assertPeriodEditable_nonExistentPeriod_allowsOperation() {
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        FakeAccountingPeriodMapperForLock mapper = new FakeAccountingPeriodMapperForLock();
        // No period with ID 999 exists

        try {
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", mapper);
        } catch (Exception e) {
            fail("Failed to inject mapper: " + e.getMessage());
        }

        assertDoesNotThrow(() -> periodService.assertPeriodEditable(999L),
                "Non-existent period should allow operations");
    }

    @Test
    void assertPeriodEditable_breakEvenPeriod_throwsServiceException() {
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        FakeAccountingPeriodMapperForLock mapper = new FakeAccountingPeriodMapperForLock();
        FinAccountingPeriod breakEvenPeriod = new FinAccountingPeriod();
        breakEvenPeriod.setPeriodId(3L);
        breakEvenPeriod.setStatus(PeriodStatus.BREAK_EVEN);
        mapper.periods.put(3L, breakEvenPeriod);

        try {
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", mapper);
        } catch (Exception e) {
            fail("Failed to inject mapper: " + e.getMessage());
        }

        assertThrows(ServiceException.class, () -> periodService.assertPeriodEditable(3L),
                "BREAK_EVEN period should also throw ServiceException");
    }

    // ── Error message quality tests ──

    @Test
    void assertPeriodEditable_carriedPeriod_errorMessageContainsPeriodNoAndStatusLabel() {
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        FakeAccountingPeriodMapperForLock mapper = new FakeAccountingPeriodMapperForLock();
        FinAccountingPeriod carriedPeriod = new FinAccountingPeriod();
        carriedPeriod.setPeriodId(5L);
        carriedPeriod.setPeriodNo("AP20260630120000100");
        carriedPeriod.setStatus(PeriodStatus.CARRIED);
        carriedPeriod.setCarryForwardBy("zhangsan");
        mapper.periods.put(5L, carriedPeriod);

        try {
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", mapper);
        } catch (Exception e) {
            fail("Failed to inject mapper: " + e.getMessage());
        }

        ServiceException ex = assertThrows(ServiceException.class, () -> periodService.assertPeriodEditable(5L));
        String msg = ex.getMessage();
        assertTrue(msg.contains("AP20260630120000100"), "Error should contain period number: " + msg);
        assertTrue(msg.contains("已结转"), "Error should contain status label '已结转': " + msg);
        assertTrue(msg.contains("zhangsan"), "Error should contain lock person: " + msg);
    }

    @Test
    void assertPeriodEditable_carriedPeriodWithoutLockPerson_noPersonInMessage() {
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        FakeAccountingPeriodMapperForLock mapper = new FakeAccountingPeriodMapperForLock();
        FinAccountingPeriod carriedPeriod = new FinAccountingPeriod();
        carriedPeriod.setPeriodId(6L);
        carriedPeriod.setPeriodNo("AP20260630120000200");
        carriedPeriod.setStatus(PeriodStatus.CARRIED);
        // carryForwardBy is null — no lock person
        mapper.periods.put(6L, carriedPeriod);

        try {
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", mapper);
        } catch (Exception e) {
            fail("Failed to inject mapper: " + e.getMessage());
        }

        ServiceException ex = assertThrows(ServiceException.class, () -> periodService.assertPeriodEditable(6L));
        String msg = ex.getMessage();
        assertTrue(msg.contains("已结转"), "Error should contain status label: " + msg);
        assertFalse(msg.contains("锁账人"), "Error should NOT mention lock person when carryForwardBy is null: " + msg);
    }

    // ── Integration: expense update on CARRIED period ──

    @Test
    void expenseUpdate_carriedPeriod_throwsServiceException() throws Exception {
        setupAdmin();
        FinExpenseServiceImpl expenseService = new FinExpenseServiceImpl();

        FakeAccountingPeriodMapperForLock periodMapper = new FakeAccountingPeriodMapperForLock();
        FinAccountingPeriod carriedPeriod = new FinAccountingPeriod();
        carriedPeriod.setPeriodId(10L);
        carriedPeriod.setStatus(PeriodStatus.CARRIED);
        periodMapper.periods.put(10L, carriedPeriod);

        // Create a fake period service
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", periodMapper);

        // Inject fake expense mapper with an existing expense record
        FakeExpenseMapperForLock expenseMapper = new FakeExpenseMapperForLock();
        FinExpense existingExpense = new FinExpense();
        existingExpense.setExpenseId(42L);
        existingExpense.setPeriodId(10L);
        existingExpense.setStatus(VerifyStatus.UNVERIFIED);
        existingExpense.setExpenseAmount(new BigDecimal("100.00"));
        expenseMapper.expenses.put(42L, existingExpense);

        setFieldInClass(expenseService, FinExpenseServiceImpl.class, "finExpenseMapper", expenseMapper);
        setFieldInClass(expenseService, FinExpenseServiceImpl.class, "finAccountingPeriodService", periodService);

        FinExpense updateReq = new FinExpense();
        updateReq.setExpenseId(42L);
        updateReq.setPeriodId(10L);

        assertThrows(ServiceException.class, () -> expenseService.updateFinExpense(updateReq),
                "Expense update on CARRIED period should throw");
    }

    // ── Integration: sale update on CARRIED period ──

    @Test
    void saleUpdate_carriedPeriod_throwsServiceException() throws Exception {
        setupAdmin();
        FinSaleRecordServiceImpl saleService = new FinSaleRecordServiceImpl();

        FakeAccountingPeriodMapperForLock periodMapper = new FakeAccountingPeriodMapperForLock();
        FinAccountingPeriod carriedPeriod = new FinAccountingPeriod();
        carriedPeriod.setPeriodId(10L);
        carriedPeriod.setStatus(PeriodStatus.CARRIED);
        periodMapper.periods.put(10L, carriedPeriod);

        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", periodMapper);

        FakeSaleMapperForLock saleMapper = new FakeSaleMapperForLock();
        FinSaleRecord existingSale = new FinSaleRecord();
        existingSale.setSaleId(55L);
        existingSale.setPeriodId(10L);
        existingSale.setSaleAmount(new BigDecimal("200.00"));
        saleMapper.sales.put(55L, existingSale);

        FakeSalePaymentMapperForLock paymentMapper = new FakeSalePaymentMapperForLock();

        setFieldInClass(saleService, FinSaleRecordServiceImpl.class, "finSaleRecordMapper", saleMapper);
        setFieldInClass(saleService, FinSaleRecordServiceImpl.class, "finSalePaymentMapper", paymentMapper);
        setFieldInClass(saleService, FinSaleRecordServiceImpl.class, "finAccountingPeriodService", periodService);

        FinSaleRecord updateReq = new FinSaleRecord();
        updateReq.setSaleId(55L);
        updateReq.setPeriodId(10L);

        assertThrows(ServiceException.class, () -> saleService.updateFinSaleRecord(updateReq),
                "Sale update on CARRIED period should throw");
    }

    // ── Integration: expense delete on CARRIED period ──

    @Test
    void expenseDelete_carriedPeriod_throwsServiceException() throws Exception {
        setupAdmin();
        FinExpenseServiceImpl expenseService = new FinExpenseServiceImpl();

        FakeAccountingPeriodMapperForLock periodMapper = new FakeAccountingPeriodMapperForLock();
        FinAccountingPeriod carriedPeriod = new FinAccountingPeriod();
        carriedPeriod.setPeriodId(10L);
        carriedPeriod.setStatus(PeriodStatus.CARRIED);
        periodMapper.periods.put(10L, carriedPeriod);

        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", periodMapper);

        FakeExpenseMapperForLock expenseMapper = new FakeExpenseMapperForLock();
        FinExpense existingExpense = new FinExpense();
        existingExpense.setExpenseId(42L);
        existingExpense.setPeriodId(10L);
        existingExpense.setStatus(VerifyStatus.UNVERIFIED);
        expenseMapper.expenses.put(42L, existingExpense);

        setFieldInClass(expenseService, FinExpenseServiceImpl.class, "finExpenseMapper", expenseMapper);
        setFieldInClass(expenseService, FinExpenseServiceImpl.class, "finAccountingPeriodService", periodService);

        assertThrows(ServiceException.class, () -> expenseService.deleteFinExpenseByExpenseId(42L),
                "Expense delete on CARRIED period should throw");
    }

    // ── Integration: sale delete on CARRIED period ──

    @Test
    void saleDelete_carriedPeriod_throwsServiceException() throws Exception {
        setupAdmin();
        FinSaleRecordServiceImpl saleService = new FinSaleRecordServiceImpl();

        FakeAccountingPeriodMapperForLock periodMapper = new FakeAccountingPeriodMapperForLock();
        FinAccountingPeriod carriedPeriod = new FinAccountingPeriod();
        carriedPeriod.setPeriodId(10L);
        carriedPeriod.setStatus(PeriodStatus.CARRIED);
        periodMapper.periods.put(10L, carriedPeriod);

        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", periodMapper);

        FakeSaleMapperForLock saleMapper = new FakeSaleMapperForLock();
        FinSaleRecord existingSale = new FinSaleRecord();
        existingSale.setSaleId(55L);
        existingSale.setPeriodId(10L);
        existingSale.setSaleAmount(new BigDecimal("200.00"));
        saleMapper.sales.put(55L, existingSale);

        FakeSalePaymentMapperForLock paymentMapper = new FakeSalePaymentMapperForLock();

        setFieldInClass(saleService, FinSaleRecordServiceImpl.class, "finSaleRecordMapper", saleMapper);
        setFieldInClass(saleService, FinSaleRecordServiceImpl.class, "finSalePaymentMapper", paymentMapper);
        setFieldInClass(saleService, FinSaleRecordServiceImpl.class, "finAccountingPeriodService", periodService);

        assertThrows(ServiceException.class, () -> saleService.deleteFinSaleRecordBySaleId(55L),
                "Sale delete on CARRIED period should throw");
    }

    // ── ACTIVE period allows all operations ──

    @Test
    void expenseUpdate_activePeriod_allowsOperation() throws Exception {
        setupAdmin();
        FinExpenseServiceImpl expenseService = new FinExpenseServiceImpl();

        FakeAccountingPeriodMapperForLock periodMapper = new FakeAccountingPeriodMapperForLock();
        FinAccountingPeriod activePeriod = new FinAccountingPeriod();
        activePeriod.setPeriodId(10L);
        activePeriod.setStatus(PeriodStatus.ACTIVE);
        periodMapper.periods.put(10L, activePeriod);

        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", periodMapper);

        FakeExpenseMapperForLock expenseMapper = new FakeExpenseMapperForLock();
        FinExpense existingExpense = new FinExpense();
        existingExpense.setExpenseId(42L);
        existingExpense.setPeriodId(10L);
        existingExpense.setStatus(VerifyStatus.UNVERIFIED);
        existingExpense.setExpenseAmount(new BigDecimal("100.00"));
        expenseMapper.expenses.put(42L, existingExpense);

        setFieldInClass(expenseService, FinExpenseServiceImpl.class, "finExpenseMapper", expenseMapper);
        setFieldInClass(expenseService, FinExpenseServiceImpl.class, "finAccountingPeriodService", periodService);

        FinExpense updateReq = new FinExpense();
        updateReq.setExpenseId(42L);
        updateReq.setPeriodId(10L);

        // Should not throw
        assertDoesNotThrow(() -> expenseService.updateFinExpense(updateReq),
                "Expense update on ACTIVE period should be allowed");
    }

    // ── Tests: rollbackCarryForward requires non-empty reason ──

    @Test
    void rollbackCarryForward_nullReason_throwsServiceException() {
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        FakeAccountingPeriodMapperForLock mapper = new FakeAccountingPeriodMapperForLock();
        try {
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", mapper);
        } catch (Exception e) {
            fail("Failed to inject mapper: " + e.getMessage());
        }

        ServiceException ex = assertThrows(ServiceException.class,
                () -> periodService.rollbackCarryForward(100L, null));
        assertTrue(ex.getMessage().contains("反结账原因不能为空"));
    }

    @Test
    void rollbackCarryForward_emptyReason_throwsServiceException() {
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        FakeAccountingPeriodMapperForLock mapper = new FakeAccountingPeriodMapperForLock();
        try {
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", mapper);
        } catch (Exception e) {
            fail("Failed to inject mapper: " + e.getMessage());
        }

        ServiceException ex = assertThrows(ServiceException.class,
                () -> periodService.rollbackCarryForward(100L, ""));
        assertTrue(ex.getMessage().contains("反结账原因不能为空"));
    }

    @Test
    void rollbackCarryForward_whitespaceOnlyReason_throwsServiceException() {
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        FakeAccountingPeriodMapperForLock mapper = new FakeAccountingPeriodMapperForLock();
        try {
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", mapper);
        } catch (Exception e) {
            fail("Failed to inject mapper: " + e.getMessage());
        }

        ServiceException ex = assertThrows(ServiceException.class,
                () -> periodService.rollbackCarryForward(100L, "   "));
        assertTrue(ex.getMessage().contains("反结账原因不能为空"));
    }

    // ── Tests: carryForward blocks when checkBeforeLock has BLOCK items ──

    @Test
    void carryForward_blocksWhenCheckBeforeLockHasBlockItem() {
        setupAdmin();
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        FakeAccountingPeriodMapperForLock mapper = new FakeAccountingPeriodMapperForLock();

        // Setup active period
        FinAccountingPeriod activePeriod = new FinAccountingPeriod();
        activePeriod.setPeriodId(1L);
        activePeriod.setDeptId(100L);
        activePeriod.setStatus(PeriodStatus.ACTIVE);
        mapper.currentPeriod = activePeriod;

        // Setup check service that returns BLOCK item
        FakeAccountingPeriodCheckServiceForLock checkService = new FakeAccountingPeriodCheckServiceForLock();
        AccountingPeriodCheckResultVO checkResult = new AccountingPeriodCheckResultVO();
        checkResult.setCanLock(false);
        List<AccountingPeriodCheckItemVO> items = new ArrayList<>();
        items.add(new AccountingPeriodCheckItemVO(
                "UNSETTLED_PROFIT_SHARE", "BLOCK", "未结算分润",
                "存在3笔未结算的分润记录，必须先完成结算", 3, BigDecimal.ZERO));
        checkResult.setItems(items);
        checkResult.setDeptId(100L);
        checkService.checkResult = checkResult;

        try {
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", mapper);
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "accountingPeriodCheckService", checkService);
        } catch (Exception e) {
            fail("Failed to inject dependencies: " + e.getMessage());
        }

        ServiceException ex = assertThrows(ServiceException.class,
                () -> periodService.carryForward(100L),
                "carryForward should throw when BLOCK items exist");
        String msg = ex.getMessage();
        assertTrue(msg.contains("阻断"), "Error should mention blocking: " + msg);
        assertTrue(msg.contains("未结算分润"), "Error should mention the BLOCK item: " + msg);
    }

    @Test
    void carryForward_proceedsWhenCheckBeforeLockIsClean() {
        setupAdmin();
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        FakeAccountingPeriodMapperForLock mapper = new FakeAccountingPeriodMapperForLock();

        // Setup active period
        FinAccountingPeriod activePeriod = new FinAccountingPeriod();
        activePeriod.setPeriodId(1L);
        activePeriod.setDeptId(100L);
        activePeriod.setStatus(PeriodStatus.ACTIVE);
        mapper.currentPeriod = activePeriod;

        // Setup check service that returns canLock=true with no BLOCK items
        FakeAccountingPeriodCheckServiceForLock checkService = new FakeAccountingPeriodCheckServiceForLock();
        AccountingPeriodCheckResultVO checkResult = new AccountingPeriodCheckResultVO();
        checkResult.setCanLock(true);
        checkResult.setItems(new ArrayList<>());
        checkResult.setDeptId(100L);
        checkService.checkResult = checkResult;

        // Stub profit share service and audit recorder
        FakeProfitShareRecordServiceForLock profitShareService = new FakeProfitShareRecordServiceForLock();
        NoOpAuditTrailRecorder auditRecorder = new NoOpAuditTrailRecorder();

        try {
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", mapper);
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "accountingPeriodCheckService", checkService);
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finProfitShareRecordService", profitShareService);
            setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "auditTrailRecorder", auditRecorder);
        } catch (Exception e) {
            fail("Failed to inject dependencies: " + e.getMessage());
        }

        // Should not throw (carryForward proceeds when no BLOCK items)
        assertDoesNotThrow(() -> periodService.carryForward(100L),
                "carryForward should proceed when check is clean");
    }

    // ── Fake mappers ──

    static class FakeAccountingPeriodMapperForLock implements FinAccountingPeriodMapper {
        Map<Long, FinAccountingPeriod> periods = new HashMap<>();
        FinAccountingPeriod currentPeriod = null;

        @Override public FinAccountingPeriod selectFinAccountingPeriodByPeriodId(Long id) {
            FinAccountingPeriod p = periods.get(id);
            return p != null ? p : (currentPeriod != null && currentPeriod.getPeriodId() != null && currentPeriod.getPeriodId().equals(id) ? currentPeriod : null);
        }
        @Override public FinAccountingPeriod selectCurrentPeriodByDeptId(Long deptId) { return currentPeriod; }
        @Override public FinAccountingPeriod selectLatestCarriedPeriodByDeptId(Long deptId) { return null; }
        @Override public List<FinAccountingPeriod> selectFinAccountingPeriodList(FinAccountingPeriod p) { return Collections.emptyList(); }
        @Override public int insertFinAccountingPeriod(FinAccountingPeriod p) {
            if (p.getPeriodId() != null) { periods.put(p.getPeriodId(), p); }
            return 1;
        }
        @Override public int updateFinAccountingPeriod(FinAccountingPeriod p) {
            if (p.getPeriodId() != null) { periods.put(p.getPeriodId(), p); }
            return 1;
        }
        @Override public int resetCarryForwardByPeriodId(Long id, String u) { return 0; }
        @Override public int deleteFinAccountingPeriodByPeriodId(Long id) { return 0; }
        @Override public int deleteFinAccountingPeriodByPeriodIds(Long[] ids) { return 0; }
        @Override public BigDecimal selectTotalVerifiedExpense(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalPurchase(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalSalePayment(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalSaleAmount(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalUnverifiedAdvance(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public String selectCurrentPeriodStatusByDeptIds(List<Long> deptIds) { return null; }
        @Override public FinAccountingPeriod selectPeriodById(Long id) { return periods.get(id); }
    }

    static class FakeExpenseMapperForLock implements FinExpenseMapper {
        Map<Long, FinExpense> expenses = new HashMap<>();

        @Override public FinExpense selectFinExpenseByExpenseId(Long id) { return expenses.get(id); }
        @Override public List<FinExpense> selectFinExpenseList(FinExpense e) { return Collections.emptyList(); }
        @Override public int insertFinExpense(FinExpense e) { return 0; }
        @Override public int updateFinExpense(FinExpense e) { return 1; }
        @Override public int deleteFinExpenseByExpenseId(Long id) { return 1; }
        @Override public int deleteFinExpenseByExpenseIds(Long[] ids) { return ids.length; }
        @Override public FinExpense checkExpenseNoUnique(String no) { return null; }
        @Override public int countTodayExpenses() { return 0; }
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
        @Override public BigDecimal selectTodayTotalExpense(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalExpense(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override
        public BigDecimal selectMonthTotalExpenseForPrev(List<Long> deptIds) {
            return BigDecimal.ZERO;
        }
        @Override public int countUnverifiedExpenses(List<Long> deptIds) { return 0; }
        @Override public BigDecimal sumUnverifiedExpenseAmount(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public int countUnverifiedExpensesByPeriodId(List<Long> deptIds, Long periodId) { return 0; }
        @Override public BigDecimal sumUnverifiedExpenseAmountByPeriodId(List<Long> deptIds, Long periodId) { return BigDecimal.ZERO; }
        @Override public List<Map<String, Object>> selectExpenseCategoryStatsWithPrev(List<Long> d, Date s, Date e, Date ps, Date pe) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectUnverifiedExpenseList(List<Long> deptIds) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectOcrAnomalies(List<Long> deptIds) { return Collections.emptyList(); }
    }

    static class FakeSaleMapperForLock implements FinSaleRecordMapper {
        Map<Long, FinSaleRecord> sales = new HashMap<>();

        @Override public FinSaleRecord selectFinSaleRecordBySaleId(Long id) { return sales.get(id); }
        @Override public List<FinSaleRecord> selectFinSaleRecordList(FinSaleRecord r) { return Collections.emptyList(); }
        @Override public int insertFinSaleRecord(FinSaleRecord r) { return 0; }
        @Override public int updateFinSaleRecord(FinSaleRecord r) { return 1; }
        @Override public int deleteFinSaleRecordBySaleId(Long id) { return 1; }
        @Override public int deleteFinSaleRecordBySaleIds(Long[] ids) { return ids.length; }
        @Override public List<Map<String, Object>> selectSaleTrendStats(List<Long> d, Date s, Date e) { return Collections.emptyList(); }
        @Override public int countSaleRecords(List<Long> d, Date s, Date e) { return 0; }
        @Override public int sumSaleQuantity(List<Long> d, Date s, Date e) { return 0; }
        @Override public FinSaleRecord checkSaleNoUnique(String no) { return null; }
        @Override public int countTodaySales() { return 0; }
        @Override public BigDecimal selectTodayTotalSales(List<Long> d) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalSales(List<Long> d) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTodayTotalSalesForPrev(List<Long> d) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalSalesForPrev(List<Long> d) { return BigDecimal.ZERO; }
        @Override public List<Map<String, Object>> selectSalesByDept(List<Long> d, Date s, Date e) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectProductSalesRank(List<Long> d, Date s, Date e) { return Collections.emptyList(); }
        @Override public BigDecimal selectMemberSales(List<Long> d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectSeckillSales(List<Long> d, Date s, Date e) { return BigDecimal.ZERO; }
    }

    static class FakeSalePaymentMapperForLock implements FinSalePaymentMapper {
        @Override public com.junsong.finance.domain.FinSalePayment selectFinSalePaymentByPaymentId(Long id) { return null; }
        @Override public List<com.junsong.finance.domain.FinSalePayment> selectFinSalePaymentBySaleId(Long saleId) { return Collections.emptyList(); }
        @Override public List<com.junsong.finance.domain.FinSalePayment> selectFinSalePaymentList(com.junsong.finance.domain.FinSalePayment p) { return Collections.emptyList(); }
        @Override public int insertFinSalePayment(com.junsong.finance.domain.FinSalePayment p) { return 0; }
        @Override public int batchFinSalePayment(List<com.junsong.finance.domain.FinSalePayment> list) { return 0; }
        @Override public int updateFinSalePayment(com.junsong.finance.domain.FinSalePayment p) { return 0; }
        @Override public int deleteFinSalePaymentByPaymentId(Long id) { return 0; }
        @Override public int deleteFinSalePaymentByPaymentIds(Long[] ids) { return 0; }
        @Override public int deleteFinSalePaymentBySaleId(Long saleId) { return 0; }
        @Override public int deleteFinSalePaymentBySaleIds(Long[] saleIds) { return 0; }
        @Override public com.junsong.finance.domain.FinSalePayment checkPaymentNoUnique(String no) { return null; }
        @Override public BigDecimal sumPaymentAmountBySaleId(Long saleId) { return BigDecimal.ZERO; }
        @Override public int countTodayPayments() { return 0; }
    }

    static class FakeAccountingPeriodCheckServiceForLock implements IAccountingPeriodCheckService {
        AccountingPeriodCheckResultVO checkResult = null;

        @Override
        public AccountingPeriodCheckResultVO checkBeforeLock(Long deptId) {
            return checkResult;
        }
    }

    static class FakeProfitShareRecordServiceForLock implements IFinProfitShareRecordService {
        @Override public FinProfitShareRecord selectFinProfitShareRecordByShareId(Long shareId) { return null; }
        @Override public FinProfitShareRecord carryForwardPeriod(Long periodId) { return null; }
        @Override public List<FinProfitShareRecord> selectFinProfitShareRecordList(FinProfitShareRecord r) { return Collections.emptyList(); }
        @Override public int insertFinProfitShareRecord(FinProfitShareRecord r) { return 0; }
        @Override public int updateFinProfitShareRecord(FinProfitShareRecord r) { return 0; }
        @Override public int deleteFinProfitShareRecordByShareIds(Long[] ids) { return 0; }
    }

    static class NoOpAuditTrailRecorder extends FinAuditTrailRecorder {
        NoOpAuditTrailRecorder() {
            // super has JdbcTemplate dependency, but record() is overridden to do nothing
        }
        @Override
        public void record(String action, String targetType, String targetId,
                           String beforeSnapshot, String afterSnapshot) {
            // no-op for testing
        }
    }
}
