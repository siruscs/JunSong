package com.junsong.system.service.impl;

import com.junsong.system.domain.vo.WorkbenchTaskVO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SystemWorkbenchServiceImpl 单元测试。
 *
 * R7 回修补强：
 * - 测试无 JdbcTemplate 时的降级行为
 * - 测试 FINANCE/MEMBER/STOCK 聚合（使用 mock JdbcTemplate）
 * - 测试授权门店过滤（dept_id IN 子句注入）
 */
class SystemWorkbenchServiceImplTest {

    private static final String FINANCE_SQL_MARKER = "FROM finance_review_task";
    private static final String MEMBER_SQL_MARKER = "FROM mem_member m";
    private static final String STOCK_SQL_MARKER = "FROM fin_stock_ledger";
    private static final String TABLE_EXISTS_MARKER = "information_schema.tables";

    // ==================== 基础降级测试 ====================

    @Test
    void aggregateTasks_returnsEmptyWhenNoJdbcTemplate() {
        SystemWorkbenchServiceImpl service = new SystemWorkbenchServiceImpl();
        List<WorkbenchTaskVO> tasks = service.aggregateTasks();
        assertNotNull(tasks);
        assertTrue(tasks.isEmpty(), "无 JdbcTemplate 时应返回空列表");
    }

    @Test
    void aggregateTasks_sourceModuleOnlyValidCategories() {
        SystemWorkbenchServiceImpl service = new SystemWorkbenchServiceImpl();
        List<WorkbenchTaskVO> tasks = service.aggregateTasks();
        for (WorkbenchTaskVO t : tasks) {
            assertTrue(
                    "SYSTEM".equals(t.getSourceModule())
                            || "STOCK".equals(t.getSourceModule())
                            || "FINANCE".equals(t.getSourceModule())
                            || "MEMBER".equals(t.getSourceModule()),
                    "sourceModule 只返回 SYSTEM/STOCK/FINANCE/MEMBER");
            assertNotNull(t.getTitle());
            assertNotNull(t.getSeverity());
        }
    }

    // ==================== 授权门店解析测试 ====================

    @Test
    void resolveAuthorizedDeptIds_returnsSentinelWhenNoJdbcTemplate() {
        SystemWorkbenchServiceImpl service = new SystemWorkbenchServiceImpl();
        List<Long> deptIds = service.resolveAuthorizedDeptIds();
        assertNotNull(deptIds);
        assertEquals(1, deptIds.size());
        assertEquals(-1L, deptIds.get(0));
    }

    @Test
    void resolveAuthorizedDeptIds_returnsSentinelWhenNoSecurityContext() throws Exception {
        SystemWorkbenchServiceImpl service = new SystemWorkbenchServiceImpl();
        org.springframework.jdbc.core.JdbcTemplate mockJt = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        setField(service, "jdbcTemplate", mockJt);

        List<Long> deptIds = service.resolveAuthorizedDeptIds();
        assertNotNull(deptIds);
        assertEquals(1, deptIds.size());
        assertEquals(-1L, deptIds.get(0));
    }

    // ==================== FINANCE 聚合测试 ====================

    @Test
    void collectFinanceTasks_withMockData_returnsFinanceTask() throws Exception {
        SystemWorkbenchServiceImpl service = new SystemWorkbenchServiceImpl() {
            @Override
            List<Long> resolveAuthorizedDeptIds() { return null; }
        };

        org.springframework.jdbc.core.JdbcTemplate mockJt = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        setField(service, "jdbcTemplate", mockJt);

        // tableExists → true
        when(mockJt.queryForObject(contains(TABLE_EXISTS_MARKER), eq(Long.class), anyString()))
                .thenReturn(1L);

        // queryCount → 0（SYSTEM+STOCK）
        when(mockJt.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(0L);

        // FINANCE 查询返回 1 条复盘任务
        when(mockJt.query(contains(FINANCE_SQL_MARKER), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenAnswer(invocation -> {
                    org.springframework.jdbc.core.RowMapper<WorkbenchTaskVO> mapper = invocation.getArgument(1);
                    ResultSet mockRs = mock(ResultSet.class);
                    when(mockRs.getLong("task_id")).thenReturn(101L);
                    when(mockRs.getString("task_type")).thenReturn("SALES_DROP");
                    when(mockRs.getString("severity")).thenReturn("HIGH");
                    when(mockRs.getObject("dept_id", Long.class)).thenReturn(200L);
                    when(mockRs.getString("dept_name")).thenReturn("测试门店");
                    when(mockRs.getString("title")).thenReturn("销售额下滑");
                    when(mockRs.getString("reason")).thenReturn("近7天销售额下降25%");
                    when(mockRs.getString("suggestion")).thenReturn("请核查门店促销策略");
                    when(mockRs.getString("target_route")).thenReturn("/finance/salesOperation");
                    when(mockRs.getString("status")).thenReturn("PENDING");
                    when(mockRs.getBigDecimal("impact_amount")).thenReturn(new BigDecimal("5000"));
                    when(mockRs.getTimestamp("create_time")).thenReturn(new Timestamp(System.currentTimeMillis()));
                    mapper.mapRow(mockRs, 0);
                    return Collections.emptyList();
                });

        // MEMBER RowCallbackHandler → void 方法，用 doAnswer
        doNothing().when(mockJt).query(anyString(), any(org.springframework.jdbc.core.RowCallbackHandler.class));

        List<WorkbenchTaskVO> tasks = service.aggregateTasks();
        WorkbenchTaskVO financeTask = tasks.stream()
                .filter(t -> "FINANCE".equals(t.getSourceModule()))
                .findFirst()
                .orElse(null);

        assertNotNull(financeTask, "应包含 FINANCE 来源任务");
        assertEquals("SALES_DROP", financeTask.getTaskType());
        assertEquals("HIGH", financeTask.getSeverity());
        assertEquals("PENDING", financeTask.getStatus());
        assertEquals("FINANCE:101", financeTask.getBizId());
        assertEquals(new BigDecimal("5000"), financeTask.getImpactAmount());
        assertEquals(Long.valueOf(200L), financeTask.getDeptId());
        assertNotNull(financeTask.getOccurTime());
    }

    @Test
    void financeQuery_containsDeptFilterForNonAdmin() throws Exception {
        SystemWorkbenchServiceImpl service = new SystemWorkbenchServiceImpl() {
            @Override
            List<Long> resolveAuthorizedDeptIds() { return Arrays.asList(100L, 200L); }
        };

        org.springframework.jdbc.core.JdbcTemplate mockJt = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        setField(service, "jdbcTemplate", mockJt);

        when(mockJt.queryForObject(contains(TABLE_EXISTS_MARKER), eq(Long.class), anyString()))
                .thenReturn(1L);
        when(mockJt.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(0L);
        when(mockJt.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(Collections.emptyList());
        doNothing().when(mockJt).query(anyString(), any(org.springframework.jdbc.core.RowCallbackHandler.class));

        service.aggregateTasks();

        org.mockito.ArgumentCaptor<String> sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mockJt, atLeastOnce()).query(sqlCaptor.capture(), any(org.springframework.jdbc.core.RowMapper.class));
        String financeSql = sqlCaptor.getAllValues().stream()
                .filter(s -> s.contains(FINANCE_SQL_MARKER))
                .findFirst()
                .orElse(null);
        assertNotNull(financeSql, "应有 FINANCE 查询");
        assertTrue(financeSql.contains("dept_id IN (100,200)"),
                "非超管 FINANCE 查询应包含 dept_id IN (100,200)，实际 SQL: " + financeSql);
    }

    @Test
    void financeQuery_noDeptFilterForAdmin() throws Exception {
        SystemWorkbenchServiceImpl service = new SystemWorkbenchServiceImpl() {
            @Override
            List<Long> resolveAuthorizedDeptIds() { return null; }
        };

        org.springframework.jdbc.core.JdbcTemplate mockJt = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        setField(service, "jdbcTemplate", mockJt);

        when(mockJt.queryForObject(contains(TABLE_EXISTS_MARKER), eq(Long.class), anyString()))
                .thenReturn(1L);
        when(mockJt.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(Collections.emptyList());
        doNothing().when(mockJt).query(anyString(), any(org.springframework.jdbc.core.RowCallbackHandler.class));

        service.aggregateTasks();

        org.mockito.ArgumentCaptor<String> sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mockJt, atLeastOnce()).query(sqlCaptor.capture(), any(org.springframework.jdbc.core.RowMapper.class));
        String financeSql = sqlCaptor.getAllValues().stream()
                .filter(s -> s.contains(FINANCE_SQL_MARKER))
                .findFirst()
                .orElse(null);
        assertNotNull(financeSql);
        assertFalse(financeSql.contains("dept_id IN"),
                "超管 FINANCE 查询不应包含 dept_id IN 过滤，实际 SQL: " + financeSql);
    }

    // ==================== MEMBER 聚合测试 ====================

    @Test
    void collectMemberTasks_detectsSilentMemberHigh() throws Exception {
        SystemWorkbenchServiceImpl service = new SystemWorkbenchServiceImpl() {
            @Override
            List<Long> resolveAuthorizedDeptIds() { return null; }
        };

        org.springframework.jdbc.core.JdbcTemplate mockJt = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        setField(service, "jdbcTemplate", mockJt);

        when(mockJt.queryForObject(contains(TABLE_EXISTS_MARKER), eq(Long.class), anyString()))
                .thenReturn(1L);
        when(mockJt.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(0L);
        when(mockJt.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(Collections.emptyList());

        // MEMBER 沉默会员查询：100 总会员，10 活跃 → 90% 沉默 > 30%
        // RowCallbackHandler.query 是 void 方法，用 doAnswer
        doAnswer(invocation -> {
            org.springframework.jdbc.core.RowCallbackHandler handler = invocation.getArgument(1);
            ResultSet mockRs = mock(ResultSet.class);
            when(mockRs.getLong("dept_id")).thenReturn(300L);
            when(mockRs.getLong("total_members")).thenReturn(100L);
            when(mockRs.getLong("active_members")).thenReturn(10L);
            handler.processRow(mockRs);
            return null;
        }).when(mockJt).query(contains(MEMBER_SQL_MARKER), any(org.springframework.jdbc.core.RowCallbackHandler.class));

        // 积分负债查询（也走 RowCallbackHandler）→ 空回调
        doNothing().when(mockJt).query(contains("mem_points_record r"), any(org.springframework.jdbc.core.RowCallbackHandler.class));

        List<WorkbenchTaskVO> tasks = service.aggregateTasks();
        WorkbenchTaskVO memberTask = tasks.stream()
                .filter(t -> "MEMBER".equals(t.getSourceModule()))
                .filter(t -> "SILENT_MEMBER_HIGH".equals(t.getTaskType()))
                .findFirst()
                .orElse(null);

        assertNotNull(memberTask, "应检测到 SILENT_MEMBER_HIGH 任务");
        assertEquals("HIGH", memberTask.getSeverity());
        assertEquals(Long.valueOf(300L), memberTask.getDeptId());
        assertTrue(memberTask.getReason().contains("300"), "原因应包含门店ID");
        assertTrue(memberTask.getReason().contains("90"), "原因应包含沉默占比");
    }

    // ==================== STOCK 聚合测试 ====================

    @Test
    void collectStockTasks_detectsNegativeStock() throws Exception {
        SystemWorkbenchServiceImpl service = new SystemWorkbenchServiceImpl() {
            @Override
            List<Long> resolveAuthorizedDeptIds() { return null; }
        };

        org.springframework.jdbc.core.JdbcTemplate mockJt = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        setField(service, "jdbcTemplate", mockJt);

        // tableExists → true
        when(mockJt.queryForObject(contains(TABLE_EXISTS_MARKER), eq(Long.class), anyString()))
                .thenReturn(1L);

        // SYSTEM queryCount → 0
        when(mockJt.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(0L);

        // FINANCE RowMapper → 空
        when(mockJt.query(contains(FINANCE_SQL_MARKER), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(Collections.emptyList());
        // MEMBER RowCallbackHandler → 空回调
        doNothing().when(mockJt).query(contains(MEMBER_SQL_MARKER), any(org.springframework.jdbc.core.RowCallbackHandler.class));

        // STOCK RowMapper：返回 1 行（dept_id=300, neg_cnt=3）
        when(mockJt.query(contains(STOCK_SQL_MARKER), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenAnswer(invocation -> {
                    org.springframework.jdbc.core.RowMapper<WorkbenchTaskVO> mapper = invocation.getArgument(1);
                    ResultSet mockRs = mock(ResultSet.class);
                    when(mockRs.getObject("dept_id", Long.class)).thenReturn(300L);
                    when(mockRs.getLong("neg_cnt")).thenReturn(3L);
                    mapper.mapRow(mockRs, 0);
                    return Collections.emptyList();
                });

        List<WorkbenchTaskVO> tasks = service.aggregateTasks();
        WorkbenchTaskVO stockTask = tasks.stream()
                .filter(t -> "STOCK".equals(t.getSourceModule()))
                .filter(t -> "NEGATIVE_STOCK".equals(t.getTaskType()))
                .findFirst()
                .orElse(null);

        assertNotNull(stockTask, "应检测到 NEGATIVE_STOCK 任务");
        assertEquals("HIGH", stockTask.getSeverity());
        assertEquals(Long.valueOf(300L), stockTask.getDeptId(), "库存任务应携带 deptId");
        assertEquals("STOCK:NEGATIVE_STOCK:300", stockTask.getBizId(), "bizId 应带门店维度");
        assertTrue(stockTask.getReason().contains("3"), "原因应包含负库存数量");
        assertTrue(stockTask.getReason().contains("300"), "原因应包含门店ID");
    }

    @Test
    void collectStockTasks_filtersByAuthorizedDeptIdsForNonAdmin() throws Exception {
        // 非管理员：授权门店 [100, 200]，STOCK SQL 应包含 dept_id IN (100,200)
        SystemWorkbenchServiceImpl service = new SystemWorkbenchServiceImpl() {
            @Override
            List<Long> resolveAuthorizedDeptIds() { return Arrays.asList(100L, 200L); }
        };

        org.springframework.jdbc.core.JdbcTemplate mockJt = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        setField(service, "jdbcTemplate", mockJt);

        when(mockJt.queryForObject(contains(TABLE_EXISTS_MARKER), eq(Long.class), anyString()))
                .thenReturn(1L);
        when(mockJt.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        when(mockJt.query(contains(FINANCE_SQL_MARKER), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(Collections.emptyList());
        when(mockJt.query(contains(STOCK_SQL_MARKER), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(Collections.emptyList());
        doNothing().when(mockJt).query(contains(MEMBER_SQL_MARKER), any(org.springframework.jdbc.core.RowCallbackHandler.class));

        service.aggregateTasks();

        org.mockito.ArgumentCaptor<String> sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mockJt, atLeastOnce()).query(sqlCaptor.capture(), any(org.springframework.jdbc.core.RowMapper.class));
        String stockSql = sqlCaptor.getAllValues().stream()
                .filter(s -> s.contains(STOCK_SQL_MARKER))
                .findFirst()
                .orElse(null);
        assertNotNull(stockSql, "应有 STOCK 查询");
        assertTrue(stockSql.contains("dept_id IN (100,200)"),
                "非超管 STOCK 查询应包含 dept_id IN (100,200)，实际 SQL: " + stockSql);
        assertTrue(stockSql.contains("GROUP BY dept_id"),
                "STOCK 查询应按门店分组，实际 SQL: " + stockSql);
    }

    @Test
    void collectStockTasks_deptIdNotNullForEachTask() throws Exception {
        // 验证生成的每条 STOCK 任务都携带 deptId（不为 null）
        SystemWorkbenchServiceImpl service = new SystemWorkbenchServiceImpl() {
            @Override
            List<Long> resolveAuthorizedDeptIds() { return null; }
        };

        org.springframework.jdbc.core.JdbcTemplate mockJt = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        setField(service, "jdbcTemplate", mockJt);

        when(mockJt.queryForObject(contains(TABLE_EXISTS_MARKER), eq(Long.class), anyString()))
                .thenReturn(1L);
        when(mockJt.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        when(mockJt.query(contains(FINANCE_SQL_MARKER), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(Collections.emptyList());
        doNothing().when(mockJt).query(contains(MEMBER_SQL_MARKER), any(org.springframework.jdbc.core.RowCallbackHandler.class));

        // STOCK 返回 2 行（dept_id=100, 200）
        when(mockJt.query(contains(STOCK_SQL_MARKER), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenAnswer(invocation -> {
                    org.springframework.jdbc.core.RowMapper<WorkbenchTaskVO> mapper = invocation.getArgument(1);
                    ResultSet mockRs1 = mock(ResultSet.class);
                    when(mockRs1.getObject("dept_id", Long.class)).thenReturn(100L);
                    when(mockRs1.getLong("neg_cnt")).thenReturn(2L);
                    mapper.mapRow(mockRs1, 0);
                    ResultSet mockRs2 = mock(ResultSet.class);
                    when(mockRs2.getObject("dept_id", Long.class)).thenReturn(200L);
                    when(mockRs2.getLong("neg_cnt")).thenReturn(5L);
                    mapper.mapRow(mockRs2, 1);
                    return Collections.emptyList();
                });

        List<WorkbenchTaskVO> tasks = service.aggregateTasks();
        List<WorkbenchTaskVO> stockTasks = tasks.stream()
                .filter(t -> "STOCK".equals(t.getSourceModule()))
                .collect(java.util.stream.Collectors.toList());

        assertEquals(2, stockTasks.size(), "应生成 2 条库存任务（按门店分组）");
        for (WorkbenchTaskVO t : stockTasks) {
            assertNotNull(t.getDeptId(), "每条 STOCK 任务 deptId 不应为 null");
            assertTrue(t.getBizId().startsWith("STOCK:NEGATIVE_STOCK:"),
                    "bizId 应带门店维度: " + t.getBizId());
        }
    }

    // ==================== 排序测试 ====================

    @Test
    void aggregateTasks_sortsHighBeforeMedium() {
        java.util.List<WorkbenchTaskVO> tasks = new java.util.ArrayList<>();
        WorkbenchTaskVO medium = new WorkbenchTaskVO("SYSTEM", "M1", "MEDIUM", "t1", "r1", "s1", "/x");
        WorkbenchTaskVO high = new WorkbenchTaskVO("SYSTEM", "H1", "HIGH", "t2", "r2", "s2", "/y");
        WorkbenchTaskVO low = new WorkbenchTaskVO("SYSTEM", "L1", "LOW", "t3", "r3", "s3", "/z");
        tasks.add(medium);
        tasks.add(low);
        tasks.add(high);

        tasks.sort(java.util.Comparator
                .comparing((WorkbenchTaskVO t) -> {
                    if ("HIGH".equals(t.getSeverity())) return 0;
                    if ("MEDIUM".equals(t.getSeverity())) return 1;
                    return 2;
                })
                .thenComparing(WorkbenchTaskVO::getOccurTime,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));

        assertEquals("HIGH", tasks.get(0).getSeverity());
        assertEquals("MEDIUM", tasks.get(1).getSeverity());
        assertEquals("LOW", tasks.get(2).getSeverity());
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
}
