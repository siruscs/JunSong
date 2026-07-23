package com.junsong.system.service.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.junsong.system.domain.SysNotification;
import com.junsong.system.mapper.SysNotificationMapper;
import com.junsong.system.service.NotificationPushService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统通知已读/未读闭环测试
 *
 * 验证 SysNotificationServiceImpl 的通知列表返回 read/unread 状态、
 * 标记已读、批量已读、未读数查询、WebSocket 推送等行为。
 * 使用手写 fake 替代 Mockito，避免 JDK 26+ 兼容性问题。
 */
class SysNotificationServiceImplTest
{
    private SysNotificationServiceImpl service;
    private RecordingNotificationMapper mapper;
    private FakeNotificationPushService pushService;

    @BeforeEach
    void setUp() throws Exception
    {
        service = new SysNotificationServiceImpl();
        mapper = new RecordingNotificationMapper();
        pushService = new FakeNotificationPushService();
        setField(service, "notificationMapper", mapper);
        setField(service, "notificationPushService", pushService);
    }

    // ── Step 2：通知列表返回 read/unread 状态 ──

    @Test
    void listShouldReturnNotificationsWithReadStatus()
    {
        SysNotification unread = buildNotification(1L, 100L, "订单审核", "0");
        SysNotification read = buildNotification(2L, 100L, "注册审核通过", "1");
        mapper.notificationList = Arrays.asList(unread, read);

        SysNotification query = new SysNotification();
        query.setUserId(100L);
        List<SysNotification> result = service.selectNotificationList(query);

        assertEquals(2, result.size());
        assertEquals("0", result.get(0).getIsRead(), "第一条应为未读");
        assertEquals("1", result.get(1).getIsRead(), "第二条应为已读");
    }

    @Test
    void listShouldReturnEmptyWhenNoNotifications()
    {
        mapper.notificationList = Collections.emptyList();

        List<SysNotification> result = service.selectNotificationList(new SysNotification());

        assertTrue(result.isEmpty(), "无通知时应返回空列表");
    }

    // ── Step 3：标记单条已读后未读数减少 ──

    @Test
    void unreadCountShouldDelegateToMapper()
    {
        mapper.unreadCount = 5;
        assertEquals(5, service.selectUnreadCount(100L));
        assertEquals(100L, mapper.lastUnreadCountUserId);
    }

    @Test
    void markAsReadShouldDelegateToMapper()
    {
        mapper.markAsReadResult = 1;
        int rows = service.markAsRead(42L, 100L);

        assertEquals(1, rows);
        assertEquals(42L, mapper.lastMarkAsReadId);
        assertEquals(100L, mapper.lastMarkAsReadUserId);
    }

    @Test
    void markAllAsReadShouldDelegateToMapper()
    {
        mapper.markAllAsReadResult = 3;
        int rows = service.markAllAsRead(100L);

        assertEquals(3, rows, "应标记3条为已读");
        assertEquals(100L, mapper.lastMarkAllAsReadUserId);
    }

    // ── 插入 + WebSocket 推送 ──

    @Test
    void insertShouldPushToUserWhenSuccessful()
    {
        mapper.insertResult = 1;
        SysNotification n = buildNotification(null, 100L, "新订单", "0");

        int rows = service.insertNotification(n);

        assertEquals(1, rows);
        assertEquals(1, pushService.pushCount);
        assertEquals(100L, pushService.lastPushUserId);
        assertEquals("新订单", pushService.lastPushNotification.getTitle());
    }

    @Test
    void insertShouldNotPushWhenInsertFails()
    {
        mapper.insertResult = 0;
        SysNotification n = buildNotification(null, 100L, "失败通知", "0");

        int rows = service.insertNotification(n);

        assertEquals(0, rows);
        assertEquals(0, pushService.pushCount, "插入失败时不应推送");
    }

    @Test
    void insertShouldNotPushWhenUserIdIsNull()
    {
        mapper.insertResult = 1;
        SysNotification n = buildNotification(null, null, "无目标通知", "0");

        int rows = service.insertNotification(n);

        assertEquals(1, rows);
        assertEquals(0, pushService.pushCount, "userId 为 null 时不应推送");
    }

    @Test
    void batchInsertShouldPushForEachSuccessful()
    {
        mapper.insertResult = 1;
        List<SysNotification> list = Arrays.asList(
            buildNotification(null, 100L, "通知A", "0"),
            buildNotification(null, 200L, "通知B", "0"),
            buildNotification(null, null, "无目标", "0")
        );

        int count = service.batchInsertNotification(list);

        assertEquals(3, count);
        assertEquals(2, pushService.pushCount, "仅 userId 非空的2条应推送");
    }

    @Test
    void deleteShouldDelegateToMapper()
    {
        mapper.deleteResult = 2;
        Long[] ids = {1L, 2L};

        int rows = service.deleteNotificationByIds(ids);

        assertEquals(2, rows);
        assertArrayEquals(ids, mapper.lastDeleteIds);
    }

    // ── R7-C Task 5.1: dedup_key 幂等 ──

    @Test
    void insertNotification_ignoresDuplicateDedupKeyForSameUser()
    {
        mapper.insertResult = 1;
        // 第一次：同 user_id + dedupKey 不存在，应插入成功
        SysNotification first = buildNotificationWithDedup(
                100L, "工作台告警", "SYSTEM:LOGIN_FAIL:", "0");
        int rowsFirst = service.insertNotification(first);

        assertEquals(1, rowsFirst, "首次插入应返回 1");
        assertEquals(1, pushService.pushCount, "首次应推送");
        assertEquals(1, mapper.dedupKeysSeen.size(), "应记录 dedupKey");

        // 第二次：相同 user_id + dedupKey，应幂等跳过
        SysNotification second = buildNotificationWithDedup(
                100L, "工作台告警-重复", "SYSTEM:LOGIN_FAIL:", "0");
        int rowsSecond = service.insertNotification(second);

        assertEquals(0, rowsSecond, "重复 dedupKey 应返回 0（幂等跳过）");
        assertEquals(1, pushService.pushCount, "重复时不应再次推送");
    }

    @Test
    void insertNotification_allowsSameDedupKeyForDifferentUsers()
    {
        mapper.insertResult = 1;
        // 不同用户 + 相同 dedupKey，应都能插入
        SysNotification userA = buildNotificationWithDedup(
                100L, "告警A", "SYSTEM:LOGIN_FAIL:", "0");
        SysNotification userB = buildNotificationWithDedup(
                200L, "告警B", "SYSTEM:LOGIN_FAIL:", "0");

        assertEquals(1, service.insertNotification(userA));
        assertEquals(1, service.insertNotification(userB));
        assertEquals(2, pushService.pushCount, "两个不同用户都应推送");
    }

    @Test
    void insertNotification_skipsDedupCheckWhenDedupKeyBlank()
    {
        mapper.insertResult = 1;
        // dedupKey 为空时，走原有逻辑，不触发去重查询
        SysNotification n = buildNotification(null, 100L, "普通通知", "0");
        n.setDedupKey("");

        int rows = service.insertNotification(n);

        assertEquals(1, rows);
        assertEquals(0, mapper.countByUserDedupKeyCalls, "空 dedupKey 不应查询去重");
    }

    // ── 辅助方法 ──

    private static SysNotification buildNotification(Long id, Long userId, String title, String isRead)
    {
        SysNotification n = new SysNotification();
        n.setId(id);
        n.setUserId(userId);
        n.setTitle(title);
        n.setIsRead(isRead);
        return n;
    }

    private static SysNotification buildNotificationWithDedup(Long userId, String title, String dedupKey, String isRead)
    {
        SysNotification n = buildNotification(null, userId, title, isRead);
        n.setDedupKey(dedupKey);
        return n;
    }

    private static void setField(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ── Fake 实现 ──

    /**
     * 录制型 Notification Mapper：记录调用参数，返回可配置结果。
     */
    static class RecordingNotificationMapper implements SysNotificationMapper
    {
        List<SysNotification> notificationList = new ArrayList<>();
        int unreadCount = 0;
        Long lastUnreadCountUserId = null;
        int markAsReadResult = 1;
        Long lastMarkAsReadId = null;
        Long lastMarkAsReadUserId = null;
        int markAllAsReadResult = 0;
        Long lastMarkAllAsReadUserId = null;
        int insertResult = 1;
        int deleteResult = 0;
        Long[] lastDeleteIds = null;
        /** 已记录的 dedup_key（key=userId:dedupKey），模拟数据库唯一约束 */
        final Set<String> dedupKeysSeen = new HashSet<>();
        int countByUserDedupKeyCalls = 0;

        @Override
        public SysNotification selectNotificationById(Long id)
        {
            return notificationList.stream()
                .filter(n -> id.equals(n.getId()))
                .findFirst().orElse(null);
        }

        @Override
        public List<SysNotification> selectNotificationList(SysNotification notification)
        {
            return notificationList;
        }

        @Override
        public int selectUnreadCount(Long userId)
        {
            lastUnreadCountUserId = userId;
            return unreadCount;
        }

        @Override
        public int insertNotification(SysNotification notification)
        {
            // 成功插入时记录 dedup_key，使后续 countByUserDedupKey 返回 1
            if (notification.getDedupKey() != null && !notification.getDedupKey().isEmpty()
                    && notification.getUserId() != null)
            {
                dedupKeysSeen.add(notification.getUserId() + ":" + notification.getDedupKey());
            }
            return insertResult;
        }

        @Override
        public int markAsRead(Long id, Long userId)
        {
            lastMarkAsReadId = id;
            lastMarkAsReadUserId = userId;
            return markAsReadResult;
        }

        @Override
        public int markAllAsRead(Long userId)
        {
            lastMarkAllAsReadUserId = userId;
            return markAllAsReadResult;
        }

        @Override
        public int deleteNotificationByIds(Long[] ids)
        {
            lastDeleteIds = ids;
            return deleteResult;
        }

        @Override
        public int countByUserTypeBizId(Long userId, String type, String bizId)
        {
            return 0;
        }

        @Override
        public int countByUserDedupKey(Long userId, String dedupKey)
        {
            countByUserDedupKeyCalls++;
            return dedupKeysSeen.contains(userId + ":" + dedupKey) ? 1 : 0;
        }
    }

    /**
     * 简化版 NotificationPushService：记录推送调用次数和参数。
     */
    static class FakeNotificationPushService extends NotificationPushService
    {
        int pushCount = 0;
        Long lastPushUserId = null;
        SysNotification lastPushNotification = null;
        int broadcastCount = 0;

        @Override
        public void pushToUser(Long userId, SysNotification notification)
        {
            pushCount++;
            lastPushUserId = userId;
            lastPushNotification = notification;
        }

        @Override
        public void broadcast(SysNotification notification)
        {
            broadcastCount++;
        }
    }
}
