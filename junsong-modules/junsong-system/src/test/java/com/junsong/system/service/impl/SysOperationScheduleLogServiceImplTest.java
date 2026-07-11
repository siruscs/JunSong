package com.junsong.system.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.junsong.system.domain.SysOperationAlertEvent;
import com.junsong.system.domain.SysOperationScheduleLog;
import com.junsong.system.domain.vo.AlertEventQueryParams;
import com.junsong.system.domain.vo.OperationScheduleDashboardVO;
import com.junsong.system.domain.vo.OperationScheduleLogVO;
import com.junsong.system.mapper.SysOperationScheduleLogMapper;
import com.junsong.system.service.ISysOperationAlertService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R21 运维调度日志服务测试。
 * 使用手写 FakeMapper 替代 Mockito，避免 JDK 26+ 兼容性问题。
 */
class SysOperationScheduleLogServiceImplTest
{
    private FakeMapper mapper;
    private NoOpAlertService alertService;
    private SysOperationScheduleLogServiceImpl service;

    @BeforeEach
    void setUp()
    {
        mapper = new FakeMapper();
        alertService = new NoOpAlertService();
        service = new SysOperationScheduleLogServiceImpl(mapper, alertService);
    }

    // ==================== start 测试 ====================

    @Test
    void startCreatesLogWithRunningStatus()
    {
        // 调用 start 方法
        OperationScheduleLogVO vo = service.start("R21_STOCK_DAILY_SNAPSHOT", "库存日快照", "CRON");

        // 验证 insertLog 被调用
        assertEquals(1, mapper.insertedLogs.size(), "应调用 insertLog 一次");

        // 验证插入的日志字段
        SysOperationScheduleLog inserted = mapper.insertedLogs.get(0);
        assertEquals("R21_STOCK_DAILY_SNAPSHOT", inserted.getJobCode());
        assertEquals("库存日快照", inserted.getJobName());
        assertEquals("CRON", inserted.getTriggerType());
        assertEquals("RUNNING", inserted.getStatus(), "初始状态必须为 RUNNING");
        assertNotNull(inserted.getStartedAt(), "startedAt 不能为空");

        // 验证返回 VO 包含 logId
        assertNotNull(vo, "返回 VO 不能为空");
        assertEquals("RUNNING", vo.getStatus());
    }

    // ==================== finishSuccess 测试 ====================

    @Test
    void finishSuccessUpdatesStatusAndDuration()
    {
        // 准备：先 start 一条日志
        SysOperationScheduleLog seeded = seedRunningLog("R21_CASHFLOW_FORECAST_SNAPSHOT");

        // 调用 finishSuccess
        service.finishSuccess(seeded.getLogId(), 42, "成功处理 42 条现金流预测");

        // 验证 updateLog 被调用
        assertEquals(1, mapper.updatedLogs.size(), "应调用 updateLog 一次");

        SysOperationScheduleLog updated = mapper.updatedLogs.get(0);
        assertEquals("SUCCESS", updated.getStatus(), "状态应为 SUCCESS");
        assertEquals(42, updated.getAffectedRows(), "影响行数应为 42");
        assertEquals("成功处理 42 条现金流预测", updated.getResultSummary());
        assertNotNull(updated.getFinishedAt(), "finishedAt 不能为空");
        assertNotNull(updated.getDurationMs(), "durationMs 不能为空");
        assertTrue(updated.getDurationMs() >= 0, "durationMs 应 >= 0");
    }

    // ==================== finishFailed 测试 ====================

    @Test
    void finishFailedRecordsExceptionMessage()
    {
        // 准备
        SysOperationScheduleLog seeded = seedRunningLog("R21_MEMBER_GROWTH_EFFECT_BACKFILL");

        // 模拟异常
        RuntimeException ex = new IllegalStateException("数据库连接超时");

        // 调用 finishFailed
        service.finishFailed(seeded.getLogId(), ex);

        // 验证
        assertEquals(1, mapper.updatedLogs.size());

        SysOperationScheduleLog updated = mapper.updatedLogs.get(0);
        assertEquals("FAILED", updated.getStatus(), "状态应为 FAILED");
        assertNotNull(updated.getErrorMessage(), "errorMessage 不能为空");
        // 必须包含异常类名
        assertTrue(updated.getErrorMessage().contains("IllegalStateException"),
                "errorMessage 必须包含异常类名，实际值: " + updated.getErrorMessage());
        // 必须包含异常消息
        assertTrue(updated.getErrorMessage().contains("数据库连接超时"),
                "errorMessage 必须包含异常消息，实际值: " + updated.getErrorMessage());
        assertNotNull(updated.getFinishedAt(), "finishedAt 不能为空");
        assertNotNull(updated.getDurationMs(), "durationMs 不能为空");
    }

    // ==================== finishSkipped 测试 ====================

    @Test
    void finishSkippedRecordsSummary()
    {
        // 准备
        SysOperationScheduleLog seeded = seedRunningLog("R21_OPERATION_MEMO_DRAFT");

        // 调用 finishSkipped
        service.finishSkipped(seeded.getLogId(), "今日无新增运营备忘，跳过执行");

        // 验证
        assertEquals(1, mapper.updatedLogs.size());

        SysOperationScheduleLog updated = mapper.updatedLogs.get(0);
        assertEquals("SKIPPED", updated.getStatus(), "状态应为 SKIPPED");
        assertEquals("今日无新增运营备忘，跳过执行", updated.getResultSummary(), "应记录跳过原因摘要");
        assertNull(updated.getErrorMessage(), "SKIPPED 不应有 errorMessage");
        assertNotNull(updated.getFinishedAt());
        assertNotNull(updated.getDurationMs());
    }

    // ==================== finishPartial 测试 ====================

    @Test
    void finishPartialRecordsAffectedRowsAndError()
    {
        // 准备
        SysOperationScheduleLog seeded = seedRunningLog("R21_STOCK_DAILY_SNAPSHOT");

        // 调用 finishPartial
        service.finishPartial(seeded.getLogId(), 15, "部分完成", "3 条库存数据同步失败");

        // 验证
        assertEquals(1, mapper.updatedLogs.size());

        SysOperationScheduleLog updated = mapper.updatedLogs.get(0);
        assertEquals("PARTIAL", updated.getStatus(), "状态应为 PARTIAL");
        assertEquals(15, updated.getAffectedRows(), "影响行数应为 15");
        assertEquals("部分完成", updated.getResultSummary());
        assertEquals("3 条库存数据同步失败", updated.getErrorMessage(), "应记录部分失败原因");
        assertNotNull(updated.getFinishedAt());
        assertNotNull(updated.getDurationMs());
    }

    // ==================== getDashboard 测试 ====================

    @Test
    void dashboardAggregatesLatestAndFailures()
    {
        // 准备看板数据
        SysOperationScheduleLog latestLog = buildLog("R21_STOCK_DAILY_SNAPSHOT", "SUCCESS");
        mapper.latestPerJobCode.add(latestLog);
        mapper.failedInLast24hCount = 3;
        mapper.recentFailures.add(buildLog("R21_CASHFLOW_FORECAST_SNAPSHOT", "FAILED"));

        // 调用 getDashboard
        OperationScheduleDashboardVO dashboard = service.getDashboard();

        // 验证 selectLatestPerJobCode 被调用
        assertTrue(mapper.selectLatestPerJobCodeCalled, "应调用 selectLatestPerJobCode");
        assertEquals(1, dashboard.getRecentLogs().size(), "recentLogs 应包含每个任务的最近记录");
        assertEquals("R21_STOCK_DAILY_SNAPSHOT", dashboard.getRecentLogs().get(0).getJobCode());

        // 验证 countFailedInLast24h 被调用
        assertTrue(mapper.countFailedInLast24hCalled, "应调用 countFailedInLast24h");
        assertEquals(3L, dashboard.getFailureCount24h(), "failureCount24h 应为 3");

        // 验证 selectRecentFailures 被调用
        assertTrue(mapper.selectRecentFailuresCalled, "应调用 selectRecentFailures");
        assertEquals(1, dashboard.getRecentFailures().size());
        assertEquals("FAILED", dashboard.getRecentFailures().get(0).getStatus());
    }

    // ==================== listRecent 测试 ====================

    @Test
    void listRecentDelegatesToMapper()
    {
        mapper.recentLogs.add(buildLog("R21_STOCK_DAILY_SNAPSHOT", "SUCCESS"));
        mapper.recentLogs.add(buildLog("R21_STOCK_DAILY_SNAPSHOT", "FAILED"));

        List<OperationScheduleLogVO> result = service.listRecent("R21_STOCK_DAILY_SNAPSHOT", 10);

        assertEquals(2, result.size());
        assertEquals("R21_STOCK_DAILY_SNAPSHOT", mapper.lastQueryJobCode, "应透传 jobCode 到 mapper");
        assertEquals(10, mapper.lastQueryLimit, "应透传 limit 到 mapper");
    }

    // ==================== 辅助方法 ====================

    /**
     * 在 FakeMapper 中预置一条 RUNNING 状态日志，供 finish 系列测试使用。
     */
    private SysOperationScheduleLog seedRunningLog(String jobCode)
    {
        SysOperationScheduleLog log = new SysOperationScheduleLog();
        log.setLogId((long) (mapper.insertedLogs.size() + 1));
        log.setJobCode(jobCode);
        log.setJobName(jobCode);
        log.setTriggerType("CRON");
        log.setStatus("RUNNING");
        log.setStartedAt(new Date());
        mapper.seededLogs.add(log);
        return log;
    }

    /**
     * 构建一条测试用日志对象。
     */
    private static SysOperationScheduleLog buildLog(String jobCode, String status)
    {
        SysOperationScheduleLog log = new SysOperationScheduleLog();
        log.setLogId(System.nanoTime());
        log.setJobCode(jobCode);
        log.setJobName(jobCode);
        log.setTriggerType("CRON");
        log.setStatus(status);
        log.setStartedAt(new Date());
        return log;
    }

    // ==================== Fake Mapper ====================

    /**
     * 手写 Fake Mapper：用 ArrayList 记录插入和更新，用预设列表模拟查询。
     * 不使用 Mockito，避免 JDK 26+ 兼容性问题。
     */
    static class FakeMapper implements SysOperationScheduleLogMapper
    {
        /** 记录 insertLog 调用 */
        final List<SysOperationScheduleLog> insertedLogs = new ArrayList<>();

        /** 记录 updateLog 调用 */
        final List<SysOperationScheduleLog> updatedLogs = new ArrayList<>();

        /** 预置的日志（供 selectById 查找） */
        final List<SysOperationScheduleLog> seededLogs = new ArrayList<>();

        /** 预置：每个任务编码的最近日志 */
        final List<SysOperationScheduleLog> latestPerJobCode = new ArrayList<>();

        /** 预置：最近日志列表 */
        final List<SysOperationScheduleLog> recentLogs = new ArrayList<>();

        /** 预置：最近失败记录 */
        final List<SysOperationScheduleLog> recentFailures = new ArrayList<>();

        /** 预置：过去 24 小时失败次数 */
        int failedInLast24hCount = 0;

        /** 记录 selectLatestPerJobCode 是否被调用 */
        boolean selectLatestPerJobCodeCalled = false;

        /** 记录 countFailedInLast24h 是否被调用 */
        boolean countFailedInLast24hCalled = false;

        /** 记录 selectRecentFailures 是否被调用 */
        boolean selectRecentFailuresCalled = false;

        /** 记录最后一次查询的 jobCode */
        String lastQueryJobCode;

        /** 记录最后一次查询的 limit */
        int lastQueryLimit;

        /** 自增 ID 生成器 */
        private long nextId = 1L;

        @Override
        public void insertLog(SysOperationScheduleLog log)
        {
            // 模拟数据库自增 ID
            if (log.getLogId() == null)
            {
                log.setLogId(nextId++);
            }
            insertedLogs.add(log);
        }

        @Override
        public void updateLog(SysOperationScheduleLog log)
        {
            updatedLogs.add(log);
        }

        @Override
        public SysOperationScheduleLog selectById(Long logId)
        {
            // 优先从预置数据中查找
            return seededLogs.stream()
                    .filter(l -> logId.equals(l.getLogId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("未找到 logId=" + logId));
        }

        @Override
        public List<SysOperationScheduleLog> selectRecent(String jobCode, int limit)
        {
            lastQueryJobCode = jobCode;
            lastQueryLimit = limit;
            if (jobCode == null)
            {
                return recentLogs.stream().limit(limit).collect(Collectors.toList());
            }
            return recentLogs.stream()
                    .filter(l -> jobCode.equals(l.getJobCode()))
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        @Override
        public List<SysOperationScheduleLog> selectLatestPerJobCode()
        {
            selectLatestPerJobCodeCalled = true;
            return latestPerJobCode;
        }

        @Override
        public int countFailedInLast24h()
        {
            countFailedInLast24hCalled = true;
            return failedInLast24hCount;
        }

        @Override
        public List<SysOperationScheduleLog> selectRecentFailures()
        {
            selectRecentFailuresCalled = true;
            return recentFailures;
        }
    }

    /**
     * R25 告警服务 no-op fake：构造签名变化后需要注入，但不参与断言。
     */
    static class NoOpAlertService implements ISysOperationAlertService
    {
        @Override
        public void raiseAlert(String ruleKey, String dedupKey, String sourceType, String sourceId, String severity, String title, String content)
        {
            // no-op
        }

        @Override
        public List<SysOperationAlertEvent> listEvents(AlertEventQueryParams params)
        {
            return Collections.emptyList();
        }

        @Override
        public int ackEvent(Long eventId)
        {
            return 0;
        }

        @Override
        public int resolveEvent(Long eventId)
        {
            return 0;
        }
    }
}
