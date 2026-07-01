package com.junsong.system.service.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.junsong.system.domain.SysNotice;
import com.junsong.system.domain.SysNoticeRead;
import com.junsong.system.mapper.SysNoticeReadMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 公告已读/未读闭环测试
 *
 * 验证 SysNoticeReadServiceImpl 的标记已读、未读计数、
 * 批量标记、已读用户查询、级联清理等行为。
 * 使用手写 fake 替代 Mockito，避免 JDK 26+ 兼容性问题。
 */
class SysNoticeReadServiceImplTest
{
    private SysNoticeReadServiceImpl service;
    private RecordingNoticeReadMapper mapper;

    @BeforeEach
    void setUp() throws Exception
    {
        service = new SysNoticeReadServiceImpl();
        mapper = new RecordingNoticeReadMapper();
        setField(service, "noticeReadMapper", mapper);
    }

    // ── 标记已读 ──

    @Test
    void markReadShouldInsertNoticeReadRecord()
    {
        service.markRead(10L, 100L);

        assertEquals(1, mapper.insertCount);
        assertEquals(10L, mapper.lastInsertedRecord.getNoticeId());
        assertEquals(100L, mapper.lastInsertedRecord.getUserId());
    }

    @Test
    void markReadMultipleTimesShouldInsertEachTime()
    {
        service.markRead(10L, 100L);
        service.markRead(20L, 100L);

        assertEquals(2, mapper.insertCount, "每次标记都应调用 insert（SQL 层忽略重复）");
    }

    // ── 未读计数 ──

    @Test
    void selectUnreadCountShouldDelegateToMapper()
    {
        mapper.unreadCount = 7;
        assertEquals(7, service.selectUnreadCount(100L));
        assertEquals(100L, mapper.lastUnreadCountUserId);
    }

    @Test
    void selectUnreadCountShouldReturnZeroWhenAllRead()
    {
        mapper.unreadCount = 0;
        assertEquals(0, service.selectUnreadCount(100L));
    }

    // ── 公告列表带已读状态 ──

    @Test
    void selectNoticeListWithReadStatusShouldReturnMixedStatus()
    {
        SysNotice readNotice = buildNotice(1L, "系统升级通知", true);
        SysNotice unreadNotice = buildNotice(2L, "新功能上线", false);
        mapper.noticeListWithReadStatus = Arrays.asList(readNotice, unreadNotice);

        List<SysNotice> result = service.selectNoticeListWithReadStatus(100L, 10);

        assertEquals(2, result.size());
        assertTrue(result.get(0).getIsRead(), "第一条应为已读");
        assertFalse(result.get(1).getIsRead(), "第二条应为未读");
        assertEquals(100L, mapper.lastNoticeListUserId);
        assertEquals(10, mapper.lastNoticeListLimit);
    }

    @Test
    void selectNoticeListWithReadStatusShouldReturnEmptyWhenNoNotices()
    {
        mapper.noticeListWithReadStatus = Collections.emptyList();

        List<SysNotice> result = service.selectNoticeListWithReadStatus(100L, 5);

        assertTrue(result.isEmpty());
    }

    // ── 批量标记已读 ──

    @Test
    void markReadBatchShouldCallMapperWithCorrectParams()
    {
        Long[] noticeIds = {1L, 2L, 3L};
        service.markReadBatch(100L, noticeIds);

        assertEquals(1, mapper.batchInsertCount);
        assertEquals(100L, mapper.lastBatchUserId);
        assertArrayEquals(noticeIds, mapper.lastBatchNoticeIds);
    }

    @Test
    void markReadBatchShouldSkipWhenNull()
    {
        service.markReadBatch(100L, null);
        assertEquals(0, mapper.batchInsertCount, "null 数组不应调用 mapper");
    }

    @Test
    void markReadBatchShouldSkipWhenEmpty()
    {
        service.markReadBatch(100L, new Long[0]);
        assertEquals(0, mapper.batchInsertCount, "空数组不应调用 mapper");
    }

    // ── 已读用户查询 ──

    @Test
    void selectReadUsersByNoticeIdShouldReturnUserList()
    {
        Map<String, Object> user1 = new HashMap<>();
        user1.put("userId", 100L);
        user1.put("userName", "admin");
        Map<String, Object> user2 = new HashMap<>();
        user2.put("userId", 200L);
        user2.put("userName", "operator");
        mapper.readUsers = Arrays.asList(user1, user2);

        List<Map<String, Object>> result = service.selectReadUsersByNoticeId(10L, null);

        assertEquals(2, result.size());
        assertEquals("admin", result.get(0).get("userName"));
        assertEquals(10L, mapper.lastReadUsersNoticeId);
        assertNull(mapper.lastReadUsersSearchValue);
    }

    @Test
    void selectReadUsersByNoticeIdShouldPassSearchValue()
    {
        mapper.readUsers = Collections.emptyList();

        service.selectReadUsersByNoticeId(10L, "admin");

        assertEquals("admin", mapper.lastReadUsersSearchValue);
    }

    // ── 级联清理 ──

    @Test
    void deleteByNoticeIdsShouldDelegateToMapper()
    {
        Long[] noticeIds = {5L, 6L, 7L};
        service.deleteByNoticeIds(noticeIds);

        assertEquals(1, mapper.deleteCount);
        assertArrayEquals(noticeIds, mapper.lastDeleteNoticeIds);
    }

    // ── 辅助方法 ──

    private static SysNotice buildNotice(Long noticeId, String title, boolean isRead)
    {
        SysNotice notice = new SysNotice();
        notice.setNoticeId(noticeId);
        notice.setNoticeTitle(title);
        notice.setIsRead(isRead);
        return notice;
    }

    private static void setField(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ── Fake 实现 ──

    /**
     * 录制型 NoticeRead Mapper：记录调用参数，返回可配置结果。
     */
    static class RecordingNoticeReadMapper implements SysNoticeReadMapper
    {
        int insertCount = 0;
        SysNoticeRead lastInsertedRecord = null;
        int unreadCount = 0;
        Long lastUnreadCountUserId = null;
        int isReadResult = 0;
        int batchInsertCount = 0;
        Long lastBatchUserId = null;
        Long[] lastBatchNoticeIds = null;
        List<SysNotice> noticeListWithReadStatus = new ArrayList<>();
        Long lastNoticeListUserId = null;
        int lastNoticeListLimit = 0;
        List<Map<String, Object>> readUsers = new ArrayList<>();
        Long lastReadUsersNoticeId = null;
        String lastReadUsersSearchValue = null;
        int deleteCount = 0;
        Long[] lastDeleteNoticeIds = null;

        @Override
        public int insertNoticeRead(SysNoticeRead noticeRead)
        {
            insertCount++;
            lastInsertedRecord = noticeRead;
            return 1;
        }

        @Override
        public int selectUnreadCount(Long userId)
        {
            lastUnreadCountUserId = userId;
            return unreadCount;
        }

        @Override
        public int selectIsRead(Long noticeId, Long userId)
        {
            return isReadResult;
        }

        @Override
        public int insertNoticeReadBatch(Long userId, Long[] noticeIds)
        {
            batchInsertCount++;
            lastBatchUserId = userId;
            lastBatchNoticeIds = noticeIds;
            return noticeIds.length;
        }

        @Override
        public List<SysNotice> selectNoticeListWithReadStatus(Long userId, int limit)
        {
            lastNoticeListUserId = userId;
            lastNoticeListLimit = limit;
            return noticeListWithReadStatus;
        }

        @Override
        public List<Map<String, Object>> selectReadUsersByNoticeId(Long noticeId, String searchValue)
        {
            lastReadUsersNoticeId = noticeId;
            lastReadUsersSearchValue = searchValue;
            return readUsers;
        }

        @Override
        public int deleteByNoticeIds(Long[] noticeIds)
        {
            deleteCount++;
            lastDeleteNoticeIds = noticeIds;
            return noticeIds.length;
        }
    }
}
