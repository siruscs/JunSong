package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.system.api.model.LoginUser;
import com.junsong.finance.domain.FinanceReviewKnowledge;
import com.junsong.finance.domain.FinanceReviewTask;
import com.junsong.finance.mapper.FinanceReviewKnowledgeMapper;
import com.junsong.finance.mapper.FinanceReviewTaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 复盘知识库Service单测
 * Uses hand-written fakes (no Mockito).
 */
class FinanceReviewKnowledgeServiceImplTest {

    private FinanceReviewKnowledgeServiceImpl service;
    private RecordingKnowledgeMapper knowledgeMapper;
    private FakeTaskMapper taskMapper;

    private static void setupAdmin() {
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(1L);
        loginUser.setUsername("admin");
        loginUser.setDeptId(100L);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static void setupNonAdmin(String username, Long deptId) {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName(username);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(2L);
        loginUser.setUsername(username);
        loginUser.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    @BeforeEach
    void setUp() throws Exception {
        setupAdmin();
        service = new FinanceReviewKnowledgeServiceImpl();
        knowledgeMapper = new RecordingKnowledgeMapper();
        taskMapper = new FakeTaskMapper();

        inject(service, "knowledgeMapper", knowledgeMapper);
        inject(service, "reviewTaskMapper", taskMapper);
        inject(service, "remoteUserService", null);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.remove();
    }

    // ─── createFromTask tests ────────────────────────────────────────────────

    @Test
    void doneTask_canCreateKnowledge() {
        FinanceReviewTask task = doneTask(100L, "SALES_DROP", 200L);
        taskMapper.tasks.put(100L, task);

        Map<String, String> body = new HashMap<>();
        body.put("rootCause", "季节因素");
        body.put("resultSummary", "恢复正常");

        FinanceReviewKnowledge k = service.createFromTask(100L, body);

        assertNotNull(k);
        assertEquals("SALES_DROP", k.getProblemType());
        assertEquals("季节因素", k.getRootCause());
        assertEquals("恢复正常", k.getResultSummary());
        assertEquals(200L, k.getDeptId());
        assertEquals(1, knowledgeMapper.inserted.size());
    }

    @Test
    void pendingTask_cannotCreateKnowledge() {
        FinanceReviewTask task = new FinanceReviewTask();
        task.setTaskId(101L);
        task.setStatus("PENDING");
        task.setDeptId(200L);
        task.setTaskType("EXPENSE_SPIKE");
        task.setTitle("费用异常");
        task.setReason("费用飙升");
        taskMapper.tasks.put(101L, task);

        Map<String, String> body = new HashMap<>();
        body.put("actionTaken", "已调整");

        assertThrows(ServiceException.class, () -> service.createFromTask(101L, body));
    }

    @Test
    void duplicateTaskKnowledge_rejected() {
        FinanceReviewTask task = doneTask(102L, "PROFIT_RATE_DROP", 200L);
        taskMapper.tasks.put(102L, task);

        FinanceReviewKnowledge existing = new FinanceReviewKnowledge();
        existing.setKnowledgeId(1L);
        existing.setTaskId(102L);
        existing.setReusable("1");
        knowledgeMapper.existingReusable = existing;

        Map<String, String> body = new HashMap<>();
        body.put("actionTaken", "已处理");

        assertThrows(ServiceException.class, () -> service.createFromTask(102L, body));
    }

    @Test
    void emptyActionTaken_rejected() {
        FinanceReviewTask task = doneTask(103L, "PENDING_VERIFY", 200L);
        task.setHandlerNote(null); // no fallback either
        taskMapper.tasks.put(103L, task);

        Map<String, String> body = new HashMap<>();
        // no actionTaken, no handlerNote fallback

        assertThrows(ServiceException.class, () -> service.createFromTask(103L, body));
    }

    @Test
    void customProblemType_overridesTaskType() {
        FinanceReviewTask task = doneTask(104L, "SALES_DROP", 200L);
        taskMapper.tasks.put(104L, task);

        Map<String, String> body = new HashMap<>();
        body.put("problemType", "CUSTOM_TYPE");
        body.put("actionTaken", "已处理");

        FinanceReviewKnowledge k = service.createFromTask(104L, body);

        assertEquals("CUSTOM_TYPE", k.getProblemType());
    }

    // ─── P0 data permission tests ──────────────────────────────────────────

    @Test
    void listKnowledge_nonAdmin_injectsAllowedDeptIds() {
        setupNonAdmin("user1", 300L);
        Map<String, Object> params = new HashMap<>();
        service.listKnowledge(params);
        @SuppressWarnings("unchecked")
        List<Long> allowed = (List<Long>) params.get("allowedDeptIds");
        assertNotNull(allowed, "Non-admin listKnowledge must inject allowedDeptIds");
        assertTrue(allowed.contains(300L), "allowedDeptIds must contain user's currentDeptId");
    }

    @Test
    void addKnowledge_nonAdmin_unauthorizedDeptId_rejected() {
        setupNonAdmin("user1", 300L);
        FinanceReviewKnowledge k = new FinanceReviewKnowledge();
        k.setDeptId(999L); // not in allowed [300L]
        k.setProblemType("SALES_DROP");
        k.setTitle("测试");
        k.setProblemSummary("问题");
        k.setActionTaken("动作");

        assertThrows(ServiceException.class, () -> service.addKnowledge(k));
    }

    @Test
    void updateKnowledge_nonAdmin_unauthorizedDept_rejected() {
        setupNonAdmin("user1", 300L);
        FinanceReviewKnowledge existing = new FinanceReviewKnowledge();
        existing.setKnowledgeId(10L);
        existing.setDeptId(999L); // not authorized
        knowledgeMapper.existingById = existing;

        FinanceReviewKnowledge k = new FinanceReviewKnowledge();
        k.setKnowledgeId(10L);
        k.setProblemType("SALES_DROP");
        k.setTitle("测试");
        k.setProblemSummary("问题");
        k.setActionTaken("动作");

        assertThrows(ServiceException.class, () -> service.updateKnowledge(k));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private FinanceReviewTask doneTask(Long taskId, String taskType, Long deptId) {
        FinanceReviewTask task = new FinanceReviewTask();
        task.setTaskId(taskId);
        task.setStatus("DONE");
        task.setTaskType(taskType);
        task.setDeptId(deptId);
        task.setTitle("测试任务-" + taskType);
        task.setReason("触发原因");
        task.setSuggestion("建议动作");
        task.setHandlerName("张三");
        task.setHandlerNote("已处理完毕");
        return task;
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    // ─── Fakes ───────────────────────────────────────────────────────────────

    static class RecordingKnowledgeMapper implements FinanceReviewKnowledgeMapper {
        final List<FinanceReviewKnowledge> inserted = new ArrayList<>();
        FinanceReviewKnowledge existingReusable;
        FinanceReviewKnowledge existingById;
        Map<String, Object> lastListParams;

        @Override
        public List<FinanceReviewKnowledge> selectKnowledgeList(Map<String, Object> params) {
            lastListParams = params;
            return Collections.emptyList();
        }

        @Override
        public FinanceReviewKnowledge selectByKnowledgeId(Long knowledgeId) {
            return existingById;
        }

        @Override
        public FinanceReviewKnowledge selectReusableByTaskId(Long taskId) {
            return existingReusable;
        }

        @Override
        public int insertKnowledge(FinanceReviewKnowledge knowledge) {
            inserted.add(knowledge);
            return 1;
        }

        @Override
        public int updateKnowledge(FinanceReviewKnowledge knowledge) {
            return 1;
        }

        @Override
        public List<FinanceReviewKnowledge> selectRecentReusable(List<String> problemTypes, int limit) {
            return Collections.emptyList();
        }

        @Override
        public List<FinanceReviewKnowledge> selectRecommendations(String problemType, Long deptId, List<String> keywords, List<Long> allowedDeptIds, Integer limit) {
            return Collections.emptyList();
        }
    }

    static class FakeTaskMapper implements FinanceReviewTaskMapper {
        final Map<Long, FinanceReviewTask> tasks = new HashMap<>();

        @Override
        public List<FinanceReviewTask> selectReviewTaskList(Map<String, Object> params) {
            return new ArrayList<>(tasks.values());
        }

        @Override
        public FinanceReviewTask selectByTaskId(Long taskId) {
            return tasks.get(taskId);
        }

        @Override
        public FinanceReviewTask selectByAlertId(String alertId, String taskDate) {
            return null;
        }

        @Override
        public int insertReviewTask(FinanceReviewTask task) {
            return 1;
        }

        @Override
        public int updateReviewTask(FinanceReviewTask task) {
            return 1;
        }

        @Override
        public int countByStatus(String status, List<Long> deptIds) {
            return 0;
        }

        @Override
        public Map<String, Object> selectTaskEffectAmountWindow(Long deptId, Date startTime, Date endTime) {
            Map<String, Object> result = new HashMap<>();
            result.put("salesAmount", BigDecimal.ZERO);
            result.put("expenseAmount", BigDecimal.ZERO);
            return result;
        }

        @Override
        public int countSimilarOpenTasks(Long deptId, String problemType, Date startTime, Date endTime) {
            return 0;
        }

        @Override
        public List<FinanceReviewTask> selectRecentDoneTasks(List<Long> deptIds, Date sinceDate, int limit) {
            return Collections.emptyList();
        }

        @Override
        public List<FinanceReviewTask> selectReopenCandidates(List<Long> deptIds, Date cutoffDate, int limit) {
            return Collections.emptyList();
        }
    }
}
