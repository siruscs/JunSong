package com.junsong.system.service.impl;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.system.domain.SysOperatingTask;
import com.junsong.system.mapper.SysOperatingTaskLogMapper;
import com.junsong.system.mapper.SysOperatingTaskMapper;
import com.junsong.system.service.AuthorizedDeptResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SysOperatingTaskServiceImpl 单元测试。
 *
 * JUnit5 + Mockito，无 Spring Context，用反射注入字段。
 * 参考 SystemWorkbenchServiceImplTest 风格：
 * - 通过匿名子类覆盖 currentUserId()/currentUsername() 绕过 SecurityUtils 静态方法
 * - mock Mapper 和 AuthorizedDeptResolver
 *
 * @author junsong
 */
class SysOperatingTaskServiceImplTest {

    private SysOperatingTaskMapper operatingTaskMapper;
    private SysOperatingTaskLogMapper operatingTaskLogMapper;
    private AuthorizedDeptResolver authorizedDeptResolver;

    @BeforeEach
    void setUp() throws Exception {
        operatingTaskMapper = mock(SysOperatingTaskMapper.class);
        operatingTaskLogMapper = mock(SysOperatingTaskLogMapper.class);
        authorizedDeptResolver = mock(AuthorizedDeptResolver.class);
    }

    /**
     * 创建带 mock 依赖和固定用户身份的 Service 实例。
     */
    private SysOperatingTaskServiceImpl createService(Long userId, String username) {
        SysOperatingTaskServiceImpl service = new SysOperatingTaskServiceImpl() {
            @Override
            Long currentUserId() { return userId; }

            @Override
            String currentUsername() { return username; }
        };
        try {
            setField(service, "operatingTaskMapper", operatingTaskMapper);
            setField(service, "operatingTaskLogMapper", operatingTaskLogMapper);
            setField(service, "authorizedDeptResolver", authorizedDeptResolver);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return service;
    }

    /** 创建 admin（授权门店=null 不过滤）的 Service */
    private SysOperatingTaskServiceImpl createAdminService() {
        when(authorizedDeptResolver.resolveAuthorizedDeptIds()).thenReturn(null);
        when(authorizedDeptResolver.canAccessDept(anyLong(), isNull())).thenReturn(true);
        return createService(1L, "admin");
    }

    // ==================== 测试用例 ====================

    // 1. 重复创建返回已存在任务，不抛错
    @Test
    void createOrUpdateTask_idempotentDuplicateReturnsExisting() {
        SysOperatingTaskServiceImpl service = createAdminService();

        SysOperatingTask existing = new SysOperatingTask();
        existing.setTaskId(999L);
        existing.setIdempotencyKey("1:FINANCE:REVIEW_TASK:123");
        existing.setStatus("PENDING");
        when(operatingTaskMapper.selectByIdempotencyKey(anyLong(), anyString())).thenReturn(existing);

        SysOperatingTask newTask = new SysOperatingTask();
        newTask.setSourceModule("FINANCE");
        newTask.setSourceType("REVIEW_TASK");
        newTask.setSourceId("123");
        newTask.setIdempotencyKey("1:FINANCE:REVIEW_TASK:123");
        newTask.setTaskType("SALES_DROP");
        newTask.setDeptId(100L);
        newTask.setTitle("测试任务");

        SysOperatingTask result = service.createOrUpdateTask(newTask);

        assertNotNull(result);
        assertEquals(999L, result.getTaskId());
        verify(operatingTaskMapper, never()).insertOperatingTask(any());
    }

    // 2. PENDING -> IN_PROGRESS 认领成功
    @Test
    void claimTask_conditionalUpdateSuccess() {
        SysOperatingTaskServiceImpl service = createService(10L, "handler1");
        when(authorizedDeptResolver.resolveAuthorizedDeptIds()).thenReturn(null);
        when(authorizedDeptResolver.canAccessDept(anyLong(), isNull())).thenReturn(true);

        SysOperatingTask task = buildTask(1L, "PENDING", 0, 100L);
        when(operatingTaskMapper.selectOperatingTaskById(1L)).thenReturn(task);
        when(operatingTaskMapper.conditionalUpdateStatus(
                eq(1L), eq("PENDING"), eq(0), eq("IN_PROGRESS"),
                eq(10L), eq("handler1"), isNull(), isNull(), isNull()))
                .thenReturn(1);

        int affected = service.claimTask(1L);

        assertEquals(1, affected);
        verify(operatingTaskLogMapper).insertTaskLog(any());
    }

    // 3. 非 PENDING 状态认领抛 ServiceException
    @Test
    void claimTask_failsWhenNotPending() {
        SysOperatingTaskServiceImpl service = createAdminService();

        SysOperatingTask task = buildTask(1L, "IN_PROGRESS", 0, 100L);
        when(operatingTaskMapper.selectOperatingTaskById(1L)).thenReturn(task);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.claimTask(1L));
        assertTrue(ex.getMessage().contains("无法认领"));
        verify(operatingTaskMapper, never()).conditionalUpdateStatus(
                anyLong(), anyString(), anyInt(), anyString(),
                any(), any(), any(), any(), any());
    }

    // 4. 条件更新返回0行时抛"任务已被他人认领"
    @Test
    void claimTask_failsOnConcurrentClaim() {
        SysOperatingTaskServiceImpl service = createService(10L, "handler1");
        when(authorizedDeptResolver.resolveAuthorizedDeptIds()).thenReturn(null);
        when(authorizedDeptResolver.canAccessDept(anyLong(), isNull())).thenReturn(true);

        SysOperatingTask task = buildTask(1L, "PENDING", 0, 100L);
        when(operatingTaskMapper.selectOperatingTaskById(1L)).thenReturn(task);
        when(operatingTaskMapper.conditionalUpdateStatus(
                anyLong(), anyString(), anyInt(), anyString(),
                any(), any(), any(), any(), any()))
                .thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.claimTask(1L));
        assertTrue(ex.getMessage().contains("已被他人认领"));
    }

    // 5. handlerNote 为空抛 ServiceException
    @Test
    void completeTask_requiresHandlerNote() {
        SysOperatingTaskServiceImpl service = createAdminService();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.completeTask(1L, null));
        assertTrue(ex.getMessage().contains("处理备注不能为空"));

        ServiceException ex2 = assertThrows(ServiceException.class,
                () -> service.completeTask(1L, "   "));
        assertTrue(ex2.getMessage().contains("处理备注不能为空"));
    }

    // 6. 非 IN_PROGRESS/REOPENED 完成抛错
    @Test
    void completeTask_failsWhenNotInProgress() {
        SysOperatingTaskServiceImpl service = createAdminService();

        SysOperatingTask task = buildTask(1L, "PENDING", 0, 100L);
        when(operatingTaskMapper.selectOperatingTaskById(1L)).thenReturn(task);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.completeTask(1L, "处理完成"));
        assertTrue(ex.getMessage().contains("无法完成"));
    }

    // 7. rejectReason 为空抛错
    @Test
    void rejectTask_requiresRejectReason() {
        SysOperatingTaskServiceImpl service = createAdminService();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.rejectTask(1L, null));
        assertTrue(ex.getMessage().contains("驳回原因不能为空"));

        ServiceException ex2 = assertThrows(ServiceException.class,
                () -> service.rejectTask(1L, ""));
        assertTrue(ex2.getMessage().contains("驳回原因不能为空"));
    }

    // 8. 非 DONE/REJECTED 重开抛错
    @Test
    void reopenTask_failsWhenNotDoneOrRejected() {
        SysOperatingTaskServiceImpl service = createAdminService();

        SysOperatingTask task = buildTask(1L, "IN_PROGRESS", 0, 100L);
        when(operatingTaskMapper.selectOperatingTaskById(1L)).thenReturn(task);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.reopenTask(1L, "需要重新处理"));
        assertTrue(ex.getMessage().contains("无法重开"));
    }

    // 9. 重开 reopen_count+1
    @Test
    void reopenTask_incrementsReopenCount() {
        SysOperatingTaskServiceImpl service = createAdminService();

        SysOperatingTask task = buildTask(1L, "DONE", 2, 100L);
        task.setReopenCount(3);
        when(operatingTaskMapper.selectOperatingTaskById(1L)).thenReturn(task);
        when(operatingTaskMapper.conditionalUpdateStatus(
                eq(1L), eq("DONE"), eq(2), eq("IN_PROGRESS"),
                isNull(), isNull(), isNull(), isNull(), eq(4)))
                .thenReturn(1);

        int affected = service.reopenTask(1L, "需要补充处理");

        assertEquals(1, affected);
        // 验证 reopenCount 传入了 4（3+1）
        verify(operatingTaskMapper).conditionalUpdateStatus(
                eq(1L), eq("DONE"), eq(2), eq("IN_PROGRESS"),
                isNull(), isNull(), isNull(), isNull(), eq(4));
        verify(operatingTaskLogMapper).insertTaskLog(any());
    }

    // 10. 非授权门店任务抛"无权操作"
    @Test
    void canAccessTask_failsForUnauthorizedDept() {
        SysOperatingTaskServiceImpl service = createService(10L, "user1");
        // 非超管，授权门店=[100L, 200L]
        List<Long> authorizedDepts = Arrays.asList(100L, 200L);
        when(authorizedDeptResolver.resolveAuthorizedDeptIds()).thenReturn(authorizedDepts);
        when(authorizedDeptResolver.canAccessDept(eq(300L), eq(authorizedDepts))).thenReturn(false);

        SysOperatingTask task = buildTask(1L, "PENDING", 0, 300L);
        when(operatingTaskMapper.selectOperatingTaskById(1L)).thenReturn(task);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.claimTask(1L));
        assertTrue(ex.getMessage().contains("无权操作"));
    }

    // 11. 非 admin 列表带 deptIds 过滤
    @Test
    @SuppressWarnings("unchecked")
    void selectOperatingTaskList_filtersByAuthorizedDepts() {
        SysOperatingTaskServiceImpl service = createService(10L, "user1");
        List<Long> authorizedDepts = Arrays.asList(100L, 200L);
        when(authorizedDeptResolver.resolveAuthorizedDeptIds()).thenReturn(authorizedDepts);
        when(operatingTaskMapper.selectOperatingTaskList(anyMap())).thenReturn(Collections.emptyList());

        Map<String, Object> params = new HashMap<>();
        params.put("status", "PENDING");
        service.selectOperatingTaskList(params);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(operatingTaskMapper).selectOperatingTaskList(captor.capture());
        Map<String, Object> captured = captor.getValue();
        assertEquals(authorizedDepts, captured.get("deptIds"));
        assertEquals("PENDING", captured.get("status"));
    }

    // 12. 幂等键含 tenantId 前缀
    @Test
    void createOrUpdateTask_buildsIdempotencyKeyWithTenant() {
        SysOperatingTaskServiceImpl service = createAdminService();

        // selectByIdempotencyKey 返回 null（不存在）
        when(operatingTaskMapper.selectByIdempotencyKey(anyLong(), anyString())).thenReturn(null);
        when(operatingTaskMapper.insertOperatingTask(any())).thenReturn(1);

        SysOperatingTask newTask = new SysOperatingTask();
        newTask.setSourceModule("FINANCE");
        newTask.setSourceType("RECEIVABLE_COLLECTION");
        newTask.setSourceId("1024");
        newTask.setTaskType("OVERDUE_RECEIVABLE");
        newTask.setDeptId(100L);
        newTask.setTitle("逾期应收任务");
        // 不设置 idempotencyKey，让 Service 自动构建

        SysOperatingTask result = service.createOrUpdateTask(newTask);

        assertNotNull(result.getIdempotencyKey());
        // tenantId 前缀（TenantContext 默认返回 1）
        assertTrue(result.getIdempotencyKey().startsWith("1:"), 
                "幂等键应以 tenantId 前缀开头，实际: " + result.getIdempotencyKey());
        assertTrue(result.getIdempotencyKey().contains("FINANCE"));
        assertTrue(result.getIdempotencyKey().contains("RECEIVABLE_COLLECTION"));
        assertTrue(result.getIdempotencyKey().contains("1024"));
        // 验证格式：{tenantId}:{sourceModule}:{sourceType}:{sourceId}
        assertEquals("1:FINANCE:RECEIVABLE_COLLECTION:1024", result.getIdempotencyKey());
    }

    // ==================== 辅助方法 ====================

    private SysOperatingTask buildTask(Long taskId, String status, Integer version, Long deptId) {
        SysOperatingTask task = new SysOperatingTask();
        task.setTaskId(taskId);
        task.setStatus(status);
        task.setVersion(version);
        task.setDeptId(deptId);
        task.setReopenCount(0);
        task.setTitle("测试任务");
        task.setDelFlag("0");
        return task;
    }

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
