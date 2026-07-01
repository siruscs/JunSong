package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.finance.domain.FinAccountingPeriod;
import com.junsong.finance.domain.FinExpense;
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.domain.vo.AccountingPeriodCheckItemVO;
import com.junsong.finance.domain.vo.AccountingPeriodCheckResultVO;
import com.junsong.finance.constant.PeriodStatus;
import com.junsong.finance.constant.VerifyStatus;
import com.junsong.finance.mapper.*;
import com.junsong.finance.service.IAccountingPeriodCheckService;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that audit trail recording is invoked during critical finance operations.
 * Uses a FakeAuditTrailRecorder that captures calls in a list instead of using JdbcTemplate.
 */
class FinAuditTrailTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.remove();
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

    // ── FakeAuditTrailRecorder ──

    static class FakeAuditTrailRecorder extends FinAuditTrailRecorder {
        final List<AuditCall> calls = new ArrayList<>();

        FakeAuditTrailRecorder() {
            // Do not call super() with JdbcTemplate; override all methods
        }

        @Override
        public void record(String action, String targetType, String targetId,
                           String beforeSnapshot, String afterSnapshot) {
            calls.add(new AuditCall(action, targetType, targetId, beforeSnapshot, afterSnapshot));
        }
    }

    static class AuditCall {
        final String action;
        final String targetType;
        final String targetId;
        final String beforeSnapshot;
        final String afterSnapshot;

        AuditCall(String action, String targetType, String targetId,
                  String beforeSnapshot, String afterSnapshot) {
            this.action = action;
            this.targetType = targetType;
            this.targetId = targetId;
            this.beforeSnapshot = beforeSnapshot;
            this.afterSnapshot = afterSnapshot;
        }
    }

    // ── Tests: expense delete records audit trail ──

    @Test
    void deleteExpense_recordsAuditTrail() throws Exception {
        setupAdmin();
        FinExpenseServiceImpl expenseService = new FinExpenseServiceImpl();

        // Fake period service that allows edits (ACTIVE period)
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        AccountingPeriodLockGuardTest.FakeAccountingPeriodMapperForLock periodMapper =
                new AccountingPeriodLockGuardTest.FakeAccountingPeriodMapperForLock();
        FinAccountingPeriod activePeriod = new FinAccountingPeriod();
        activePeriod.setPeriodId(10L);
        activePeriod.setStatus(PeriodStatus.ACTIVE);
        periodMapper.periods.put(10L, activePeriod);
        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", periodMapper);

        // Fake expense mapper with an existing expense
        AccountingPeriodLockGuardTest.FakeExpenseMapperForLock expenseMapper =
                new AccountingPeriodLockGuardTest.FakeExpenseMapperForLock();
        FinExpense existingExpense = new FinExpense();
        existingExpense.setExpenseId(42L);
        existingExpense.setPeriodId(10L);
        existingExpense.setExpenseNo("FY202606010001");
        existingExpense.setExpenseAmount(new BigDecimal("500.00"));
        existingExpense.setStatus(VerifyStatus.UNVERIFIED);
        expenseMapper.expenses.put(42L, existingExpense);

        FakeAuditTrailRecorder recorder = new FakeAuditTrailRecorder();

        setFieldInClass(expenseService, FinExpenseServiceImpl.class, "finExpenseMapper", expenseMapper);
        setFieldInClass(expenseService, FinExpenseServiceImpl.class, "finAccountingPeriodService", periodService);
        setFieldInClass(expenseService, FinExpenseServiceImpl.class, "auditTrailRecorder", recorder);

        expenseService.deleteFinExpenseByExpenseId(42L);

        assertFalse(recorder.calls.isEmpty(), "Audit trail should be recorded on expense delete");
        assertEquals("delete_expense", recorder.calls.get(0).action);
        assertEquals("fin_expense", recorder.calls.get(0).targetType);
        assertEquals("42", recorder.calls.get(0).targetId);
        assertNotNull(recorder.calls.get(0).beforeSnapshot);
        assertTrue(recorder.calls.get(0).beforeSnapshot.contains("FY202606010001"));
        assertNull(recorder.calls.get(0).afterSnapshot);
    }

    // ── Tests: sale delete records audit trail ──

    @Test
    void deleteSale_recordsAuditTrail() throws Exception {
        setupAdmin();
        FinSaleRecordServiceImpl saleService = new FinSaleRecordServiceImpl();

        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        AccountingPeriodLockGuardTest.FakeAccountingPeriodMapperForLock periodMapper =
                new AccountingPeriodLockGuardTest.FakeAccountingPeriodMapperForLock();
        FinAccountingPeriod activePeriod = new FinAccountingPeriod();
        activePeriod.setPeriodId(10L);
        activePeriod.setStatus(PeriodStatus.ACTIVE);
        periodMapper.periods.put(10L, activePeriod);
        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", periodMapper);

        AccountingPeriodLockGuardTest.FakeSaleMapperForLock saleMapper =
                new AccountingPeriodLockGuardTest.FakeSaleMapperForLock();
        FinSaleRecord existingSale = new FinSaleRecord();
        existingSale.setSaleId(55L);
        existingSale.setPeriodId(10L);
        existingSale.setSaleNo("XS202606010001");
        existingSale.setSaleAmount(new BigDecimal("800.00"));
        saleMapper.sales.put(55L, existingSale);

        AccountingPeriodLockGuardTest.FakeSalePaymentMapperForLock paymentMapper =
                new AccountingPeriodLockGuardTest.FakeSalePaymentMapperForLock();

        FakeAuditTrailRecorder recorder = new FakeAuditTrailRecorder();

        setFieldInClass(saleService, FinSaleRecordServiceImpl.class, "finSaleRecordMapper", saleMapper);
        setFieldInClass(saleService, FinSaleRecordServiceImpl.class, "finSalePaymentMapper", paymentMapper);
        setFieldInClass(saleService, FinSaleRecordServiceImpl.class, "finAccountingPeriodService", periodService);
        setFieldInClass(saleService, FinSaleRecordServiceImpl.class, "auditTrailRecorder", recorder);

        saleService.deleteFinSaleRecordBySaleId(55L);

        assertFalse(recorder.calls.isEmpty(), "Audit trail should be recorded on sale delete");
        assertEquals("delete_sale", recorder.calls.get(0).action);
        assertEquals("fin_sale_record", recorder.calls.get(0).targetType);
        assertEquals("55", recorder.calls.get(0).targetId);
        assertTrue(recorder.calls.get(0).beforeSnapshot.contains("XS202606010001"));
    }

    // ── Tests: carry forward records audit trail ──

    @Test
    void carryForward_recordsAuditTrail() throws Exception {
        setupAdmin();
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();

        FakeAccountingPeriodMapperForAudit periodMapper = new FakeAccountingPeriodMapperForAudit();
        FinAccountingPeriod activePeriod = new FinAccountingPeriod();
        activePeriod.setPeriodId(100L);
        activePeriod.setDeptId(100L);
        activePeriod.setStatus(PeriodStatus.ACTIVE);
        activePeriod.setStartTime(new Date());
        activePeriod.setTotalVerifiedExpense(BigDecimal.ZERO);
        activePeriod.setTotalPurchase(BigDecimal.ZERO);
        activePeriod.setTotalSalePayment(BigDecimal.ZERO);
        activePeriod.setTotalSaleAmount(BigDecimal.ZERO);
        activePeriod.setTotalUnverifiedAdvance(BigDecimal.ZERO);
        activePeriod.setNetProfit(BigDecimal.ZERO);
        periodMapper.periods.put(100L, activePeriod);
        periodMapper.currentDeptPeriod.put(100L, activePeriod);

        FakeAuditTrailRecorder recorder = new FakeAuditTrailRecorder();

        // Fake profit share service (no-op)
        FakeProfitShareServiceForAudit profitShareService = new FakeProfitShareServiceForAudit();

        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", periodMapper);
        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "auditTrailRecorder", recorder);
        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finProfitShareRecordService", profitShareService);

        // Inject check service that returns clean result (canLock=true)
        FakeAccountingPeriodCheckServiceForAudit checkService = new FakeAccountingPeriodCheckServiceForAudit();
        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "accountingPeriodCheckService", checkService);

        periodService.carryForward(100L);

        assertTrue(recorder.calls.stream().anyMatch(c -> "period_carry_forward".equals(c.action)),
                "Carry forward should record audit trail");
        assertTrue(recorder.calls.stream().anyMatch(c -> "accounting_period".equals(c.targetType)));
    }

    // ── Tests: rollback carry forward records audit trail ──

    @Test
    void rollbackCarryForward_recordsAuditTrail() throws Exception {
        setupAdmin();
        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();

        FakeAccountingPeriodMapperForAudit periodMapper = new FakeAccountingPeriodMapperForAudit();

        // An active period (empty, no business data) that will be deleted
        FinAccountingPeriod activePeriod = new FinAccountingPeriod();
        activePeriod.setPeriodId(200L);
        activePeriod.setDeptId(100L);
        activePeriod.setStatus(PeriodStatus.ACTIVE);
        activePeriod.setStartTime(new Date());
        periodMapper.periods.put(200L, activePeriod);
        periodMapper.currentDeptPeriod.put(100L, activePeriod);

        // A carried period that will be rolled back
        FinAccountingPeriod carriedPeriod = new FinAccountingPeriod();
        carriedPeriod.setPeriodId(199L);
        carriedPeriod.setDeptId(100L);
        carriedPeriod.setStatus(PeriodStatus.CARRIED);
        carriedPeriod.setStartTime(new Date());
        carriedPeriod.setCarryForwardTime(new Date());
        periodMapper.periods.put(199L, carriedPeriod);
        periodMapper.latestCarriedPeriod.put(100L, carriedPeriod);

        FakeAuditTrailRecorder recorder = new FakeAuditTrailRecorder();
        FakeProfitShareServiceForAudit profitShareService = new FakeProfitShareServiceForAudit();

        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", periodMapper);
        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "auditTrailRecorder", recorder);
        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finProfitShareRecordService", profitShareService);

        periodService.rollbackCarryForward(100L, "审计追踪测试-反结账");

        assertTrue(recorder.calls.stream().anyMatch(c -> "period_rollback".equals(c.action)),
                "Rollback carry forward should record audit trail");
        assertTrue(recorder.calls.stream().anyMatch(c -> "accounting_period".equals(c.targetType)));
    }

    // ── Batch expense delete records multiple audit trails ──

    @Test
    void batchDeleteExpenses_recordsMultipleAuditTrails() throws Exception {
        setupAdmin();
        FinExpenseServiceImpl expenseService = new FinExpenseServiceImpl();

        FinAccountingPeriodServiceImpl periodService = new FinAccountingPeriodServiceImpl();
        AccountingPeriodLockGuardTest.FakeAccountingPeriodMapperForLock periodMapper =
                new AccountingPeriodLockGuardTest.FakeAccountingPeriodMapperForLock();
        FinAccountingPeriod activePeriod = new FinAccountingPeriod();
        activePeriod.setPeriodId(10L);
        activePeriod.setStatus(PeriodStatus.ACTIVE);
        periodMapper.periods.put(10L, activePeriod);
        setFieldInClass(periodService, FinAccountingPeriodServiceImpl.class, "finAccountingPeriodMapper", periodMapper);

        AccountingPeriodLockGuardTest.FakeExpenseMapperForLock expenseMapper =
                new AccountingPeriodLockGuardTest.FakeExpenseMapperForLock();

        for (long i = 1L; i <= 3L; i++) {
            FinExpense exp = new FinExpense();
            exp.setExpenseId(i);
            exp.setPeriodId(10L);
            exp.setExpenseNo("FY" + i);
            exp.setExpenseAmount(new BigDecimal("100.00"));
            exp.setStatus(VerifyStatus.UNVERIFIED);
            expenseMapper.expenses.put(i, exp);
        }

        FakeAuditTrailRecorder recorder = new FakeAuditTrailRecorder();

        setFieldInClass(expenseService, FinExpenseServiceImpl.class, "finExpenseMapper", expenseMapper);
        setFieldInClass(expenseService, FinExpenseServiceImpl.class, "finAccountingPeriodService", periodService);
        setFieldInClass(expenseService, FinExpenseServiceImpl.class, "auditTrailRecorder", recorder);

        expenseService.deleteFinExpenseByExpenseIds(new Long[]{1L, 2L, 3L});

        assertEquals(3, recorder.calls.size(), "Should record 3 audit trails for 3 deleted expenses");
        assertTrue(recorder.calls.stream().allMatch(c -> "delete_expense".equals(c.action)));
    }

    // ── Fake mappers ──

    static class FakeAccountingPeriodMapperForAudit implements FinAccountingPeriodMapper {
        Map<Long, FinAccountingPeriod> periods = new HashMap<>();
        Map<Long, FinAccountingPeriod> currentDeptPeriod = new HashMap<>();
        Map<Long, FinAccountingPeriod> latestCarriedPeriod = new HashMap<>();

        @Override public FinAccountingPeriod selectFinAccountingPeriodByPeriodId(Long id) { return periods.get(id); }
        @Override public FinAccountingPeriod selectCurrentPeriodByDeptId(Long deptId) { return currentDeptPeriod.get(deptId); }
        @Override public FinAccountingPeriod selectLatestCarriedPeriodByDeptId(Long deptId) { return latestCarriedPeriod.get(deptId); }
        @Override public List<FinAccountingPeriod> selectFinAccountingPeriodList(FinAccountingPeriod p) { return new ArrayList<>(periods.values()); }
        @Override public int insertFinAccountingPeriod(FinAccountingPeriod p) {
            if (p.getPeriodId() == null) { p.setPeriodId((long)(periods.size() + 1000)); }
            periods.put(p.getPeriodId(), p);
            return 1;
        }
        @Override public int updateFinAccountingPeriod(FinAccountingPeriod p) {
            periods.put(p.getPeriodId(), p);
            return 1;
        }
        @Override public int resetCarryForwardByPeriodId(Long id, String u) { return 0; }
        @Override public int deleteFinAccountingPeriodByPeriodId(Long id) { periods.remove(id); return 1; }
        @Override public int deleteFinAccountingPeriodByPeriodIds(Long[] ids) { return 0; }
        @Override public BigDecimal selectTotalVerifiedExpense(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalPurchase(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalSalePayment(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalSaleAmount(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTotalUnverifiedAdvance(Long p, Long d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public String selectCurrentPeriodStatusByDeptIds(List<Long> deptIds) { return null; }
        @Override public FinAccountingPeriod selectPeriodById(Long id) { return periods.get(id); }
    }

    static class FakeProfitShareServiceForAudit implements com.junsong.finance.service.IFinProfitShareRecordService {
        @Override public com.junsong.finance.domain.FinProfitShareRecord selectFinProfitShareRecordByShareId(Long id) { return null; }
        @Override public List<com.junsong.finance.domain.FinProfitShareRecord> selectFinProfitShareRecordList(com.junsong.finance.domain.FinProfitShareRecord r) { return Collections.emptyList(); }
        @Override public int insertFinProfitShareRecord(com.junsong.finance.domain.FinProfitShareRecord r) { return 0; }
        @Override public int updateFinProfitShareRecord(com.junsong.finance.domain.FinProfitShareRecord r) { return 0; }
        @Override public int deleteFinProfitShareRecordByShareIds(Long[] ids) { return 0; }
        @Override public com.junsong.finance.domain.FinProfitShareRecord carryForwardPeriod(Long periodId) { return null; }
    }

    static class FakeAccountingPeriodCheckServiceForAudit implements IAccountingPeriodCheckService {
        @Override
        public AccountingPeriodCheckResultVO checkBeforeLock(Long deptId) {
            AccountingPeriodCheckResultVO result = new AccountingPeriodCheckResultVO();
            result.setCanLock(true);
            result.setHasWarning(false);
            result.setDeptId(deptId);
            result.setItems(new ArrayList<>());
            return result;
        }
    }
}
