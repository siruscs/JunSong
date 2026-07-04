package com.junsong.system.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.junsong.system.domain.SysNotification;
import com.junsong.system.domain.vo.WorkbenchTaskVO;
import com.junsong.system.service.ISysNotificationService;
import com.junsong.system.service.ISystemWorkbenchService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SystemWorkbenchNotifierServiceImpl 单元测试（R7-C）。
 *
 * 使用子类覆写接收者查询方法 + 手写 fake 服务，避免依赖 Spring context / JdbcTemplate。
 */
class SystemWorkbenchNotifierServiceImplTest
{
    private FakeWorkbenchService workbenchService;
    private RecordingNotificationService notificationService;
    private TestableNotifierService notifier;

    @BeforeEach
    void setUp()
    {
        workbenchService = new FakeWorkbenchService();
        notificationService = new RecordingNotificationService();
        notifier = new TestableNotifierService();
        notifier.setSystemWorkbenchService(workbenchService);
        notifier.setNotificationService(notificationService);
    }

    // ── highTasksGenerateNotifications ──

    @Test
    void highTasksGenerateNotifications()
    {
        // 两个 HIGH 任务：SYSTEM（超管1,2）+ STOCK（权限用户3，deptId=100）
        WorkbenchTaskVO stockTask = buildTask("STOCK", "NEGATIVE_STOCK", "HIGH", "负库存", "STOCK:NEGATIVE_STOCK:100");
        stockTask.setDeptId(100L);
        workbenchService.tasks = Arrays.asList(
                buildTask("SYSTEM", "LOGIN_FAIL", "HIGH", "登录失败", "biz-sys"),
                stockTask,
                buildTask("MEMBER", "SILENT_MEMBER_HIGH", "MEDIUM", "沉默会员", "biz-member")
        );
        notifier.systemUsers = setOf(1L, 2L);
        notifier.permUsers = setOf(3L);

        int sent = notifier.notifyHighPriorityTasks();

        // SYSTEM 告警发给超管1,2（2条）+ STOCK 告警发给权限用户3（1条）= 3条
        // MEMBER 是 MEDIUM 被跳过
        assertEquals(3, sent, "仅 HIGH 任务应发送，共3条");
        assertEquals(3, notificationService.inserted.size(), "应插入3条通知");
        // 验证 dedupKey 格式
        SysNotification sysNotif = notificationService.inserted.stream()
                .filter(n -> n.getUserId().equals(1L)).findFirst().orElse(null);
        assertNotNull(sysNotif);
        assertEquals("SYSTEM:LOGIN_FAIL:biz-sys", sysNotif.getDedupKey());
        assertEquals("workbench", sysNotif.getType());
        assertEquals("登录失败", sysNotif.getTitle());
    }

    // ── duplicateDedupKeyDoesNotDuplicate ──

    @Test
    void duplicateDedupKeyDoesNotDuplicate()
    {
        // 同一 HIGH 任务调用两次，第二次应全部幂等跳过
        WorkbenchTaskVO highTask = buildTask("SYSTEM", "LOGIN_FAIL", "HIGH", "登录失败", "biz-sys");
        workbenchService.tasks = Collections.singletonList(highTask);
        notifier.systemUsers = setOf(1L);

        int firstRun = notifier.notifyHighPriorityTasks();
        int secondRun = notifier.notifyHighPriorityTasks();

        assertEquals(1, firstRun, "首次应发送1条");
        assertEquals(0, secondRun, "第二次相同 dedupKey 应全部幂等跳过");
        assertEquals(1, notificationService.inserted.size(), "仅1条记录入库");
    }

    // ── nonHighTasksAreIgnored ──

    @Test
    void nonHighTasksAreIgnored()
    {
        workbenchService.tasks = Arrays.asList(
                buildTask("FINANCE", "REVIEW_TASK", "MEDIUM", "财务复盘", "biz-fin"),
                buildTask("MEMBER", "SILENT_MEMBER_HIGH", "LOW", "沉默会员", "biz-mem"),
                buildTask("SYSTEM", "MENU_WITHOUT_ROLE", "MEDIUM", "未授权菜单", "biz-sys2")
        );
        notifier.systemUsers = setOf(1L);
        notifier.permUsers = setOf(2L, 3L);

        int sent = notifier.notifyHighPriorityTasks();

        assertEquals(0, sent, "无 HIGH 任务时应发送0条");
        assertEquals(0, notificationService.inserted.size(), "无通知入库");
    }

    // ── noAuthorizedUsersSkipsSilently ──

    @Test
    void noAuthorizedUsersSkipsSilently()
    {
        // HIGH 任务但无授权用户，应静默跳过，不抛异常
        workbenchService.tasks = Collections.singletonList(
                buildTask("SYSTEM", "LOGIN_FAIL", "HIGH", "登录失败", "biz-sys"));
        notifier.systemUsers = Collections.emptySet(); // 无授权用户

        int sent = notifier.notifyHighPriorityTasks();

        assertEquals(0, sent, "无授权用户应返回0");
        assertEquals(0, notificationService.inserted.size(), "不应插入任何通知");
    }

    @Test
    void emptyTasksReturnsZero()
    {
        workbenchService.tasks = Collections.emptyList();

        assertEquals(0, notifier.notifyHighPriorityTasks());
    }

    @Test
    void financeHighTaskSendsToBothPermGroups()
    {
        // FINANCE 来源：拥有 finance:dashboard:alerts 或 finance:reviewTask:list 的用户都应收到
        WorkbenchTaskVO task = buildTask("FINANCE", "REVIEW_TASK", "HIGH", "财务复盘告警", "FINANCE:1");
        task.setDeptId(200L);
        workbenchService.tasks = Collections.singletonList(task);
        notifier.permUsers = setOf(10L, 20L);

        int sent = notifier.notifyHighPriorityTasks();

        assertEquals(2, sent, "FINANCE HIGH 应发给2个授权用户");
        // 验证调用了 FINANCE 相关权限
        assertTrue(notifier.requestedPerms.contains("finance:dashboard:alerts"));
        assertTrue(notifier.requestedPerms.contains("finance:reviewTask:list"));
        // R7 回修：验证 taskDeptId 被传递给接收人查询
        assertTrue(notifier.requestedDeptIds.contains(200L),
                "FINANCE 任务 deptId=200 应传递给 findUserIdsByPermissionsAndDept");
    }

    @Test
    void systemTaskDeptIdIsNull()
    {
        // SYSTEM 来源任务无 deptId，resolveReceivers 传 null
        workbenchService.tasks = Collections.singletonList(
                buildTask("SYSTEM", "LOGIN_FAIL", "HIGH", "登录失败", "biz-sys"));
        notifier.systemUsers = setOf(1L);

        notifier.notifyHighPriorityTasks();

        // SYSTEM 走 findSuperAdminUserIds，不走 findUserIdsByPermissionsAndDept
        assertTrue(notifier.requestedDeptIds.isEmpty(),
                "SYSTEM 任务不走权限+门店查询，requestedDeptIds 应为空");
    }

    @Test
    void stockHighTaskPassesDeptIdToReceiverQuery()
    {
        // R7 回修补强：STOCK 任务必须携带 deptId，触发 sys_user_dept 过滤
        // bizId 带门店维度避免不同门店通知互相去重
        WorkbenchTaskVO task = buildTask("STOCK", "NEGATIVE_STOCK", "HIGH", "负库存告警", "STOCK:NEGATIVE_STOCK:300");
        task.setDeptId(300L);
        workbenchService.tasks = Collections.singletonList(task);
        notifier.permUsers = setOf(10L, 20L);

        int sent = notifier.notifyHighPriorityTasks();

        assertEquals(2, sent, "STOCK HIGH 应发给2个授权用户");
        assertTrue(notifier.requestedPerms.contains("finance:stock:health"),
                "应查询 finance:stock:health 权限");
        assertTrue(notifier.requestedDeptIds.contains(300L),
                "STOCK 任务 deptId=300 应传递给 findUserIdsByPermissionsAndDept，触发 sys_user_dept 过滤");
    }

    @Test
    void stockTasksWithDifferentDeptIdsAreNotDeduped()
    {
        // R7 回修补强：不同门店的 STOCK 任务 bizId 不同，不应互相去重
        WorkbenchTaskVO task1 = buildTask("STOCK", "NEGATIVE_STOCK", "HIGH", "负库存-门店100", "STOCK:NEGATIVE_STOCK:100");
        task1.setDeptId(100L);
        WorkbenchTaskVO task2 = buildTask("STOCK", "NEGATIVE_STOCK", "HIGH", "负库存-门店200", "STOCK:NEGATIVE_STOCK:200");
        task2.setDeptId(200L);
        workbenchService.tasks = Arrays.asList(task1, task2);
        notifier.permUsers = setOf(10L);

        int sent = notifier.notifyHighPriorityTasks();

        assertEquals(2, sent, "两个不同门店的 STOCK 任务应各发1条，不应互相去重");
    }

    // ── 辅助方法 ──

    private static WorkbenchTaskVO buildTask(String sourceModule, String taskType, String severity,
                                             String title, String bizId)
    {
        WorkbenchTaskVO vo = new WorkbenchTaskVO(sourceModule, taskType, severity,
                title, "原因详情", "处理建议", "/workbench");
        vo.setBizId(bizId);
        return vo;
    }

    private static Set<Long> setOf(Long... ids)
    {
        return new HashSet<>(Arrays.asList(ids));
    }

    // ── 测试替身 ──

    /**
     * 可覆写接收者查询的 NotifierService 子类。
     * R7 回修：方法名改为 findUserIdsByPermissionsAndDept（增加 taskDeptId 参数）。
     */
    static class TestableNotifierService extends SystemWorkbenchNotifierServiceImpl
    {
        Set<Long> systemUsers = Collections.emptySet();
        Set<Long> permUsers = Collections.emptySet();
        final List<String> requestedPerms = new ArrayList<>();
        final List<Long> requestedDeptIds = new ArrayList<>();

        @Override
        protected Set<Long> findUserIdsByPermissionsAndDept(Long taskDeptId, String... perms)
        {
            requestedPerms.addAll(Arrays.asList(perms));
            requestedDeptIds.add(taskDeptId);
            return permUsers;
        }

        @Override
        protected Set<Long> findSuperAdminUserIds()
        {
            return systemUsers;
        }
    }

    /**
     * 假工作台服务：返回可配置的任务列表。
     */
    static class FakeWorkbenchService implements ISystemWorkbenchService
    {
        List<WorkbenchTaskVO> tasks = Collections.emptyList();

        @Override
        public List<WorkbenchTaskVO> aggregateTasks()
        {
            return tasks;
        }
    }

    /**
     * 录制型通知服务：记录插入并模拟 dedup_key 幂等。
     */
    static class RecordingNotificationService implements ISysNotificationService
    {
        final List<SysNotification> inserted = new ArrayList<>();
        final Set<String> dedupKeys = new HashSet<>();

        @Override
        public int insertNotification(SysNotification notification)
        {
            String dedupKey = notification.getDedupKey();
            if (dedupKey != null && !dedupKey.isEmpty() && notification.getUserId() != null)
            {
                String key = notification.getUserId() + ":" + dedupKey;
                if (dedupKeys.contains(key))
                {
                    return 0; // 幂等跳过
                }
                dedupKeys.add(key);
            }
            inserted.add(notification);
            return 1;
        }

        @Override
        public List<SysNotification> selectNotificationList(SysNotification notification)
        {
            return inserted;
        }

        @Override
        public int selectUnreadCount(Long userId)
        {
            return 0;
        }

        @Override
        public int batchInsertNotification(List<SysNotification> notifications)
        {
            return 0;
        }

        @Override
        public int markAsRead(Long id)
        {
            return 0;
        }

        @Override
        public int markAllAsRead(Long userId)
        {
            return 0;
        }

        @Override
        public int deleteNotificationByIds(Long[] ids)
        {
            return 0;
        }

        @Override
        public int countByUserTypeBizId(Long userId, String type, String bizId)
        {
            return 0;
        }
    }
}
