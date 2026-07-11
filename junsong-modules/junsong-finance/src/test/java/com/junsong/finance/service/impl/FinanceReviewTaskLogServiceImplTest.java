package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.finance.domain.FinExpense;
import com.junsong.finance.domain.FinProfitShareRecord;
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.domain.FinanceReviewTask;
import com.junsong.finance.domain.FinanceReviewTaskLog;
import com.junsong.finance.mapper.FinExpenseMapper;
import com.junsong.finance.mapper.FinProfitShareRecordMapper;
import com.junsong.finance.mapper.FinSaleRecordMapper;
import com.junsong.finance.mapper.FinanceReviewTaskMapper;
import com.junsong.finance.mapper.FinanceReviewTaskLogMapper;
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
 * FinanceReviewTaskLog audit-trail tests.
 * Uses hand-written fakes (no Mockito).
 */
class FinanceReviewTaskLogServiceImplTest {

    // ─── Security context helpers ──────────────────────────────────────────

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.remove();
    }

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

    private static FinanceReviewTaskServiceImpl createService(
            FinanceReviewTaskMapper reviewTaskMapper,
            FinanceReviewTaskLogMapper reviewTaskLogMapper) throws Exception {
        setupSecurityContext(1L, "admin", 1L);
        FinanceReviewTaskServiceImpl service = new FinanceReviewTaskServiceImpl();
        setField(service, "reviewTaskMapper", reviewTaskMapper);
        setField(service, "reviewTaskLogMapper", reviewTaskLogMapper);
        setField(service, "finSaleRecordMapper", new NoOpSaleMapper());
        setField(service, "finExpenseMapper", new NoOpExpenseMapper());
        setField(service, "finProfitShareRecordMapper", new NoOpProfitShareMapper());
        setField(service, "remoteUserService", null);
        return service;
    }

    // ─── Helper: create a PENDING task ──────────────────────────────────────

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

    private static FinanceReviewTask createInProgressTask(FakeReviewTaskMapper mapper) {
        FinanceReviewTask task = createPendingTask(mapper);
        task.setStatus("IN_PROGRESS");
        return task;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TESTS
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void insertLog_onMarkInProgress_writesLog() throws Exception {
        FakeReviewTaskMapper taskMapper = new FakeReviewTaskMapper();
        RecordingReviewTaskLogMapper logMapper = new RecordingReviewTaskLogMapper();
        FinanceReviewTask task = createPendingTask(taskMapper);

        FinanceReviewTaskServiceImpl service = createService(taskMapper, logMapper);
        service.markInProgress(task.getTaskId(), 1L, "admin");

        assertEquals(1, logMapper.insertedLogs.size(), "Should have inserted exactly one log");
        FinanceReviewTaskLog log = logMapper.insertedLogs.get(0);
        assertEquals(task.getTaskId(), log.getTaskId());
        assertEquals("IN_PROGRESS", log.getActionType());
        assertEquals("IN_PROGRESS", log.getAfterStatus());
        assertEquals(1L, log.getHandlerId());
        assertEquals("admin", log.getHandlerName());
    }

    @Test
    void insertLog_onMarkDone_writesLog() throws Exception {
        FakeReviewTaskMapper taskMapper = new FakeReviewTaskMapper();
        RecordingReviewTaskLogMapper logMapper = new RecordingReviewTaskLogMapper();
        FinanceReviewTask task = createPendingTask(taskMapper);

        FinanceReviewTaskServiceImpl service = createService(taskMapper, logMapper);
        service.markDone(task.getTaskId(), 1L, "admin", "Task completed");

        assertEquals(1, logMapper.insertedLogs.size(), "Should have inserted exactly one log");
        FinanceReviewTaskLog log = logMapper.insertedLogs.get(0);
        assertEquals(task.getTaskId(), log.getTaskId());
        assertEquals("DONE", log.getActionType());
        assertEquals("DONE", log.getAfterStatus());
        assertEquals("Task completed", log.getHandlerNote());
    }

    @Test
    void insertLog_onMarkIgnored_writesLog() throws Exception {
        FakeReviewTaskMapper taskMapper = new FakeReviewTaskMapper();
        RecordingReviewTaskLogMapper logMapper = new RecordingReviewTaskLogMapper();
        FinanceReviewTask task = createPendingTask(taskMapper);

        FinanceReviewTaskServiceImpl service = createService(taskMapper, logMapper);
        service.markIgnored(task.getTaskId(), 1L, "admin", "Not applicable");

        assertEquals(1, logMapper.insertedLogs.size(), "Should have inserted exactly one log");
        FinanceReviewTaskLog log = logMapper.insertedLogs.get(0);
        assertEquals(task.getTaskId(), log.getTaskId());
        assertEquals("IGNORED", log.getActionType());
        assertEquals("IGNORED", log.getAfterStatus());
        assertEquals("Not applicable", log.getHandlerNote());
    }

    @Test
    void getTaskLogs_returnsLogsOrderedByTime() throws Exception {
        FakeReviewTaskMapper taskMapper = new FakeReviewTaskMapper();
        RecordingReviewTaskLogMapper logMapper = new RecordingReviewTaskLogMapper();

        // Store task 100L in taskMapper (verifyTaskAccess checks task existence)
        FinanceReviewTask task100 = new FinanceReviewTask();
        task100.setTaskId(100L);
        task100.setTaskType("TEST_RULE");
        task100.setDeptId(1L);
        task100.setDeptName("Test Store");
        task100.setTaskDate(new Date());
        task100.setStatus("DONE");
        task100.setSeverity("MEDIUM");
        task100.setTitle("Task 100");
        task100.setAlertId("FIN_REVIEW:TEST_RULE:100:20260101");
        taskMapper.store(task100);

        // Pre-populate logs in order
        FinanceReviewTaskLog log1 = new FinanceReviewTaskLog();
        log1.setLogId(1L);
        log1.setTaskId(100L);
        log1.setActionType("IN_PROGRESS");
        log1.setActionTime(new Date(1000000L));

        FinanceReviewTaskLog log2 = new FinanceReviewTaskLog();
        log2.setLogId(2L);
        log2.setTaskId(100L);
        log2.setActionType("DONE");
        log2.setActionTime(new Date(2000000L));

        logMapper.preloadedLogs.put(100L, Arrays.asList(log1, log2));

        FinanceReviewTaskServiceImpl service = createService(taskMapper, logMapper);
        List<FinanceReviewTaskLog> result = service.getTaskLogs(100L);

        assertEquals(2, result.size());
        assertEquals("IN_PROGRESS", result.get(0).getActionType());
        assertEquals("DONE", result.get(1).getActionType());
        assertTrue(result.get(0).getActionTime().before(result.get(1).getActionTime()),
                "Logs should be ordered by actionTime ascending");
    }

    @Test
    void taskLog_containsCorrectBeforeAfterStatus() throws Exception {
        FakeReviewTaskMapper taskMapper = new FakeReviewTaskMapper();
        RecordingReviewTaskLogMapper logMapper = new RecordingReviewTaskLogMapper();

        // PENDING -> IN_PROGRESS
        FinanceReviewTask task = createPendingTask(taskMapper);
        FinanceReviewTaskServiceImpl service = createService(taskMapper, logMapper);
        service.markInProgress(task.getTaskId(), 1L, "admin");

        FinanceReviewTaskLog log = logMapper.insertedLogs.get(0);
        assertEquals("PENDING", log.getBeforeStatus(), "beforeStatus should be PENDING");
        assertEquals("IN_PROGRESS", log.getAfterStatus(), "afterStatus should be IN_PROGRESS");

        // IN_PROGRESS -> DONE (need a new service since markInProgress changed the task)
        logMapper.insertedLogs.clear();
        service.markDone(task.getTaskId(), 2L, "user2", "Done now");

        FinanceReviewTaskLog doneLog = logMapper.insertedLogs.get(0);
        assertEquals("IN_PROGRESS", doneLog.getBeforeStatus(), "beforeStatus should be IN_PROGRESS");
        assertEquals("DONE", doneLog.getAfterStatus(), "afterStatus should be DONE");
    }

    @Test
    void getTaskLogs_rejectsUnauthorizedDeptAccess() throws Exception {
        FakeReviewTaskMapper taskMapper = new FakeReviewTaskMapper();
        RecordingReviewTaskLogMapper logMapper = new RecordingReviewTaskLogMapper();

        // Create a task belonging to deptId=999
        FinanceReviewTask task = new FinanceReviewTask();
        task.setTaskId(taskMapper.nextId());
        task.setTaskType("TEST_RULE");
        task.setDeptId(999L);
        task.setDeptName("Other Store");
        task.setTaskDate(new Date());
        task.setStatus("PENDING");
        task.setSeverity("MEDIUM");
        task.setTitle("Other Store Task");
        task.setAlertId("FIN_REVIEW:TEST_RULE:999:20260101");
        taskMapper.store(task);

        // Set up non-admin user (userId=999, deptId=1)
        setupSecurityContext(999L, "operator", 1L);
        FinanceReviewTaskServiceImpl service = new FinanceReviewTaskServiceImpl();
        setField(service, "reviewTaskMapper", taskMapper);
        setField(service, "reviewTaskLogMapper", logMapper);
        setField(service, "finSaleRecordMapper", new NoOpSaleMapper());
        setField(service, "finExpenseMapper", new NoOpExpenseMapper());
        setField(service, "finProfitShareRecordMapper", new NoOpProfitShareMapper());
        setField(service, "remoteUserService", null);

        // Non-admin user with deptId=1 should NOT access deptId=999 task logs
        assertThrows(Exception.class, () -> service.getTaskLogs(task.getTaskId()),
                "Non-admin user must not access task logs of unauthorized departments");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FAKE IMPLEMENTATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * RecordingReviewTaskLogMapper — records all inserted logs and serves preloaded data.
     */
    static class RecordingReviewTaskLogMapper implements FinanceReviewTaskLogMapper {

        final List<FinanceReviewTaskLog> insertedLogs = new ArrayList<>();
        final Map<Long, List<FinanceReviewTaskLog>> preloadedLogs = new HashMap<>();
        private final AtomicLong idSequence = new AtomicLong(1);

        @Override
        public int insertFinanceReviewTaskLog(FinanceReviewTaskLog log) {
            if (log.getLogId() == null) {
                log.setLogId(idSequence.getAndIncrement());
            }
            insertedLogs.add(log);
            return 1;
        }

        @Override
        public List<FinanceReviewTaskLog> selectLogsByTaskId(Long taskId) {
            List<FinanceReviewTaskLog> logs = preloadedLogs.get(taskId);
            return logs != null ? new ArrayList<>(logs) : Collections.emptyList();
        }
    }

    /**
     * FakeReviewTaskMapper — stores tasks in a HashMap keyed by taskId.
     */
    static class FakeReviewTaskMapper implements FinanceReviewTaskMapper {

        private final Map<Long, FinanceReviewTask> store = new HashMap<>();
        private final AtomicLong idSequence = new AtomicLong(1);

        Long nextId() {
            return idSequence.getAndIncrement();
        }

        void store(FinanceReviewTask task) {
            store.put(task.getTaskId(), task);
        }

        @Override
        public List<FinanceReviewTask> selectReviewTaskList(Map<String, Object> params) {
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

        @Override
        public Map<String, Object> selectTaskEffectAmountWindow(Long deptId, Date startTime, Date endTime) {
            Map<String, Object> result = new HashMap<>();
            result.put("salesAmount", BigDecimal.ZERO);
            result.put("expenseAmount", BigDecimal.ZERO);
            return result;
        }

        @Override
        public int countSimilarOpenTasks(Long deptId, String problemType, Date startTime, Date endTime) {
            return 0;
        }

        @Override
        public List<FinanceReviewTask> selectRecentDoneTasks(List<Long> deptIds, Date sinceDate, int limit) {
            return Collections.emptyList();
        }

        @Override
        public List<FinanceReviewTask> selectReopenCandidates(List<Long> deptIds, Date cutoffDate, int limit) {
            return Collections.emptyList();
        }
    }

    static class NoOpExpenseMapper implements FinExpenseMapper {
        @Override public List<FinExpense> selectFinExpenseByExpenseIdsScoped(List<Long> ids, Long tenantId, Long deptId) { return Collections.emptyList(); }
        @Override public int markExpenseVerified(Long id, Long advanceId, String by, Date time, Long tenantId, Long deptId) { return 1; }
        @Override public int restoreExpenseUnverified(Long id) { return 1; }
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

    // ─── NoOp sale mapper ──────────────────────────────────────────────────

    static class NoOpSaleMapper implements FinSaleRecordMapper {
        @Override public FinSaleRecord selectFinSaleRecordBySaleId(Long id) { return null; }
        @Override public FinSaleRecord selectFinSaleRecordBySaleIdForUpdate(Long id) { return selectFinSaleRecordBySaleId(id); }
        @Override public List<FinSaleRecord> selectFinSaleRecordList(FinSaleRecord r) { return Collections.emptyList(); }
        @Override public int insertFinSaleRecord(FinSaleRecord r) { return 0; }
        @Override public int updateFinSaleRecord(FinSaleRecord r) { return 0; }
        @Override public int updatePaidAmountAndStatus(Long saleId, java.math.BigDecimal paidAmount, String status) { return 0; }
        @Override public java.util.List<FinSaleRecord> selectReceivableList(FinSaleRecord r) { return java.util.Collections.emptyList(); }
        @Override public int countReceivableByPeriodId(Long deptId, Long periodId) { return 0; }
        @Override public java.math.BigDecimal sumReceivableByPeriodId(Long deptId, Long periodId) { return java.math.BigDecimal.ZERO; }
        @Override public int deleteFinSaleRecordBySaleId(Long id) { return 0; }
        @Override public int deleteFinSaleRecordBySaleIds(Long[] ids) { return 0; }
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
        @Override public int updateShareTimeByPeriodId(Long periodId, Date shareTime, String updateBy, String remark) { return 0; }
    }
}
