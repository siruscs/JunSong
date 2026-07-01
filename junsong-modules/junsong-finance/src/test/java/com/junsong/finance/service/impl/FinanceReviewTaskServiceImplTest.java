package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.finance.domain.FinExpense;
import com.junsong.finance.domain.FinProfitShareRecord;
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.domain.FinanceReviewTask;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.mapper.FinExpenseMapper;
import com.junsong.finance.mapper.FinProfitShareRecordMapper;
import com.junsong.finance.mapper.FinSaleRecordMapper;
import com.junsong.finance.mapper.FinanceReviewTaskMapper;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FinanceReviewTaskServiceImpl unit tests.
 * Uses hand-written fakes (no Mockito).
 */
class FinanceReviewTaskServiceImplTest {

    // ─── Security context helpers ──────────────────────────────────────────

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.remove();
    }

    /**
     * Set up SecurityContextHolder so that SecurityUtils.getUserId(), getUserName(),
     * and getDeptId() return the given values.
     */
    private static void setupSecurityContext(Long userId, String username, Long deptId) {
        SecurityContextHolder.setUserId(String.valueOf(userId));
        SecurityContextHolder.setUserName(username);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(userId);
        loginUser.setUsername(username);
        loginUser.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    // ─── Helper: inject field via reflection ────────────────────────────────

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = FinanceReviewTaskServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Create service with admin context (backward-compatible for existing tests).
     */
    private static FinanceReviewTaskServiceImpl createService(
            FinanceReviewTaskMapper reviewTaskMapper,
            FinSaleRecordMapper saleMapper,
            FinExpenseMapper expenseMapper,
            FinProfitShareRecordMapper profitShareMapper) throws Exception {
        return createService(reviewTaskMapper, saleMapper, expenseMapper, profitShareMapper, null);
    }

    /**
     * Create service injecting all fields including optional RemoteUserService.
     * Sets admin context (userId=1) by default so existing tests bypass authorization.
     */
    private static FinanceReviewTaskServiceImpl createService(
            FinanceReviewTaskMapper reviewTaskMapper,
            FinSaleRecordMapper saleMapper,
            FinExpenseMapper expenseMapper,
            FinProfitShareRecordMapper profitShareMapper,
            RemoteUserService remoteUserService) throws Exception {
        // Default to admin context so existing tests bypass authorization
        setupSecurityContext(1L, "admin", 1L);
        FinanceReviewTaskServiceImpl service = new FinanceReviewTaskServiceImpl();
        setField(service, "reviewTaskMapper", reviewTaskMapper);
        setField(service, "finSaleRecordMapper", saleMapper);
        setField(service, "finExpenseMapper", expenseMapper);
        setField(service, "finProfitShareRecordMapper", profitShareMapper);
        setField(service, "remoteUserService", remoteUserService);
        return service;
    }

    // ─── Test: idempotent generate ─────────────────────────────────────────

    @Test
    void generateFromDiagnosis_sameAlertIdAndTaskDate_onlyProducesOneRecord() throws Exception {
        // Configure mappers to trigger PendingVerifyHighRule (unverifiedAmount > 5000)
        TriggeringExpenseMapper expenseMapper = new TriggeringExpenseMapper();
        expenseMapper.unverifiedAmount = new BigDecimal("6000");

        FinanceReviewTaskServiceImpl service = createService(
                new FakeReviewTaskMapper(),
                new NoOpSaleMapper(),
                expenseMapper,
                new NoOpProfitShareMapper());

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Arrays.asList(1L));
        params.setStartTime(new Date());
        params.setEndTime(new Date());

        // First call: should generate tasks
        int firstCount = service.generateFromDiagnosis(Arrays.asList(1L), params);
        assertTrue(firstCount > 0, "First call should generate at least one task");

        // Second call: same data, same day → all alertIds already exist → 0 new
        int secondCount = service.generateFromDiagnosis(Arrays.asList(1L), params);
        assertEquals(0, secondCount, "Second call with same data on same day should produce 0 new tasks");
    }

    // ─── Test: ignore without reason fails ──────────────────────────────────

    @Test
    void markIgnored_withoutReason_throwsServiceException() throws Exception {
        FakeReviewTaskMapper fakeMapper = new FakeReviewTaskMapper();
        FinanceReviewTask task = createPendingTask(fakeMapper);

        FinanceReviewTaskServiceImpl service = createService(
                fakeMapper, new NoOpSaleMapper(), new NoOpExpenseMapper(), new NoOpProfitShareMapper());

        assertThrows(ServiceException.class, () ->
                service.markIgnored(task.getTaskId(), 1L, "admin", null),
                "Ignoring with null reason should throw");

        assertThrows(ServiceException.class, () ->
                service.markIgnored(task.getTaskId(), 1L, "admin", ""),
                "Ignoring with empty reason should throw");

        assertThrows(ServiceException.class, () ->
                service.markIgnored(task.getTaskId(), 1L, "admin", "   "),
                "Ignoring with blank reason should throw");
    }

    // ─── Test: done without note fails ──────────────────────────────────────

    @Test
    void markDone_withoutNote_throwsServiceException() throws Exception {
        FakeReviewTaskMapper fakeMapper = new FakeReviewTaskMapper();
        FinanceReviewTask task = createPendingTask(fakeMapper);

        FinanceReviewTaskServiceImpl service = createService(
                fakeMapper, new NoOpSaleMapper(), new NoOpExpenseMapper(), new NoOpProfitShareMapper());

        assertThrows(ServiceException.class, () ->
                service.markDone(task.getTaskId(), 1L, "admin", null),
                "Done with null note should throw");

        assertThrows(ServiceException.class, () ->
                service.markDone(task.getTaskId(), 1L, "admin", ""),
                "Done with empty note should throw");

        assertThrows(ServiceException.class, () ->
                service.markDone(task.getTaskId(), 1L, "admin", "   "),
                "Done with blank note should throw");
    }

    // ─── Test: DONE state cannot be updated again ──────────────────────────

    @Test
    void markDone_terminalState_cannotBeUpdatedAgain() throws Exception {
        FakeReviewTaskMapper fakeMapper = new FakeReviewTaskMapper();
        FinanceReviewTask task = createPendingTask(fakeMapper);

        FinanceReviewTaskServiceImpl service = createService(
                fakeMapper, new NoOpSaleMapper(), new NoOpExpenseMapper(), new NoOpProfitShareMapper());

        // Transition to DONE
        service.markDone(task.getTaskId(), 1L, "admin", "Task completed successfully");

        // Attempt further transitions should all fail
        assertThrows(ServiceException.class, () ->
                service.markInProgress(task.getTaskId(), 2L, "user2"),
                "DONE task should not transition to IN_PROGRESS");

        assertThrows(ServiceException.class, () ->
                service.markDone(task.getTaskId(), 2L, "user2", "another note"),
                "DONE task should not transition to DONE again");

        assertThrows(ServiceException.class, () ->
                service.markIgnored(task.getTaskId(), 2L, "user2", "some reason"),
                "DONE task should not transition to IGNORED");
    }

    // ─── Test: IGNORED state cannot be updated again ────────────────────────

    @Test
    void markIgnored_terminalState_cannotBeUpdatedAgain() throws Exception {
        FakeReviewTaskMapper fakeMapper = new FakeReviewTaskMapper();
        FinanceReviewTask task = createPendingTask(fakeMapper);

        FinanceReviewTaskServiceImpl service = createService(
                fakeMapper, new NoOpSaleMapper(), new NoOpExpenseMapper(), new NoOpProfitShareMapper());

        // Transition to IGNORED
        service.markIgnored(task.getTaskId(), 1L, "admin", "Not relevant this period");

        // Attempt further transitions should all fail
        assertThrows(ServiceException.class, () ->
                service.markInProgress(task.getTaskId(), 2L, "user2"),
                "IGNORED task should not transition to IN_PROGRESS");

        assertThrows(ServiceException.class, () ->
                service.markDone(task.getTaskId(), 2L, "user2", "some note"),
                "IGNORED task should not transition to DONE");

        assertThrows(ServiceException.class, () ->
                service.markIgnored(task.getTaskId(), 2L, "user2", "another reason"),
                "IGNORED task should not transition to IGNORED again");
    }

    // ─── Test: authorization filtering on listTasks ──────────────────────

    @Test
    void listTasks_filtersToAuthorizedDeptIds() throws Exception {
        // Setup: non-admin user (userId=99) with authorized depts [100, 200]
        FakeReviewTaskMapper fakeMapper = new FakeReviewTaskMapper();
        FakeRemoteUserService fakeRemoteUser = new FakeRemoteUserService(Arrays.asList(100L, 200L));

        FinanceReviewTaskServiceImpl service = createService(
                fakeMapper, new NoOpSaleMapper(), new NoOpExpenseMapper(), new NoOpProfitShareMapper(),
                fakeRemoteUser);
        // Override to non-admin context after createService set admin
        setupSecurityContext(99L, "testuser", 100L);

        // Call listTasks with requestedDeptIds=[100, 999]
        Map<String, Object> params = new HashMap<>();
        params.put("deptIds", Arrays.asList(100L, 999L));

        service.listTasks(params);

        // Verify: mapper receives deptIds=[100] only, NOT 999
        assertNotNull(fakeMapper.lastQueryParams, "Mapper should have been called");
        @SuppressWarnings("unchecked")
        List<Long> resolvedDeptIds = (List<Long>) fakeMapper.lastQueryParams.get("deptIds");
        assertNotNull(resolvedDeptIds, "deptIds should be set in mapper params");
        assertTrue(resolvedDeptIds.contains(100L), "deptId 100 should be included (authorized)");
        assertFalse(resolvedDeptIds.contains(999L), "deptId 999 should be filtered out (unauthorized)");
        assertEquals(1, resolvedDeptIds.size(), "Only one authorized deptId should remain");
    }

    // ─── Test: authorization rejection on generateFromDiagnosis ───────────

    @Test
    void generateFromDiagnosis_rejectsUnauthorizedDeptId() throws Exception {
        // Setup: non-admin user (userId=99) with authorized depts [100]
        FakeRemoteUserService fakeRemoteUser = new FakeRemoteUserService(Arrays.asList(100L));

        FinanceReviewTaskServiceImpl service = createService(
                new FakeReviewTaskMapper(), new NoOpSaleMapper(), new NoOpExpenseMapper(),
                new NoOpProfitShareMapper(), fakeRemoteUser);
        // Override to non-admin context
        setupSecurityContext(99L, "testuser", 100L);

        // Call generateFromDiagnosis with requestedDeptIds=[999] — unauthorized
        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Arrays.asList(999L));
        params.setStartTime(new Date());
        params.setEndTime(new Date());

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.generateFromDiagnosis(Arrays.asList(999L), params),
                "Should reject unauthorized deptId");
        assertTrue(ex.getMessage().contains("无权"),
                "Error message should contain '无权', got: " + ex.getMessage());
    }

    // ─── Test: authorization rejection on markDone ────────────────────────

    @Test
    void markDone_rejectsTaskOutsideAuthorizedDept() throws Exception {
        // Setup: non-admin user (userId=99) authorized for dept [100]
        FakeReviewTaskMapper fakeMapper = new FakeReviewTaskMapper();
        FakeRemoteUserService fakeRemoteUser = new FakeRemoteUserService(Arrays.asList(100L));

        // Create a task with deptId=999 (outside authorized depts)
        FinanceReviewTask task = new FinanceReviewTask();
        task.setTaskId(fakeMapper.nextId());
        task.setTaskType("TEST_RULE");
        task.setDeptId(999L);
        task.setDeptName("Unauthorized Store");
        task.setTaskDate(new Date());
        task.setStatus("PENDING");
        task.setSeverity("MEDIUM");
        task.setTitle("Unauthorized Task");
        task.setAlertId("FIN_REVIEW:TEST_RULE:999:20260101");
        fakeMapper.store(task);

        FinanceReviewTaskServiceImpl service = createService(
                fakeMapper, new NoOpSaleMapper(), new NoOpExpenseMapper(),
                new NoOpProfitShareMapper(), fakeRemoteUser);
        // Override to non-admin context
        setupSecurityContext(99L, "testuser", 100L);

        // Call markDone — should fail because task's deptId=999 is not in authorized [100]
        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.markDone(task.getTaskId(), 99L, "testuser", "Handling note"),
                "Should reject markDone on task outside authorized dept");
        assertTrue(ex.getMessage().contains("无权"),
                "Error message should contain '无权', got: " + ex.getMessage());
    }

    // ─── Test: beginTime/endTime passed through to mapper (TRAE-R3-03) ──────

    @Test
    void listTasks_passesBeginAndEndTimeToMapper() throws Exception {
        FakeReviewTaskMapper fakeMapper = new FakeReviewTaskMapper();
        FinanceReviewTaskServiceImpl service = createService(
                fakeMapper, new NoOpSaleMapper(), new NoOpExpenseMapper(), new NoOpProfitShareMapper());

        Map<String, Object> params = new HashMap<>();
        params.put("beginTime", "2026-01-01");
        params.put("endTime", "2026-01-31");

        service.listTasks(params);

        assertNotNull(fakeMapper.lastQueryParams, "Mapper should have been called");
        assertEquals("2026-01-01", fakeMapper.lastQueryParams.get("beginTime"),
                "beginTime should be passed through to mapper");
        assertEquals("2026-01-31", fakeMapper.lastQueryParams.get("endTime"),
                "endTime should be passed through to mapper");
    }

    @Test
    void listTasks_withoutBeginEndTimeDoesNotAddThem() throws Exception {
        FakeReviewTaskMapper fakeMapper = new FakeReviewTaskMapper();
        FinanceReviewTaskServiceImpl service = createService(
                fakeMapper, new NoOpSaleMapper(), new NoOpExpenseMapper(), new NoOpProfitShareMapper());

        Map<String, Object> params = new HashMap<>();
        // No beginTime/endTime provided

        service.listTasks(params);

        assertNotNull(fakeMapper.lastQueryParams, "Mapper should have been called");
        assertFalse(fakeMapper.lastQueryParams.containsKey("beginTime"),
                "beginTime should not be present when not provided");
        assertFalse(fakeMapper.lastQueryParams.containsKey("endTime"),
                "endTime should not be present when not provided");
    }

    @Test
    void listTasks_beginEndTimePreservedWithAuthorizedDeptFilter() throws Exception {
        // Non-admin user authorized for dept [100]
        FakeReviewTaskMapper fakeMapper = new FakeReviewTaskMapper();
        FakeRemoteUserService fakeRemoteUser = new FakeRemoteUserService(Arrays.asList(100L));

        FinanceReviewTaskServiceImpl service = createService(
                fakeMapper, new NoOpSaleMapper(), new NoOpExpenseMapper(),
                new NoOpProfitShareMapper(), fakeRemoteUser);
        setupSecurityContext(99L, "testuser", 100L);

        Map<String, Object> params = new HashMap<>();
        params.put("deptIds", Arrays.asList(100L, 999L)); // 999 unauthorized
        params.put("beginTime", "2026-06-01");
        params.put("endTime", "2026-06-30");

        service.listTasks(params);

        assertNotNull(fakeMapper.lastQueryParams);
        // beginTime/endTime preserved
        assertEquals("2026-06-01", fakeMapper.lastQueryParams.get("beginTime"));
        assertEquals("2026-06-30", fakeMapper.lastQueryParams.get("endTime"));
        // Authorized dept filtering still applies
        @SuppressWarnings("unchecked")
        List<Long> resolvedDeptIds = (List<Long>) fakeMapper.lastQueryParams.get("deptIds");
        assertNotNull(resolvedDeptIds);
        assertTrue(resolvedDeptIds.contains(100L));
        assertFalse(resolvedDeptIds.contains(999L));
    }

    // ─── Helper: create a PENDING task in the fake mapper ───────────────────

    private static FinanceReviewTask createPendingTask(FakeReviewTaskMapper mapper) {
        FinanceReviewTask task = new FinanceReviewTask();
        task.setTaskId(mapper.nextId());
        task.setTaskType("TEST_RULE");
        task.setDeptId(1L);
        task.setDeptName("Test Store");
        task.setTaskDate(new Date());
        task.setStatus("PENDING");
        task.setSeverity("MEDIUM");
        task.setTitle("Test Task");
        task.setAlertId("FIN_REVIEW:TEST_RULE:1:20260101");
        mapper.store(task);
        return task;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FAKE IMPLEMENTATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * FakeReviewTaskMapper — stores tasks in a HashMap keyed by taskId.
     */
    static class FakeReviewTaskMapper implements FinanceReviewTaskMapper {

        private final Map<Long, FinanceReviewTask> store = new HashMap<>();
        private final AtomicLong idSequence = new AtomicLong(1);

        /** Captured params from the last selectReviewTaskList call. */
        Map<String, Object> lastQueryParams;

        Long nextId() {
            return idSequence.getAndIncrement();
        }

        void store(FinanceReviewTask task) {
            store.put(task.getTaskId(), task);
        }

        int size() {
            return store.size();
        }

        @Override
        public List<FinanceReviewTask> selectReviewTaskList(Map<String, Object> params) {
            this.lastQueryParams = params;
            return new ArrayList<>(store.values());
        }

        @Override
        public FinanceReviewTask selectByTaskId(Long taskId) {
            return store.get(taskId);
        }

        @Override
        public FinanceReviewTask selectByAlertId(String alertId, String taskDate) {
            for (FinanceReviewTask t : store.values()) {
                if (alertId.equals(t.getAlertId())) {
                    // Compare task_date as yyyy-MM-dd
                    String storedDate = new SimpleDateFormat("yyyy-MM-dd").format(t.getTaskDate());
                    if (taskDate.equals(storedDate)) {
                        return t;
                    }
                }
            }
            return null;
        }

        @Override
        public int insertReviewTask(FinanceReviewTask task) {
            if (task.getTaskId() == null) {
                task.setTaskId(nextId());
            }
            store.put(task.getTaskId(), task);
            return 1;
        }

        @Override
        public int updateReviewTask(FinanceReviewTask task) {
            store.put(task.getTaskId(), task);
            return 1;
        }

        @Override
        public int countByStatus(String status, List<Long> deptIds) {
            int count = 0;
            for (FinanceReviewTask t : store.values()) {
                if (status.equals(t.getStatus())) {
                    if (deptIds == null || deptIds.contains(t.getDeptId())) {
                        count++;
                    }
                }
            }
            return count;
        }
    }

    // ─── NoOp expense mapper (returns zeros) ────────────────────────────────

    static class NoOpExpenseMapper implements FinExpenseMapper {
        @Override public FinExpense selectFinExpenseByExpenseId(Long id) { return null; }
        @Override public List<FinExpense> selectFinExpenseList(FinExpense e) { return Collections.emptyList(); }
        @Override public int insertFinExpense(FinExpense e) { return 0; }
        @Override public int updateFinExpense(FinExpense e) { return 0; }
        @Override public int deleteFinExpenseByExpenseId(Long id) { return 0; }
        @Override public int deleteFinExpenseByExpenseIds(Long[] ids) { return 0; }
        @Override public FinExpense checkExpenseNoUnique(String no) { return null; }
        @Override public int countTodayExpenses() { return 0; }
        @Override public BigDecimal sumUnverifiedExpenses() { return BigDecimal.ZERO; }
        @Override public BigDecimal sumUnverifiedExpensesByDeptId(Long id) { return BigDecimal.ZERO; }
        @Override public BigDecimal sumAllExpenses() { return BigDecimal.ZERO; }
        @Override public BigDecimal sumAllExpensesByDeptId(Long id) { return BigDecimal.ZERO; }
        @Override public BigDecimal sumAllExpensesByPeriodId(Long id) { return BigDecimal.ZERO; }
        @Override public List<FinExpense> selectFinExpenseByExpenseIds(Long[] ids) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectExpenseCategoryStats(Map<String, Object> p) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectExpenseTrendStats(Map<String, Object> p) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectExpenseDeptStats(Map<String, Object> p) { return Collections.emptyList(); }
        @Override public BigDecimal selectExpenseTotal(Map<String, Object> p) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTodayTotalExpense(List<Long> d) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalExpense(List<Long> d) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalExpenseForPrev(List<Long> d) { return BigDecimal.ZERO; }
        @Override public int countUnverifiedExpenses(List<Long> d) { return 0; }
        @Override public BigDecimal sumUnverifiedExpenseAmount(List<Long> d) { return BigDecimal.ZERO; }
        @Override public int countUnverifiedExpensesByPeriodId(List<Long> d, Long periodId) { return 0; }
        @Override public BigDecimal sumUnverifiedExpenseAmountByPeriodId(List<Long> d, Long periodId) { return BigDecimal.ZERO; }
        @Override public List<Map<String, Object>> selectExpenseCategoryStatsWithPrev(List<Long> d, Date s, Date e, Date ps, Date pe) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectUnverifiedExpenseList(List<Long> d) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectOcrAnomalies(List<Long> d) { return Collections.emptyList(); }
    }

    // ─── Triggering expense mapper (triggers PendingVerifyHighRule) ─────────

    static class TriggeringExpenseMapper extends NoOpExpenseMapper {
        BigDecimal unverifiedAmount = BigDecimal.ZERO;
        int unverifiedCount = 0;

        @Override
        public BigDecimal sumUnverifiedExpenseAmount(List<Long> d) { return unverifiedAmount; }
        @Override
        public int countUnverifiedExpenses(List<Long> d) { return unverifiedCount; }
    }

    // ─── NoOp sale mapper ──────────────────────────────────────────────────

    static class NoOpSaleMapper implements FinSaleRecordMapper {
        @Override public FinSaleRecord selectFinSaleRecordBySaleId(Long id) { return null; }
        @Override public List<FinSaleRecord> selectFinSaleRecordList(FinSaleRecord r) { return Collections.emptyList(); }
        @Override public int insertFinSaleRecord(FinSaleRecord r) { return 0; }
        @Override public int updateFinSaleRecord(FinSaleRecord r) { return 0; }
        @Override public int deleteFinSaleRecordBySaleId(Long id) { return 0; }
        @Override public int deleteFinSaleRecordBySaleIds(Long[] ids) { return 0; }
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

    // ─── NoOp profit share mapper ──────────────────────────────────────────

    static class NoOpProfitShareMapper implements FinProfitShareRecordMapper {
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
        @Override public int countUnsettledRecords(List<Long> d) { return 0; }
        @Override public int countUnsettledRecordsByPeriodId(List<Long> d, Long periodId) { return 0; }
        @Override public List<Map<String, Object>> selectSettlementByDept(List<Long> d, Date s, Date e) { return Collections.emptyList(); }
        @Override public BigDecimal selectPaidAmount(List<Long> d, Date s, Date e) { return BigDecimal.ZERO; }
    }

    // ─── Fake RemoteUserService (returns configurable dept list) ──────────

    static class FakeRemoteUserService implements RemoteUserService {

        private final List<Long> authorizedDeptIds;

        FakeRemoteUserService(List<Long> authorizedDeptIds) {
            this.authorizedDeptIds = authorizedDeptIds;
        }

        @Override
        public R<LoginUser> getUserInfo(String username, String source) {
            return R.ok(new LoginUser());
        }

        @Override
        public R<Boolean> registerUserInfo(com.junsong.system.api.domain.SysUser sysUser, String source) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> recordUserLogin(com.junsong.system.api.domain.SysUser sysUser, String source) {
            return R.ok(true);
        }

        @Override
        public R<List<SysDept>> getUserDeptList(String username, String source) {
            List<SysDept> depts = new ArrayList<>();
            for (Long deptId : authorizedDeptIds) {
                SysDept dept = new SysDept();
                dept.setDeptId(deptId);
                dept.setDeptName("Dept-" + deptId);
                depts.add(dept);
            }
            return R.ok(depts);
        }

        @Override
        public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) {
            return R.ok(Collections.emptyList());
        }
    }
}
