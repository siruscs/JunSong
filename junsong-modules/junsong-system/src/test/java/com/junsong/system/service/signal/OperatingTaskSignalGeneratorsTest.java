package com.junsong.system.service.signal;

import com.junsong.system.domain.SysOperatingTask;
import com.junsong.system.mapper.SysOperatingTaskMapper;
import com.junsong.system.service.AuthorizedDeptResolver;
import com.junsong.system.service.ISysOperatingTaskService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 经营任务信号生成器单元测试。
 *
 * 测试策略：JUnit5 + Mockito，无 Spring Context，用反射注入字段。
 * 参考 SystemWorkbenchServiceImplTest 的测试风格。
 */
class OperatingTaskSignalGeneratorsTest {

    // ==================== OperatingTaskDueTimeCalculator 测试 ====================

    @Test
    void OperatingTaskDueTimeCalculator_allRules() {
        OperatingTaskDueTimeCalculator calc = new OperatingTaskDueTimeCalculator();
        Date occurTime = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(occurTime);

        // RECEIVABLE_COLLECTION 有 nextFollowTime → 返回 nextFollowTime
        Date nextFollow = new Date(occurTime.getTime() + 86400000L);
        Date result = calc.calculateDueTime("RECEIVABLE_COLLECTION", occurTime, nextFollow);
        assertEquals(nextFollow, result);

        // RECEIVABLE_COLLECTION 无 nextFollowTime → occurTime + 3天
        result = calc.calculateDueTime("RECEIVABLE_COLLECTION", occurTime, null);
        assertEquals(addDays(occurTime, 3), result);

        // OVERDUE_RECEIVABLE 无 nextFollowTime → occurTime + 3天
        result = calc.calculateDueTime("OVERDUE_RECEIVABLE", occurTime, null);
        assertEquals(addDays(occurTime, 3), result);

        // REVIEW_TASK → occurTime + 7天
        result = calc.calculateDueTime("REVIEW_TASK", occurTime, null);
        assertEquals(addDays(occurTime, 7), result);

        // NEGATIVE_STOCK → occurTime + 1天
        result = calc.calculateDueTime("NEGATIVE_STOCK", occurTime, null);
        assertEquals(addDays(occurTime, 1), result);

        // LOW_STOCK → occurTime + 1天
        result = calc.calculateDueTime("LOW_STOCK", occurTime, null);
        assertEquals(addDays(occurTime, 1), result);

        // SILENT_MEMBER_HIGH → occurTime + 14天
        result = calc.calculateDueTime("SILENT_MEMBER_HIGH", occurTime, null);
        assertEquals(addDays(occurTime, 14), result);

        // POINTS_LIABILITY_HIGH → occurTime + 14天
        result = calc.calculateDueTime("POINTS_LIABILITY_HIGH", occurTime, null);
        assertEquals(addDays(occurTime, 14), result);

        // 未知类型 → occurTime + 7天（默认）
        result = calc.calculateDueTime("UNKNOWN_TYPE", occurTime, null);
        assertEquals(addDays(occurTime, 7), result);

        // occurTime 为 null → 用当前时间，不抛异常
        result = calc.calculateDueTime("REVIEW_TASK", null, null);
        assertNotNull(result);

        // sourceType 为 null → 默认 + 7天
        result = calc.calculateDueTime(null, occurTime, null);
        assertEquals(addDays(occurTime, 7), result);
    }

    // ==================== OverdueReceivableSignalGenerator 测试 ====================

    @Test
    void OverdueReceivableSignalGenerator_idempotentDuplicateRun() throws Exception {
        OverdueReceivableSignalGenerator generator = new OverdueReceivableSignalGenerator();
        JdbcTemplate mockJt = mock(JdbcTemplate.class);
        ISysOperatingTaskService mockSvc = mock(ISysOperatingTaskService.class);
        SysOperatingTaskMapper mockMapper = mock(SysOperatingTaskMapper.class);
        AuthorizedDeptResolver mockResolver = mock(AuthorizedDeptResolver.class);
        OperatingTaskDueTimeCalculator calc = new OperatingTaskDueTimeCalculator();

        setField(generator, "jdbcTemplate", mockJt);
        setField(generator, "operatingTaskService", mockSvc);
        setField(generator, "operatingTaskMapper", mockMapper);
        setField(generator, "authorizedDeptResolver", mockResolver);
        setField(generator, "dueTimeCalculator", calc);

        // 超管不过滤
        when(mockResolver.resolveAuthorizedDeptIds()).thenReturn(null);

        // 模拟有一条未结清应收
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("PENDING")) {
                RowCallbackHandler handler = invocation.getArgument(1);
                ResultSet mockRs = mock(ResultSet.class);
                when(mockRs.getLong("collection_id")).thenReturn(1001L);
                when(mockRs.getObject("dept_id", Long.class)).thenReturn(200L);
                when(mockRs.getString("customer_name")).thenReturn("测试客户");
                when(mockRs.getBigDecimal("unpaid_amount")).thenReturn(new BigDecimal("3000"));
                when(mockRs.getInt("age_days")).thenReturn(20);
                when(mockRs.getString("priority_level")).thenReturn("HIGH");
                when(mockRs.getTimestamp("next_follow_time")).thenReturn(null);
                when(mockRs.getTimestamp("create_time")).thenReturn(new Timestamp(System.currentTimeMillis()));
                handler.processRow(mockRs);
            }
            return null;
        }).when(mockJt).query(contains("finance_receivable_collection"), any(RowCallbackHandler.class), any(Object[].class));

        // 第一次：返回同一引用（新建）
        // 第二次：返回不同对象（已存在）
        AtomicBoolean firstCall = new AtomicBoolean(true);
        when(mockSvc.createOrUpdateTask(any())).thenAnswer(invocation -> {
            SysOperatingTask input = invocation.getArgument(0);
            if (firstCall.getAndSet(false)) {
                input.setTaskId(1L);
                return input; // 同一引用 = 新建
            }
            SysOperatingTask existing = new SysOperatingTask();
            existing.setTaskId(1L);
            return existing; // 不同引用 = 已存在
        });

        int firstRun = generator.generate();
        int secondRun = generator.generate();

        assertEquals(1, firstRun, "首次运行应创建 1 个任务");
        assertEquals(0, secondRun, "重复运行不应创建新任务");
        verify(mockSvc, times(2)).createOrUpdateTask(any());
    }

    @Test
    void OverdueReceivableSignalGenerator_filtersByAuthorizedDepts() throws Exception {
        OverdueReceivableSignalGenerator generator = new OverdueReceivableSignalGenerator();
        JdbcTemplate mockJt = mock(JdbcTemplate.class);
        ISysOperatingTaskService mockSvc = mock(ISysOperatingTaskService.class);
        SysOperatingTaskMapper mockMapper = mock(SysOperatingTaskMapper.class);
        AuthorizedDeptResolver mockResolver = mock(AuthorizedDeptResolver.class);
        OperatingTaskDueTimeCalculator calc = new OperatingTaskDueTimeCalculator();

        setField(generator, "jdbcTemplate", mockJt);
        setField(generator, "operatingTaskService", mockSvc);
        setField(generator, "operatingTaskMapper", mockMapper);
        setField(generator, "authorizedDeptResolver", mockResolver);
        setField(generator, "dueTimeCalculator", calc);

        // 非 admin：授权门店 [100, 200]
        when(mockResolver.resolveAuthorizedDeptIds()).thenReturn(Arrays.asList(100L, 200L));
        when(mockSvc.createOrUpdateTask(any())).thenAnswer(invocation -> invocation.getArgument(0));

        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("PENDING")) {
                RowCallbackHandler handler = invocation.getArgument(1);
                ResultSet mockRs = mock(ResultSet.class);
                when(mockRs.getLong("collection_id")).thenReturn(1001L);
                when(mockRs.getObject("dept_id", Long.class)).thenReturn(100L);
                when(mockRs.getString("customer_name")).thenReturn("客户A");
                when(mockRs.getBigDecimal("unpaid_amount")).thenReturn(new BigDecimal("1000"));
                when(mockRs.getInt("age_days")).thenReturn(10);
                when(mockRs.getString("priority_level")).thenReturn("MEDIUM");
                when(mockRs.getTimestamp("next_follow_time")).thenReturn(null);
                when(mockRs.getTimestamp("create_time")).thenReturn(new Timestamp(System.currentTimeMillis()));
                handler.processRow(mockRs);
            }
            return null;
        }).when(mockJt).query(contains("finance_receivable_collection"), any(RowCallbackHandler.class), any(Object[].class));

        generator.generate();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockJt, atLeastOnce()).query(sqlCaptor.capture(), any(RowCallbackHandler.class), any(Object[].class));
        String scanSql = sqlCaptor.getAllValues().stream()
                .filter(s -> s.contains("PENDING"))
                .findFirst()
                .orElse(null);
        assertNotNull(scanSql, "应有未结清应收查询");
        assertTrue(scanSql.contains("dept_id IN (100,200)"),
                "非超管查询应包含 dept_id IN (100,200)，实际 SQL: " + scanSql);
    }

    @Test
    void OverdueReceivableSignalGenerator_priorityMapping() throws Exception {
        OverdueReceivableSignalGenerator generator = new OverdueReceivableSignalGenerator();
        JdbcTemplate mockJt = mock(JdbcTemplate.class);
        ISysOperatingTaskService mockSvc = mock(ISysOperatingTaskService.class);
        SysOperatingTaskMapper mockMapper = mock(SysOperatingTaskMapper.class);
        AuthorizedDeptResolver mockResolver = mock(AuthorizedDeptResolver.class);
        OperatingTaskDueTimeCalculator calc = new OperatingTaskDueTimeCalculator();

        setField(generator, "jdbcTemplate", mockJt);
        setField(generator, "operatingTaskService", mockSvc);
        setField(generator, "operatingTaskMapper", mockMapper);
        setField(generator, "authorizedDeptResolver", mockResolver);
        setField(generator, "dueTimeCalculator", calc);

        when(mockResolver.resolveAuthorizedDeptIds()).thenReturn(null);
        when(mockSvc.createOrUpdateTask(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // ageDays=45, unpaid=8000 → URGENT
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("PENDING")) {
                RowCallbackHandler handler = invocation.getArgument(1);
                ResultSet mockRs = mock(ResultSet.class);
                when(mockRs.getLong("collection_id")).thenReturn(1001L);
                when(mockRs.getObject("dept_id", Long.class)).thenReturn(200L);
                when(mockRs.getString("customer_name")).thenReturn("大客户");
                when(mockRs.getBigDecimal("unpaid_amount")).thenReturn(new BigDecimal("8000"));
                when(mockRs.getInt("age_days")).thenReturn(45);
                when(mockRs.getString("priority_level")).thenReturn("CRITICAL");
                when(mockRs.getTimestamp("next_follow_time")).thenReturn(null);
                when(mockRs.getTimestamp("create_time")).thenReturn(new Timestamp(System.currentTimeMillis()));
                handler.processRow(mockRs);
            }
            return null;
        }).when(mockJt).query(contains("finance_receivable_collection"), any(RowCallbackHandler.class), any(Object[].class));

        generator.generate();

        ArgumentCaptor<SysOperatingTask> taskCaptor = ArgumentCaptor.forClass(SysOperatingTask.class);
        verify(mockSvc).createOrUpdateTask(taskCaptor.capture());
        SysOperatingTask task = taskCaptor.getValue();
        assertEquals("URGENT", task.getPriority(), "ageDays>30 且 unpaid≥5000 应为 URGENT");
        assertEquals("HIGH", task.getSeverity(), "CRITICAL → HIGH");
        assertEquals("FINANCE", task.getSourceModule());
        assertEquals("RECEIVABLE_COLLECTION", task.getSourceType());
        assertEquals("1001", task.getSourceId());
        assertEquals(new BigDecimal("8000.00"), task.getImpactAmount());
    }

    @Test
    void OverdueReceivableSignalGenerator_autoCompleteWhenSourcePaid() throws Exception {
        OverdueReceivableSignalGenerator generator = new OverdueReceivableSignalGenerator();
        JdbcTemplate mockJt = mock(JdbcTemplate.class);
        ISysOperatingTaskService mockSvc = mock(ISysOperatingTaskService.class);
        SysOperatingTaskMapper mockMapper = mock(SysOperatingTaskMapper.class);
        AuthorizedDeptResolver mockResolver = mock(AuthorizedDeptResolver.class);
        OperatingTaskDueTimeCalculator calc = new OperatingTaskDueTimeCalculator();

        setField(generator, "jdbcTemplate", mockJt);
        setField(generator, "operatingTaskService", mockSvc);
        setField(generator, "operatingTaskMapper", mockMapper);
        setField(generator, "authorizedDeptResolver", mockResolver);
        setField(generator, "dueTimeCalculator", calc);

        when(mockResolver.resolveAuthorizedDeptIds()).thenReturn(null);

        // scan 查询无数据（不处理行）
        // auto-complete 查询返回一条 PAID 应收
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("PAID")) {
                RowCallbackHandler handler = invocation.getArgument(1);
                ResultSet mockRs = mock(ResultSet.class);
                when(mockRs.getLong("collection_id")).thenReturn(1001L);
                handler.processRow(mockRs);
            }
            return null;
        }).when(mockJt).query(contains("finance_receivable_collection"), any(RowCallbackHandler.class), any(Object[].class));

        // 模拟已有 PENDING 经营任务
        SysOperatingTask existingTask = new SysOperatingTask();
        existingTask.setTaskId(55L);
        existingTask.setStatus("PENDING");
        existingTask.setVersion(0);
        when(mockMapper.selectByIdempotencyKey(any(), anyString())).thenReturn(existingTask);
        when(mockMapper.conditionalUpdateStatus(anyLong(), anyString(), anyInt(), anyString(),
                nullable(Long.class), nullable(String.class), anyString(), nullable(String.class), nullable(Integer.class)))
                .thenReturn(1);

        generator.generate();

        verify(mockMapper).conditionalUpdateStatus(
                eq(55L), eq("PENDING"), eq(0), eq("DONE"),
                isNull(), isNull(), eq("来源已关闭，自动完成"), isNull(), isNull());
    }

    // ==================== UnverifiedExpenseSignalGenerator 测试 ====================

    @Test
    void UnverifiedExpenseSignalGenerator_autoCompleteWhenVerified() throws Exception {
        UnverifiedExpenseSignalGenerator generator = new UnverifiedExpenseSignalGenerator();
        JdbcTemplate mockJt = mock(JdbcTemplate.class);
        ISysOperatingTaskService mockSvc = mock(ISysOperatingTaskService.class);
        SysOperatingTaskMapper mockMapper = mock(SysOperatingTaskMapper.class);
        AuthorizedDeptResolver mockResolver = mock(AuthorizedDeptResolver.class);
        OperatingTaskDueTimeCalculator calc = new OperatingTaskDueTimeCalculator();

        setField(generator, "jdbcTemplate", mockJt);
        setField(generator, "operatingTaskService", mockSvc);
        setField(generator, "operatingTaskMapper", mockMapper);
        setField(generator, "authorizedDeptResolver", mockResolver);
        setField(generator, "dueTimeCalculator", calc);

        when(mockResolver.resolveAuthorizedDeptIds()).thenReturn(null);

        // scan 查询无数据；auto-complete 查询返回一条已核销费用
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("status = '1'")) {
                RowCallbackHandler handler = invocation.getArgument(1);
                ResultSet mockRs = mock(ResultSet.class);
                when(mockRs.getLong("expense_id")).thenReturn(2001L);
                handler.processRow(mockRs);
            }
            return null;
        }).when(mockJt).query(contains("fin_expense"), any(RowCallbackHandler.class), any(Object[].class));

        SysOperatingTask existingTask = new SysOperatingTask();
        existingTask.setTaskId(66L);
        existingTask.setStatus("IN_PROGRESS");
        existingTask.setVersion(1);
        when(mockMapper.selectByIdempotencyKey(any(), anyString())).thenReturn(existingTask);
        when(mockMapper.conditionalUpdateStatus(anyLong(), anyString(), anyInt(), anyString(),
                nullable(Long.class), nullable(String.class), anyString(), nullable(String.class), nullable(Integer.class)))
                .thenReturn(1);

        generator.generate();

        verify(mockMapper).conditionalUpdateStatus(
                eq(66L), eq("IN_PROGRESS"), eq(1), eq("DONE"),
                isNull(), isNull(), eq("来源已关闭，自动完成"), isNull(), isNull());
    }

    @Test
    void UnverifiedExpenseSignalGenerator_filtersByTenantId() throws Exception {
        UnverifiedExpenseSignalGenerator generator = new UnverifiedExpenseSignalGenerator();
        JdbcTemplate mockJt = mock(JdbcTemplate.class);
        ISysOperatingTaskService mockSvc = mock(ISysOperatingTaskService.class);
        SysOperatingTaskMapper mockMapper = mock(SysOperatingTaskMapper.class);
        AuthorizedDeptResolver mockResolver = mock(AuthorizedDeptResolver.class);
        OperatingTaskDueTimeCalculator calc = new OperatingTaskDueTimeCalculator();

        setField(generator, "jdbcTemplate", mockJt);
        setField(generator, "operatingTaskService", mockSvc);
        setField(generator, "operatingTaskMapper", mockMapper);
        setField(generator, "authorizedDeptResolver", mockResolver);
        setField(generator, "dueTimeCalculator", calc);

        when(mockResolver.resolveAuthorizedDeptIds()).thenReturn(null);
        when(mockSvc.createOrUpdateTask(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // scan 查询返回一条未核销费用
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("status = '0'")) {
                RowCallbackHandler handler = invocation.getArgument(1);
                ResultSet mockRs = mock(ResultSet.class);
                when(mockRs.getLong("expense_id")).thenReturn(2001L);
                when(mockRs.getObject("dept_id", Long.class)).thenReturn(100L);
                when(mockRs.getString("expense_no")).thenReturn("EXP-001");
                when(mockRs.getBigDecimal("expense_amount")).thenReturn(new BigDecimal("3000"));
                when(mockRs.getTimestamp("expense_date")).thenReturn(new Timestamp(System.currentTimeMillis()));
                handler.processRow(mockRs);
            }
            return null;
        }).when(mockJt).query(contains("fin_expense"), any(RowCallbackHandler.class), any(Object[].class));

        generator.generate();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockJt, atLeastOnce()).query(sqlCaptor.capture(), any(RowCallbackHandler.class), any(Object[].class));
        String scanSql = sqlCaptor.getAllValues().stream()
                .filter(s -> s.contains("status = '0'"))
                .findFirst()
                .orElse(null);
        assertNotNull(scanSql, "应有未核销费用查询");
        assertTrue(scanSql.contains("tenant_id = ?"),
                "fin_expense 查询应包含 tenant_id = ? 条件，实际 SQL: " + scanSql);
    }

    // ==================== StockRiskSignalGenerator 测试 ====================

    @Test
    void StockRiskSignalGenerator_generatesPerDept() throws Exception {
        StockRiskSignalGenerator generator = new StockRiskSignalGenerator();
        JdbcTemplate mockJt = mock(JdbcTemplate.class);
        ISysOperatingTaskService mockSvc = mock(ISysOperatingTaskService.class);
        SysOperatingTaskMapper mockMapper = mock(SysOperatingTaskMapper.class);
        AuthorizedDeptResolver mockResolver = mock(AuthorizedDeptResolver.class);
        OperatingTaskDueTimeCalculator calc = new OperatingTaskDueTimeCalculator();

        setField(generator, "jdbcTemplate", mockJt);
        setField(generator, "operatingTaskService", mockSvc);
        setField(generator, "operatingTaskMapper", mockMapper);
        setField(generator, "authorizedDeptResolver", mockResolver);
        setField(generator, "dueTimeCalculator", calc);

        when(mockResolver.resolveAuthorizedDeptIds()).thenReturn(null);
        // 返回同一引用，表示新建
        when(mockSvc.createOrUpdateTask(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // 负库存查询返回 2 个门店
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("fin_stock_ledger")) {
                RowCallbackHandler handler = invocation.getArgument(1);
                ResultSet mockRs1 = mock(ResultSet.class);
                when(mockRs1.getObject("dept_id", Long.class)).thenReturn(100L);
                when(mockRs1.getLong("neg_cnt")).thenReturn(3L);
                handler.processRow(mockRs1);
                ResultSet mockRs2 = mock(ResultSet.class);
                when(mockRs2.getObject("dept_id", Long.class)).thenReturn(200L);
                when(mockRs2.getLong("neg_cnt")).thenReturn(5L);
                handler.processRow(mockRs2);
            }
            return null;
        }).when(mockJt).query(contains("fin_stock_ledger"), any(RowCallbackHandler.class), any(Object[].class));

        // sys_operating_task 查询无数据（auto-complete 无操作）
        doNothing().when(mockJt).query(contains("sys_operating_task"), any(RowCallbackHandler.class), any(Object[].class));

        int created = generator.generate();

        assertEquals(2, created, "应为 2 个门店各生成 1 个任务");

        ArgumentCaptor<SysOperatingTask> taskCaptor = ArgumentCaptor.forClass(SysOperatingTask.class);
        verify(mockSvc, times(2)).createOrUpdateTask(taskCaptor.capture());
        List<SysOperatingTask> tasks = taskCaptor.getAllValues();
        assertEquals("URGENT", tasks.get(0).getPriority(), "负库存优先级应为 URGENT");
        assertEquals("HIGH", tasks.get(0).getSeverity());
        assertEquals("STOCK", tasks.get(0).getSourceModule());
        assertEquals("NEGATIVE_STOCK", tasks.get(0).getSourceType());
    }

    // ==================== ReviewTaskSignalGenerator 测试 ====================

    @Test
    void ReviewTaskSignalGenerator_dueTimePlus7Days() throws Exception {
        ReviewTaskSignalGenerator generator = new ReviewTaskSignalGenerator();
        JdbcTemplate mockJt = mock(JdbcTemplate.class);
        ISysOperatingTaskService mockSvc = mock(ISysOperatingTaskService.class);
        SysOperatingTaskMapper mockMapper = mock(SysOperatingTaskMapper.class);
        AuthorizedDeptResolver mockResolver = mock(AuthorizedDeptResolver.class);
        OperatingTaskDueTimeCalculator calc = new OperatingTaskDueTimeCalculator();

        setField(generator, "jdbcTemplate", mockJt);
        setField(generator, "operatingTaskService", mockSvc);
        setField(generator, "operatingTaskMapper", mockMapper);
        setField(generator, "authorizedDeptResolver", mockResolver);
        setField(generator, "dueTimeCalculator", calc);

        when(mockResolver.resolveAuthorizedDeptIds()).thenReturn(null);
        when(mockSvc.createOrUpdateTask(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Date occurTime = new Date();
        Timestamp occurTs = new Timestamp(occurTime.getTime());

        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("PENDING")) {
                RowCallbackHandler handler = invocation.getArgument(1);
                ResultSet mockRs = mock(ResultSet.class);
                when(mockRs.getLong("task_id")).thenReturn(3001L);
                when(mockRs.getObject("dept_id", Long.class)).thenReturn(200L);
                when(mockRs.getString("dept_name")).thenReturn("测试门店");
                when(mockRs.getString("severity")).thenReturn("HIGH");
                when(mockRs.getString("title")).thenReturn("销售下滑");
                when(mockRs.getString("target_route")).thenReturn("/finance/reviewTask");
                when(mockRs.getBigDecimal("impact_amount")).thenReturn(new BigDecimal("5000"));
                when(mockRs.getTimestamp("create_time")).thenReturn(occurTs);
                when(mockRs.getTimestamp("task_date")).thenReturn(occurTs);
                handler.processRow(mockRs);
            }
            return null;
        }).when(mockJt).query(contains("finance_review_task"), any(RowCallbackHandler.class));

        generator.generate();

        ArgumentCaptor<SysOperatingTask> taskCaptor = ArgumentCaptor.forClass(SysOperatingTask.class);
        verify(mockSvc).createOrUpdateTask(taskCaptor.capture());
        SysOperatingTask task = taskCaptor.getValue();
        // due_time = occur_time + 7天
        assertEquals(addDays(occurTime, 7), task.getDueTime(),
                "REVIEW_TASK 的 due_time 应为 occur_time + 7天");
        assertEquals("HIGH", task.getPriority(), "severity=HIGH → priority=HIGH");
        assertEquals("财务复盘：销售下滑", task.getTitle());
    }

    @Test
    void ReviewTaskSignalGenerator_autoCompleteWhenSourceDone() throws Exception {
        ReviewTaskSignalGenerator generator = new ReviewTaskSignalGenerator();
        JdbcTemplate mockJt = mock(JdbcTemplate.class);
        ISysOperatingTaskService mockSvc = mock(ISysOperatingTaskService.class);
        SysOperatingTaskMapper mockMapper = mock(SysOperatingTaskMapper.class);
        AuthorizedDeptResolver mockResolver = mock(AuthorizedDeptResolver.class);
        OperatingTaskDueTimeCalculator calc = new OperatingTaskDueTimeCalculator();

        setField(generator, "jdbcTemplate", mockJt);
        setField(generator, "operatingTaskService", mockSvc);
        setField(generator, "operatingTaskMapper", mockMapper);
        setField(generator, "authorizedDeptResolver", mockResolver);
        setField(generator, "dueTimeCalculator", calc);

        when(mockResolver.resolveAuthorizedDeptIds()).thenReturn(null);

        // scan 查询无数据；auto-complete 查询返回一条 DONE 复盘
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("DONE")) {
                RowCallbackHandler handler = invocation.getArgument(1);
                ResultSet mockRs = mock(ResultSet.class);
                when(mockRs.getLong("task_id")).thenReturn(3001L);
                handler.processRow(mockRs);
            }
            return null;
        }).when(mockJt).query(contains("finance_review_task"), any(RowCallbackHandler.class));

        SysOperatingTask existingTask = new SysOperatingTask();
        existingTask.setTaskId(77L);
        existingTask.setStatus("PENDING");
        existingTask.setVersion(0);
        when(mockMapper.selectByIdempotencyKey(any(), anyString())).thenReturn(existingTask);
        when(mockMapper.conditionalUpdateStatus(anyLong(), anyString(), anyInt(), anyString(),
                nullable(Long.class), nullable(String.class), anyString(), nullable(String.class), nullable(Integer.class)))
                .thenReturn(1);

        generator.generate();

        verify(mockMapper).conditionalUpdateStatus(
                eq(77L), eq("PENDING"), eq(0), eq("DONE"),
                isNull(), isNull(), eq("来源已关闭，自动完成"), isNull(), isNull());
    }

    // ==================== MemberActionSignalGenerator 测试 ====================

    @Test
    void MemberActionSignalGenerator_silentMemberHigh() throws Exception {
        MemberActionSignalGenerator generator = new MemberActionSignalGenerator();
        JdbcTemplate mockJt = mock(JdbcTemplate.class);
        ISysOperatingTaskService mockSvc = mock(ISysOperatingTaskService.class);
        SysOperatingTaskMapper mockMapper = mock(SysOperatingTaskMapper.class);
        AuthorizedDeptResolver mockResolver = mock(AuthorizedDeptResolver.class);
        OperatingTaskDueTimeCalculator calc = new OperatingTaskDueTimeCalculator();

        setField(generator, "jdbcTemplate", mockJt);
        setField(generator, "operatingTaskService", mockSvc);
        setField(generator, "operatingTaskMapper", mockMapper);
        setField(generator, "authorizedDeptResolver", mockResolver);
        setField(generator, "dueTimeCalculator", calc);

        when(mockResolver.resolveAuthorizedDeptIds()).thenReturn(null);
        when(mockSvc.createOrUpdateTask(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // 沉默会员查询：100 总会员，10 活跃 → 90% 沉默 > 30%
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("mem_member m")) {
                RowCallbackHandler handler = invocation.getArgument(1);
                ResultSet mockRs = mock(ResultSet.class);
                when(mockRs.getObject("dept_id", Long.class)).thenReturn(300L);
                when(mockRs.getLong("total_members")).thenReturn(100L);
                when(mockRs.getLong("active_members")).thenReturn(10L);
                handler.processRow(mockRs);
            }
            return null;
        }).when(mockJt).query(contains("mem_member m"), any(RowCallbackHandler.class), any(Object[].class));

        // 积分负债查询无数据
        doNothing().when(mockJt).query(contains("mem_points_record r"), any(RowCallbackHandler.class), any(Object[].class));
        // auto-complete 查询无数据
        doNothing().when(mockJt).query(contains("sys_operating_task"), any(RowCallbackHandler.class), any(Object[].class));

        int created = generator.generate();

        assertEquals(1, created, "应检测到 1 个 SILENT_MEMBER_HIGH 任务");

        ArgumentCaptor<SysOperatingTask> taskCaptor = ArgumentCaptor.forClass(SysOperatingTask.class);
        verify(mockSvc).createOrUpdateTask(taskCaptor.capture());
        SysOperatingTask task = taskCaptor.getValue();
        assertEquals("MEMBER", task.getSourceModule());
        assertEquals("SILENT_MEMBER_HIGH", task.getSourceType());
        assertEquals("HIGH", task.getPriority());
        assertEquals("HIGH", task.getSeverity());
        assertEquals("300", task.getSourceId());
        assertEquals(Long.valueOf(300L), task.getDeptId());
    }

    @Test
    void MemberActionSignalGenerator_pointsLiabilityHigh() throws Exception {
        MemberActionSignalGenerator generator = new MemberActionSignalGenerator();
        JdbcTemplate mockJt = mock(JdbcTemplate.class);
        ISysOperatingTaskService mockSvc = mock(ISysOperatingTaskService.class);
        SysOperatingTaskMapper mockMapper = mock(SysOperatingTaskMapper.class);
        AuthorizedDeptResolver mockResolver = mock(AuthorizedDeptResolver.class);
        OperatingTaskDueTimeCalculator calc = new OperatingTaskDueTimeCalculator();

        setField(generator, "jdbcTemplate", mockJt);
        setField(generator, "operatingTaskService", mockSvc);
        setField(generator, "operatingTaskMapper", mockMapper);
        setField(generator, "authorizedDeptResolver", mockResolver);
        setField(generator, "dueTimeCalculator", calc);

        when(mockResolver.resolveAuthorizedDeptIds()).thenReturn(null);
        when(mockSvc.createOrUpdateTask(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // 沉默会员查询无数据
        doNothing().when(mockJt).query(contains("mem_member m"), any(RowCallbackHandler.class), any(Object[].class));

        // 积分负债查询：total_balance = 200000 → liability = 2000 > 1000
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("mem_points_record r")) {
                RowCallbackHandler handler = invocation.getArgument(1);
                ResultSet mockRs = mock(ResultSet.class);
                when(mockRs.getObject("dept_id", Long.class)).thenReturn(400L);
                when(mockRs.getBigDecimal("total_balance")).thenReturn(new BigDecimal("200000"));
                handler.processRow(mockRs);
            }
            return null;
        }).when(mockJt).query(contains("mem_points_record r"), any(RowCallbackHandler.class), any(Object[].class));

        // auto-complete 查询无数据
        doNothing().when(mockJt).query(contains("sys_operating_task"), any(RowCallbackHandler.class), any(Object[].class));

        int created = generator.generate();

        assertEquals(1, created, "应检测到 1 个 POINTS_LIABILITY_HIGH 任务");

        ArgumentCaptor<SysOperatingTask> taskCaptor = ArgumentCaptor.forClass(SysOperatingTask.class);
        verify(mockSvc).createOrUpdateTask(taskCaptor.capture());
        SysOperatingTask task = taskCaptor.getValue();
        assertEquals("MEMBER", task.getSourceModule());
        assertEquals("POINTS_LIABILITY_HIGH", task.getSourceType());
        assertEquals("MEDIUM", task.getPriority());
        assertEquals("MEDIUM", task.getSeverity());
        assertEquals("400", task.getSourceId());
        assertEquals(new BigDecimal("2000.00"), task.getImpactAmount());
    }

    @Test
    void MemberActionSignalGenerator_filtersByTenantId() throws Exception {
        MemberActionSignalGenerator generator = new MemberActionSignalGenerator();
        JdbcTemplate mockJt = mock(JdbcTemplate.class);
        ISysOperatingTaskService mockSvc = mock(ISysOperatingTaskService.class);
        SysOperatingTaskMapper mockMapper = mock(SysOperatingTaskMapper.class);
        AuthorizedDeptResolver mockResolver = mock(AuthorizedDeptResolver.class);
        OperatingTaskDueTimeCalculator calc = new OperatingTaskDueTimeCalculator();

        setField(generator, "jdbcTemplate", mockJt);
        setField(generator, "operatingTaskService", mockSvc);
        setField(generator, "operatingTaskMapper", mockMapper);
        setField(generator, "authorizedDeptResolver", mockResolver);
        setField(generator, "dueTimeCalculator", calc);

        when(mockResolver.resolveAuthorizedDeptIds()).thenReturn(null);
        when(mockSvc.createOrUpdateTask(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // 沉默会员查询返回一条数据
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("mem_member m")) {
                RowCallbackHandler handler = invocation.getArgument(1);
                ResultSet mockRs = mock(ResultSet.class);
                when(mockRs.getObject("dept_id", Long.class)).thenReturn(300L);
                when(mockRs.getLong("total_members")).thenReturn(100L);
                when(mockRs.getLong("active_members")).thenReturn(10L);
                handler.processRow(mockRs);
            }
            return null;
        }).when(mockJt).query(contains("mem_member m"), any(RowCallbackHandler.class), any(Object[].class));

        // 积分负债查询无数据
        doNothing().when(mockJt).query(contains("mem_points_record r"), any(RowCallbackHandler.class), any(Object[].class));
        // auto-complete 查询无数据
        doNothing().when(mockJt).query(contains("sys_operating_task"), any(RowCallbackHandler.class), any(Object[].class));

        generator.generate();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockJt, atLeastOnce()).query(sqlCaptor.capture(), any(RowCallbackHandler.class), any(Object[].class));
        String silentSql = sqlCaptor.getAllValues().stream()
                .filter(s -> s.contains("mem_member m"))
                .findFirst()
                .orElse(null);
        assertNotNull(silentSql, "应有沉默会员查询");
        assertTrue(silentSql.contains("m.tenant_id = ?"),
                "mem_member 查询应包含 m.tenant_id = ? 条件，实际 SQL: " + silentSql);
    }

    // ==================== OperatingTaskSignalScheduler 测试 ====================

    @Test
    void OperatingTaskSignalScheduler_runAll_isolatesFailures() throws Exception {
        OperatingTaskSignalScheduler scheduler = new OperatingTaskSignalScheduler();

        OperatingTaskSignalGenerator gen1 = mock(OperatingTaskSignalGenerator.class);
        when(gen1.generatorCode()).thenReturn("GEN_OK");
        when(gen1.generate()).thenReturn(5);

        OperatingTaskSignalGenerator gen2 = mock(OperatingTaskSignalGenerator.class);
        when(gen2.generatorCode()).thenReturn("GEN_FAIL");
        when(gen2.generate()).thenThrow(new RuntimeException("模拟失败"));

        OperatingTaskSignalGenerator gen3 = mock(OperatingTaskSignalGenerator.class);
        when(gen3.generatorCode()).thenReturn("GEN_OK2");
        when(gen3.generate()).thenReturn(3);

        setField(scheduler, "generators", Arrays.asList(gen1, gen2, gen3));

        Map<String, Integer> results = scheduler.runAll();

        assertEquals(3, results.size(), "应有 3 个生成器结果");
        assertEquals(5, results.get("GEN_OK"), "GEN_OK 应返回 5");
        assertEquals(0, results.get("GEN_FAIL"), "GEN_FAIL 失败应返回 0");
        assertEquals(3, results.get("GEN_OK2"), "失败的生成器不应阻断其他生成器");
        verify(gen1).generate();
        verify(gen2).generate();
        verify(gen3).generate();
    }

    // ==================== 工具方法 ====================

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Field findField(Class<?> clazz, String fieldName) throws Exception {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }
}
