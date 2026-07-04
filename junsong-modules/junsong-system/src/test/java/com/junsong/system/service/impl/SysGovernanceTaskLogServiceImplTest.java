package com.junsong.system.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.junsong.system.domain.SysGovernanceTaskLog;
import com.junsong.system.mapper.SysGovernanceTaskLogMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统治理任务处理轨迹测试。
 * 使用手写 fake（RecordingMapper）替代 Mockito，避免 JDK 26+ 兼容性问题。
 */
class SysGovernanceTaskLogServiceImplTest
{
    private RecordingGovernanceTaskLogMapper mapper;

    @BeforeEach
    void setUp()
    {
        mapper = new RecordingGovernanceTaskLogMapper();
    }

    @Test
    void insertLog_writesCorrectFields()
    {
        SysGovernanceTaskLog log = new SysGovernanceTaskLog();
        log.setTaskType("EMPTY_MENU");
        log.setSeverity("HIGH");
        log.setCountValue(5);
        log.setActionType("ACK");
        log.setHandlerId(100L);
        log.setHandlerName("admin");
        log.setHandlerNote("已确认");
        log.setActionTime(new Date());

        int rows = mapper.insertGovernanceTaskLog(log);

        assertEquals(1, rows);
        assertEquals(1, mapper.insertedLogs.size());

        SysGovernanceTaskLog recorded = mapper.insertedLogs.get(0);
        assertEquals("EMPTY_MENU", recorded.getTaskType());
        assertEquals("HIGH", recorded.getSeverity());
        assertEquals(Integer.valueOf(5), recorded.getCountValue());
        assertEquals("ACK", recorded.getActionType());
        assertEquals(Long.valueOf(100L), recorded.getHandlerId());
        assertEquals("admin", recorded.getHandlerName());
        assertEquals("已确认", recorded.getHandlerNote());
        assertNotNull(recorded.getActionTime());
    }

    @Test
    void selectLogsByType_returnsMatchingLogs()
    {
        SysGovernanceTaskLog log1 = buildLog("EMPTY_MENU", "HIGH", "ACK");
        SysGovernanceTaskLog log2 = buildLog("EMPTY_MENU", "HIGH", "DONE");
        SysGovernanceTaskLog log3 = buildLog("LOGIN_FAIL", "MEDIUM", "ACK");
        mapper.seedLogs(List.of(log1, log2, log3));

        List<SysGovernanceTaskLog> result = mapper.selectLogsByType("EMPTY_MENU");

        assertEquals(2, result.size(), "应只返回 EMPTY_MENU 类型的记录");
        assertTrue(result.stream().allMatch(l -> "EMPTY_MENU".equals(l.getTaskType())));
        assertEquals("EMPTY_MENU", mapper.lastSelectType, "应记录查询的 taskType");
    }

    @Test
    void insertLog_withNullSeverity_succeeds()
    {
        SysGovernanceTaskLog log = new SysGovernanceTaskLog();
        log.setTaskType("ROLE_WITHOUT_USER");
        log.setSeverity(null);
        log.setCountValue(3);
        log.setActionType("IGNORED");
        log.setHandlerId(200L);
        log.setHandlerName("operator");
        log.setActionTime(new Date());

        int rows = mapper.insertGovernanceTaskLog(log);

        assertEquals(1, rows, "severity 为 null 时也应插入成功");
        assertEquals(1, mapper.insertedLogs.size());
        assertNull(mapper.insertedLogs.get(0).getSeverity(), "severity 应保持 null");
    }

    @Test
    void actionType_ACK_isRecorded()
    {
        SysGovernanceTaskLog log = buildLog("DOWN_SERVICE", "HIGH", "ACK");
        log.setHandlerId(300L);
        log.setHandlerName("sysadmin");
        log.setHandlerNote("已知悉，正在处理");

        int rows = mapper.insertGovernanceTaskLog(log);

        assertEquals(1, rows);
        SysGovernanceTaskLog recorded = mapper.insertedLogs.get(0);
        assertEquals("ACK", recorded.getActionType(), "actionType 应为 ACK");
        assertEquals("DOWN_SERVICE", recorded.getTaskType());
        assertEquals(Long.valueOf(300L), recorded.getHandlerId());
        assertEquals("sysadmin", recorded.getHandlerName());
        assertEquals("已知悉，正在处理", recorded.getHandlerNote());
    }

    // ── 辅助方法 ──

    private static SysGovernanceTaskLog buildLog(String taskType, String severity, String actionType)
    {
        SysGovernanceTaskLog log = new SysGovernanceTaskLog();
        log.setTaskType(taskType);
        log.setSeverity(severity);
        log.setCountValue(0);
        log.setActionType(actionType);
        log.setActionTime(new Date());
        return log;
    }

    // ── Fake 实现 ──

    /**
     * 录制型 GovernanceTaskLog Mapper：记录插入和查询行为。
     */
    static class RecordingGovernanceTaskLogMapper implements SysGovernanceTaskLogMapper
    {
        final List<SysGovernanceTaskLog> insertedLogs = new ArrayList<>();
        final List<SysGovernanceTaskLog> seededLogs = new ArrayList<>();
        String lastSelectType = null;
        int insertResult = 1;

        void seedLogs(List<SysGovernanceTaskLog> logs)
        {
            seededLogs.addAll(logs);
        }

        @Override
        public int insertGovernanceTaskLog(SysGovernanceTaskLog log)
        {
            insertedLogs.add(log);
            return insertResult;
        }

        @Override
        public List<SysGovernanceTaskLog> selectLogsByType(String taskType)
        {
            lastSelectType = taskType;
            return seededLogs.stream()
                    .filter(l -> taskType.equals(l.getTaskType()))
                    .collect(Collectors.toList());
        }

        @Override
        public SysGovernanceTaskLog selectLatestLogByType(String taskType)
        {
            return seededLogs.stream()
                    .filter(l -> taskType.equals(l.getTaskType()))
                    .reduce((a, b) -> b.getActionTime().after(a.getActionTime()) ? b : a)
                    .orElse(null);
        }
    }
}
