package com.junsong.system.service.impl;

import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.Test;
import com.junsong.system.domain.SysOperationAlertEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * R25操作告警服务测试。
 */
class SysOperationAlertServiceImplTest
{
    @Test
    void raiseAlertDeduplicatesByDedupKey()
    {
        FakeAlertEventMapper mapper = new FakeAlertEventMapper();
        SysOperationAlertServiceImpl service = new SysOperationAlertServiceImpl(mapper);

        service.raiseAlert("RULE_A", "dedup-1", "WEBHOOK", "wh-1", "CRITICAL", "告警标题", "告警内容");
        service.raiseAlert("RULE_A", "dedup-1", "WEBHOOK", "wh-1", "CRITICAL", "告警标题", "告警内容");

        // 同 dedupKey 第二次应命中已有事件，仅增量计数，不再次插入
        assertEquals(1, mapper.insertCount);
        assertEquals(1, mapper.incrementCount);
        assertNotNull(mapper.lastInserted);
        assertEquals("dedup-1", mapper.lastInserted.getDedupKey());
        assertEquals("OPEN", mapper.lastInserted.getStatus());
    }

    static class FakeAlertEventMapper implements com.junsong.system.mapper.SysOperationAlertEventMapper
    {
        SysOperationAlertEvent existing = null;
        SysOperationAlertEvent lastInserted = null;
        int insertCount = 0;
        int incrementCount = 0;

        @Override
        public int insertAlertEvent(SysOperationAlertEvent event)
        {
            insertCount++;
            event.setEventId((long) insertCount);
            lastInserted = event;
            existing = event; // 模拟插入后可被查到
            return 1;
        }

        @Override
        public SysOperationAlertEvent selectOpenByDedupKey(String dedupKey)
        {
            if (existing != null && dedupKey.equals(existing.getDedupKey()))
            {
                return existing;
            }
            return null;
        }

        @Override
        public int incrementHitCount(Long eventId)
        {
            incrementCount++;
            if (existing != null)
            {
                existing.setHitCount((existing.getHitCount() == null ? 0 : existing.getHitCount()) + 1);
            }
            return 1;
        }

        @Override
        public int updateEventStatus(Long eventId, String status) { return 1; }

        @Override
        public java.util.List<SysOperationAlertEvent> selectAlertEvents(
                com.junsong.system.domain.vo.AlertEventQueryParams params)
        {
            return Collections.emptyList();
        }

        @Override
        public int countOpenCritical() { return 0; }
    }
}
