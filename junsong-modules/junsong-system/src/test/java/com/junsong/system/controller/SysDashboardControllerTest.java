package com.junsong.system.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.junsong.system.domain.SysHealthRuleConfig;
import com.junsong.system.domain.SysOperationAuditSnapshot;
import com.junsong.system.domain.vo.AuditSnapshotQueryParams;
import com.junsong.system.domain.vo.SystemGovernanceTaskVO;
import com.junsong.system.mapper.SysHealthRuleConfigMapper;
import com.junsong.system.service.ISysOperationAuditService;
import com.junsong.system.service.impl.SysHealthRuleConfigServiceImpl;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SysDashboardController governance-task tests (R8-E).
 *
 * Verifies buildGovernanceTasks() correctly translates governance metrics
 * into actionable, severity-sorted task lists.
 *
 * Uses hand-written fakes (not Mockito) to avoid JDK 26+ compatibility issues.
 */
class SysDashboardControllerTest
{
    private SysDashboardController controller;

    @BeforeEach
    void setUp()
    {
        controller = new SysDashboardController();
        // Inject stub healthRuleConfigService for R10-D governance quality tests
        try {
            var field = SysDashboardController.class.getDeclaredField("healthRuleConfigService");
            field.setAccessible(true);
            field.set(controller, new SysHealthRuleConfigServiceImpl(new StubHealthRuleConfigMapper(), new NoOpAuditService()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void buildGovernanceTasks_noIssuesReturnsEmpty()
    {
        List<SystemGovernanceTaskVO> tasks = controller.buildGovernanceTasks(
                0, 0, 0, 0, 0, 0);
        assertTrue(tasks.isEmpty(), "No governance issues should produce empty task list");
    }

    @Test
    void buildGovernanceTasks_emptyMenuProducesHighPriorityTask()
    {
        List<SystemGovernanceTaskVO> tasks = controller.buildGovernanceTasks(
                5, 0, 0, 0, 0, 0);
        assertEquals(1, tasks.size());
        SystemGovernanceTaskVO task = tasks.get(0);
        assertEquals("EMPTY_MENU", task.getTaskType());
        assertEquals("HIGH", task.getSeverity());
        assertEquals("/system/menu", task.getTargetRoute());
        assertEquals(5, task.getCount());
        assertNotNull(task.getTitle(), "Title must not be null");
        assertNotNull(task.getReason(), "Reason must not be null");
        assertNotNull(task.getAction(), "Action must not be null");
    }

    @Test
    void buildGovernanceTasks_loginFailThreshold()
    {
        // 20 failures is NOT enough (threshold is > 20)
        List<SystemGovernanceTaskVO> atThreshold = controller.buildGovernanceTasks(
                0, 20, 0, 0, 0, 0);
        assertEquals(0, atThreshold.size(), "20 login fails should not trigger task");

        // 21 failures triggers HIGH task
        List<SystemGovernanceTaskVO> aboveThreshold = controller.buildGovernanceTasks(
                0, 21, 0, 0, 0, 0);
        assertEquals(1, aboveThreshold.size());
        assertEquals("LOGIN_FAIL", aboveThreshold.get(0).getTaskType());
        assertEquals("HIGH", aboveThreshold.get(0).getSeverity());
        assertEquals("/monitor/logininfor", aboveThreshold.get(0).getTargetRoute());
    }

    @Test
    void buildGovernanceTasks_loginFailUsesConfigThreshold()
    {
        // R10-FIX-E: 验证治理任务生成使用配置阈值，而非硬编码 20。
        // 配置阈值为 5 时，6 次失败应触发任务，5 次不应触发。
        SysHealthRuleConfigServiceImpl configuredService = new SysHealthRuleConfigServiceImpl(
                new ConfigurableStubHealthRuleConfigMapper("SYS_LOGIN_FAIL_24H", "5", "1"),
                new NoOpAuditService());
        SysDashboardController configuredController = new SysDashboardController();
        try {
            var field = SysDashboardController.class.getDeclaredField("healthRuleConfigService");
            field.setAccessible(true);
            field.set(configuredController, configuredService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 5 次失败不触发（阈值 5，> 5 才触发）
        List<SystemGovernanceTaskVO> atThreshold = configuredController.buildGovernanceTasks(
                0, 5, 0, 0, 0, 0);
        assertEquals(0, atThreshold.size(), "5 login fails should not trigger task when threshold is 5");

        // 6 次失败触发
        List<SystemGovernanceTaskVO> aboveThreshold = configuredController.buildGovernanceTasks(
                0, 6, 0, 0, 0, 0);
        assertEquals(1, aboveThreshold.size(), "6 login fails should trigger task when threshold is 5");
        assertEquals("LOGIN_FAIL", aboveThreshold.get(0).getTaskType());
    }

    @Test
    void buildGovernanceTasks_sortedBySeverity()
    {
        // All severity levels present:
        // HIGH: emptyMenuCount=1, recentLoginFailCount=25, downServiceCount=1
        // MEDIUM: menuWithoutRoleCount=1, recentHighRiskOperCount=1
        // LOW: roleWithoutUserCount=1
        List<SystemGovernanceTaskVO> tasks = controller.buildGovernanceTasks(
                1, 25, 1, 1, 1, 1);
        assertEquals(6, tasks.size());

        // HIGH tasks come first
        assertEquals("HIGH", tasks.get(0).getSeverity());
        assertEquals("HIGH", tasks.get(1).getSeverity());
        assertEquals("HIGH", tasks.get(2).getSeverity());
        // MEDIUM tasks next
        assertEquals("MEDIUM", tasks.get(3).getSeverity());
        assertEquals("MEDIUM", tasks.get(4).getSeverity());
        // LOW tasks last
        assertEquals("LOW", tasks.get(5).getSeverity());
    }

    @Test
    void buildGovernanceTasks_downServiceHighPriority()
    {
        List<SystemGovernanceTaskVO> tasks = controller.buildGovernanceTasks(
                0, 0, 0, 0, 0, 2);
        assertEquals(1, tasks.size());
        assertEquals("DOWN_SERVICE", tasks.get(0).getTaskType());
        assertEquals("HIGH", tasks.get(0).getSeverity());
        assertEquals(2, tasks.get(0).getCount());
        assertEquals("/monitor/server", tasks.get(0).getTargetRoute());
    }

    @Test
    void buildGovernanceTasks_mediumAndLowTasks()
    {
        // menuWithoutRoleCount -> MEDIUM, recentHighRiskOperCount -> MEDIUM, roleWithoutUserCount -> LOW
        List<SystemGovernanceTaskVO> tasks = controller.buildGovernanceTasks(
                0, 0, 3, 2, 5, 0);
        assertEquals(3, tasks.size());
        // First two should be MEDIUM
        assertEquals("MEDIUM", tasks.get(0).getSeverity());
        assertEquals("MEDIUM", tasks.get(1).getSeverity());
        // Last should be LOW
        assertEquals("LOW", tasks.get(2).getSeverity());
        assertEquals("ROLE_WITHOUT_USER", tasks.get(2).getTaskType());
        assertEquals("/system/role", tasks.get(2).getTargetRoute());
    }

    // ==================== R10-D: 治理质量评分测试 ====================

    @Test
    void governanceQualityScore_noRiskGives100()
    {
        int score = controller.computeGovernanceQualityScore(0, 0, 0);
        assertEquals(100, score);
    }

    @Test
    void governanceQualityScore_emptyMenuReduces()
    {
        int score = controller.computeGovernanceQualityScore(3, 0, 0);
        assertTrue(score < 100, "Empty menu should reduce score");
    }

    @Test
    void governanceQualityScore_loginFailReduces()
    {
        int score = controller.computeGovernanceQualityScore(0, 25, 0);
        assertTrue(score < 100, "Login fail above threshold should reduce score");
    }

    @Test
    void governanceQualityScore_repeatedTaskTypeReduces()
    {
        int score = controller.computeGovernanceQualityScore(0, 0, 2);
        assertTrue(score < 100, "Repeated task types should reduce score");
    }

    // ==================== R11-H: 治理动作严谨化测试 ====================

    @Test
    void governanceAction_ignoresFrontendSeverityAndCount()
    {
        // Create a subclass that overrides findCurrentGovernanceTask to return a known task
        SysDashboardController testController = new SysDashboardController() {
            @Override
            SystemGovernanceTaskVO findCurrentGovernanceTask(String taskType) {
                SystemGovernanceTaskVO task = new SystemGovernanceTaskVO();
                task.setTaskType("EMPTY_MENU");
                task.setSeverity("HIGH");
                task.setCount(7);
                return task;
            }
        };
        // Inject healthRuleConfigService
        try {
            var field = SysDashboardController.class.getDeclaredField("healthRuleConfigService");
            field.setAccessible(true);
            field.set(testController, new SysHealthRuleConfigServiceImpl(new StubHealthRuleConfigMapper(), new NoOpAuditService()));
            // Inject a fake governanceTaskLogMapper that captures the inserted log
            var mapperField = SysDashboardController.class.getDeclaredField("governanceTaskLogMapper");
            mapperField.setAccessible(true);
            mapperField.set(testController, new CapturingGovernanceTaskLogMapper());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        com.junsong.system.domain.SysGovernanceTaskLog log = new com.junsong.system.domain.SysGovernanceTaskLog();
        log.setTaskType("EMPTY_MENU");
        log.setActionType("ACK");
        // Frontend sends bogus severity/count
        log.setSeverity("LOW");
        log.setCountValue(999);

        com.junsong.common.core.domain.R<String> result = testController.recordGovernanceAction(log);
        assertEquals(200, result.getCode());

        // The log should have been overridden with backend values
        assertEquals("HIGH", log.getSeverity());
        assertEquals(7, log.getCountValue());
    }

    @Test
    void governanceAction_invalidActionTypeRejected()
    {
        com.junsong.system.domain.SysGovernanceTaskLog log = new com.junsong.system.domain.SysGovernanceTaskLog();
        log.setTaskType("EMPTY_MENU");
        log.setActionType("INVALID");

        com.junsong.common.core.domain.R<String> result = controller.recordGovernanceAction(log);
        assertNotEquals(200, result.getCode());
    }

    @Test
    void governanceAction_doneWithoutNoteRejected()
    {
        com.junsong.system.domain.SysGovernanceTaskLog log = new com.junsong.system.domain.SysGovernanceTaskLog();
        log.setTaskType("EMPTY_MENU");
        log.setActionType("DONE");
        // No handlerNote

        com.junsong.common.core.domain.R<String> result = controller.recordGovernanceAction(log);
        assertNotEquals(200, result.getCode());
    }

    @Test
    void governanceTaskVO_includesLastActionFields()
    {
        SystemGovernanceTaskVO task = new SystemGovernanceTaskVO();
        task.setLastActionType("DONE");
        task.setLastHandlerName("admin");
        task.setLastHandlerNote("已修复");
        task.setLastActionTime(new java.util.Date());

        assertEquals("DONE", task.getLastActionType());
        assertEquals("admin", task.getLastHandlerName());
        assertEquals("已修复", task.getLastHandlerNote());
        assertNotNull(task.getLastActionTime());
    }

    @Test
    void governanceAction_noCurrentTask_defaultsLowSeverity()
    {
        SysDashboardController testController = new SysDashboardController() {
            @Override
            SystemGovernanceTaskVO findCurrentGovernanceTask(String taskType) {
                return null; // no active task
            }
        };
        try {
            var field = SysDashboardController.class.getDeclaredField("healthRuleConfigService");
            field.setAccessible(true);
            field.set(testController, new SysHealthRuleConfigServiceImpl(new StubHealthRuleConfigMapper(), new NoOpAuditService()));
            var mapperField = SysDashboardController.class.getDeclaredField("governanceTaskLogMapper");
            mapperField.setAccessible(true);
            mapperField.set(testController, new CapturingGovernanceTaskLogMapper());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        com.junsong.system.domain.SysGovernanceTaskLog log = new com.junsong.system.domain.SysGovernanceTaskLog();
        log.setTaskType("NONEXISTENT_TYPE");
        log.setActionType("ACK");

        com.junsong.common.core.domain.R<String> result = testController.recordGovernanceAction(log);
        assertEquals(200, result.getCode());
        assertEquals("LOW", log.getSeverity());
        assertEquals(0, log.getCountValue());
        assertTrue(log.getHandlerNote().contains("当前无活动风险，仅记录知晓"));
    }

    // ==================== R12-F: 归档与重开测试 ====================

    @Test
    void archive_doneTaskHiddenByDefault()
    {
        // Setup: EMPTY_MENU task with latest log DONE → should be hidden
        LookupGovernanceTaskLogMapper mapper = new LookupGovernanceTaskLogMapper();
        com.junsong.system.domain.SysGovernanceTaskLog doneLog = new com.junsong.system.domain.SysGovernanceTaskLog();
        doneLog.setTaskType("EMPTY_MENU");
        doneLog.setActionType("DONE");
        doneLog.setHandlerName("admin");
        doneLog.setHandlerNote("已修复");
        doneLog.setActionTime(new java.util.Date());
        mapper.latestLogs.put("EMPTY_MENU", doneLog);

        controller = createControllerWithMapper(mapper);

        List<SystemGovernanceTaskVO> tasks = new ArrayList<>();
        SystemGovernanceTaskVO task = new SystemGovernanceTaskVO();
        task.setTaskType("EMPTY_MENU");
        task.setSeverity("HIGH");
        tasks.add(task);

        // Default: includeArchived=false → DONE task should be removed
        List<SystemGovernanceTaskVO> result = controller.markAndFilterArchivedTasks(tasks, false);
        assertTrue(result.isEmpty(), "DONE task should be hidden by default");
    }

    @Test
    void archive_includeArchivedShowsDoneTask()
    {
        LookupGovernanceTaskLogMapper mapper = new LookupGovernanceTaskLogMapper();
        com.junsong.system.domain.SysGovernanceTaskLog doneLog = new com.junsong.system.domain.SysGovernanceTaskLog();
        doneLog.setTaskType("EMPTY_MENU");
        doneLog.setActionType("DONE");
        doneLog.setHandlerName("admin");
        doneLog.setHandlerNote("已修复");
        doneLog.setActionTime(new java.util.Date());
        mapper.latestLogs.put("EMPTY_MENU", doneLog);

        controller = createControllerWithMapper(mapper);

        List<SystemGovernanceTaskVO> tasks = new ArrayList<>();
        SystemGovernanceTaskVO task = new SystemGovernanceTaskVO();
        task.setTaskType("EMPTY_MENU");
        task.setSeverity("HIGH");
        tasks.add(task);

        // includeArchived=true → DONE task should remain
        List<SystemGovernanceTaskVO> result = controller.markAndFilterArchivedTasks(tasks, true);
        assertEquals(1, result.size(), "DONE task should be visible with includeArchived=true");
        assertTrue(result.get(0).isArchived(), "Task should be marked as archived");
        assertEquals("DONE", result.get(0).getLastActionType());
    }

    @Test
    void archive_reopenRestoresTask()
    {
        // Setup: task was DONE, then REOPEN → latest log is REOPEN → should be visible
        LookupGovernanceTaskLogMapper mapper = new LookupGovernanceTaskLogMapper();
        com.junsong.system.domain.SysGovernanceTaskLog reopenLog = new com.junsong.system.domain.SysGovernanceTaskLog();
        reopenLog.setTaskType("EMPTY_MENU");
        reopenLog.setActionType("REOPEN");
        reopenLog.setHandlerName("admin");
        reopenLog.setHandlerNote("问题复发");
        reopenLog.setActionTime(new java.util.Date());
        mapper.latestLogs.put("EMPTY_MENU", reopenLog);

        controller = createControllerWithMapper(mapper);

        List<SystemGovernanceTaskVO> tasks = new ArrayList<>();
        SystemGovernanceTaskVO task = new SystemGovernanceTaskVO();
        task.setTaskType("EMPTY_MENU");
        task.setSeverity("HIGH");
        tasks.add(task);

        // Default: includeArchived=false → REOPEN task should be visible (not archived)
        List<SystemGovernanceTaskVO> result = controller.markAndFilterArchivedTasks(tasks, false);
        assertEquals(1, result.size(), "REOPEN task should be visible in default list");
        assertFalse(result.get(0).isArchived(), "REOPEN task should not be archived");
        assertEquals("REOPEN", result.get(0).getLastActionType());
    }

    @Test
    void archive_ignoredTaskHiddenByDefault()
    {
        LookupGovernanceTaskLogMapper mapper = new LookupGovernanceTaskLogMapper();
        com.junsong.system.domain.SysGovernanceTaskLog ignoredLog = new com.junsong.system.domain.SysGovernanceTaskLog();
        ignoredLog.setTaskType("ROLE_WITHOUT_USER");
        ignoredLog.setActionType("IGNORED");
        ignoredLog.setHandlerName("admin");
        ignoredLog.setHandlerNote("不再需要");
        ignoredLog.setActionTime(new java.util.Date());
        mapper.latestLogs.put("ROLE_WITHOUT_USER", ignoredLog);

        controller = createControllerWithMapper(mapper);

        List<SystemGovernanceTaskVO> tasks = new ArrayList<>();
        SystemGovernanceTaskVO task = new SystemGovernanceTaskVO();
        task.setTaskType("ROLE_WITHOUT_USER");
        task.setSeverity("LOW");
        tasks.add(task);

        List<SystemGovernanceTaskVO> result = controller.markAndFilterArchivedTasks(tasks, false);
        assertTrue(result.isEmpty(), "IGNORED task should be hidden by default");
    }

    @Test
    void governanceAction_reopenWithoutNoteRejected()
    {
        com.junsong.system.domain.SysGovernanceTaskLog log = new com.junsong.system.domain.SysGovernanceTaskLog();
        log.setTaskType("EMPTY_MENU");
        log.setActionType("REOPEN");
        // No handlerNote

        com.junsong.common.core.domain.R<String> result = controller.recordGovernanceAction(log);
        assertNotEquals(200, result.getCode(), "REOPEN without handlerNote should be rejected");
    }

    @Test
    void governanceAction_reopenWithNoteAccepted()
    {
        SysDashboardController testController = new SysDashboardController() {
            @Override
            SystemGovernanceTaskVO findCurrentGovernanceTask(String taskType) {
                SystemGovernanceTaskVO task = new SystemGovernanceTaskVO();
                task.setTaskType("EMPTY_MENU");
                task.setSeverity("HIGH");
                task.setCount(3);
                return task;
            }
        };
        try {
            var field = SysDashboardController.class.getDeclaredField("healthRuleConfigService");
            field.setAccessible(true);
            field.set(testController, new SysHealthRuleConfigServiceImpl(new StubHealthRuleConfigMapper(), new NoOpAuditService()));
            var mapperField = SysDashboardController.class.getDeclaredField("governanceTaskLogMapper");
            mapperField.setAccessible(true);
            mapperField.set(testController, new CapturingGovernanceTaskLogMapper());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        com.junsong.system.domain.SysGovernanceTaskLog log = new com.junsong.system.domain.SysGovernanceTaskLog();
        log.setTaskType("EMPTY_MENU");
        log.setActionType("REOPEN");
        log.setHandlerNote("问题复发，需要重新处理");

        com.junsong.common.core.domain.R<String> result = testController.recordGovernanceAction(log);
        assertEquals(200, result.getCode(), "REOPEN with handlerNote should be accepted");
        // Backend should have recalculated severity/count
        assertEquals("HIGH", log.getSeverity());
        assertEquals(3, log.getCountValue());
    }

    @Test
    void archive_ackTaskRemainsVisible()
    {
        // ACK should NOT archive the task
        LookupGovernanceTaskLogMapper mapper = new LookupGovernanceTaskLogMapper();
        com.junsong.system.domain.SysGovernanceTaskLog ackLog = new com.junsong.system.domain.SysGovernanceTaskLog();
        ackLog.setTaskType("EMPTY_MENU");
        ackLog.setActionType("ACK");
        ackLog.setHandlerName("admin");
        ackLog.setHandlerNote("已知晓");
        ackLog.setActionTime(new java.util.Date());
        mapper.latestLogs.put("EMPTY_MENU", ackLog);

        controller = createControllerWithMapper(mapper);

        List<SystemGovernanceTaskVO> tasks = new ArrayList<>();
        SystemGovernanceTaskVO task = new SystemGovernanceTaskVO();
        task.setTaskType("EMPTY_MENU");
        task.setSeverity("HIGH");
        tasks.add(task);

        List<SystemGovernanceTaskVO> result = controller.markAndFilterArchivedTasks(tasks, false);
        assertEquals(1, result.size(), "ACK task should remain visible");
        assertFalse(result.get(0).isArchived(), "ACK task should not be archived");
    }

    private SysDashboardController createControllerWithMapper(LookupGovernanceTaskLogMapper mapper)
    {
        SysDashboardController ctrl = new SysDashboardController();
        try {
            var healthField = SysDashboardController.class.getDeclaredField("healthRuleConfigService");
            healthField.setAccessible(true);
            healthField.set(ctrl, new SysHealthRuleConfigServiceImpl(new StubHealthRuleConfigMapper(), new NoOpAuditService()));
            var mapperField = SysDashboardController.class.getDeclaredField("governanceTaskLogMapper");
            mapperField.setAccessible(true);
            mapperField.set(ctrl, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ctrl;
    }

    static class CapturingGovernanceTaskLogMapper implements com.junsong.system.mapper.SysGovernanceTaskLogMapper {
        com.junsong.system.domain.SysGovernanceTaskLog lastInserted;
        @Override public int insertGovernanceTaskLog(com.junsong.system.domain.SysGovernanceTaskLog log) { lastInserted = log; return 1; }
        @Override public List<com.junsong.system.domain.SysGovernanceTaskLog> selectLogsByType(String taskType) { return new ArrayList<>(); }
        @Override public com.junsong.system.domain.SysGovernanceTaskLog selectLatestLogByType(String taskType) { return null; }
    }

    static class StubHealthRuleConfigMapper implements SysHealthRuleConfigMapper {
        @Override public List<SysHealthRuleConfig> selectHealthRuleList(SysHealthRuleConfig q) { return new ArrayList<>(); }
        @Override public SysHealthRuleConfig selectById(Long id) { return null; }
        @Override public SysHealthRuleConfig selectByCode(String code) { return null; }
        @Override public int updateHealthRule(SysHealthRuleConfig c) { return 0; }
    }

    /** R10-FIX-E: 可配置返回指定规则阈值的 Stub */
    static class ConfigurableStubHealthRuleConfigMapper implements SysHealthRuleConfigMapper {
        private final SysHealthRuleConfig rule;
        ConfigurableStubHealthRuleConfigMapper(String code, String threshold, String enabled) {
            this.rule = new SysHealthRuleConfig();
            this.rule.setRuleCode(code);
            this.rule.setThresholdValue(new BigDecimal(threshold));
            this.rule.setEnabled(enabled);
        }
        @Override public List<SysHealthRuleConfig> selectHealthRuleList(SysHealthRuleConfig q) { return new ArrayList<>(); }
        @Override public SysHealthRuleConfig selectById(Long id) { return null; }
        @Override public SysHealthRuleConfig selectByCode(String code) {
            return (rule != null && rule.getRuleCode().equals(code)) ? rule : null;
        }
        @Override public int updateHealthRule(SysHealthRuleConfig c) { return 0; }
    }

    /** R12-F: 查找型 Mapper，按 taskType 返回预设的最新治理记录 */
    static class LookupGovernanceTaskLogMapper implements com.junsong.system.mapper.SysGovernanceTaskLogMapper {
        final java.util.Map<String, com.junsong.system.domain.SysGovernanceTaskLog> latestLogs = new java.util.HashMap<>();
        @Override public int insertGovernanceTaskLog(com.junsong.system.domain.SysGovernanceTaskLog log) { return 1; }
        @Override public List<com.junsong.system.domain.SysGovernanceTaskLog> selectLogsByType(String taskType) { return new ArrayList<>(); }
        @Override public com.junsong.system.domain.SysGovernanceTaskLog selectLatestLogByType(String taskType) {
            return latestLogs.get(taskType);
        }
    }

    /** R25 no-op 审计服务 fake：构造签名变化后需要注入，但不参与断言。 */
    static class NoOpAuditService implements ISysOperationAuditService {
        @Override public void recordSnapshot(String bizType, String bizId, String operation, String riskLevel, Object before, Object after) { }
        @Override public List<SysOperationAuditSnapshot> listSnapshots(AuditSnapshotQueryParams params) { return Collections.emptyList(); }
    }
}
