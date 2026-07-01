package com.junsong.member.controller;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.member.mapper.MemMpDashboardMapper;
import com.junsong.member.service.IMemMpRoleModuleService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 小程序 Dashboard 查询收敛测试
 *
 * 验证趋势数据使用批量查询（一次 SQL 返回 7 天结果），
 * 替代原来的 21 次逐日查询。同时验证部门过滤和响应格式不变。
 * 使用手写 fake 替代 Mockito，避免 JDK 26+ 兼容性问题。
 */
class MemMpDashboardControllerTest
{
    private MemMpController controller;
    private FakeMpDashboardMapper mapper;

    @BeforeEach
    void setUp() throws Exception
    {
        controller = new MemMpController();
        mapper = new FakeMpDashboardMapper();
        setField(controller, "dashboardMapper", mapper);
        setField(controller, "mpRoleModuleService", new NoOpMpRoleModuleService());
    }

    // ── 趋势查询收敛：21 → 1 ──

    @Test
    void trendShouldUseBatchQueryInsteadOf21IndividualQueries()
    {
        setDeptId(10L);
        controller.getDashboardTrend();

        assertEquals(1, mapper.batchCallCount, "应调用一次批量查询");
        assertEquals(0, mapper.perDateCallCount, "不应再使用逐日查询");
        assertEquals(10L, mapper.lastBatchDeptId, "批量查询应传入 deptId");
    }

    @Test
    void trendBatchShouldPassCorrectDateRange()
    {
        setDeptId(10L);
        controller.getDashboardTrend();

        LocalDate today = LocalDate.now();
        assertEquals(today.minusDays(6).toString(), mapper.lastBatchStartDate, "startDate 应为 6 天前");
        assertEquals(today.plusDays(1).toString(), mapper.lastBatchEndDate, "endDate 应为明天");
    }

    @Test
    void trendShouldReturn7DaysOfData()
    {
        setDeptId(10L);
        prepareBatchData();

        AjaxResult result = controller.getDashboardTrend();

        assertEquals(200, result.get(AjaxResult.CODE_TAG));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get(AjaxResult.DATA_TAG);

        @SuppressWarnings("unchecked")
        List<String> dates = (List<String>) data.get("dates");
        assertEquals(7, dates.size(), "应返回 7 天日期");

        @SuppressWarnings("unchecked")
        List<Object> newMembers = (List<Object>) data.get("newMembers");
        @SuppressWarnings("unchecked")
        List<Object> dailyExpense = (List<Object>) data.get("dailyExpense");
        @SuppressWarnings("unchecked")
        List<Object> dailySale = (List<Object>) data.get("dailySale");
        assertEquals(7, newMembers.size());
        assertEquals(7, dailyExpense.size());
        assertEquals(7, dailySale.size());
    }

    @Test
    void trendShouldFillZerosForDaysWithNoData()
    {
        setDeptId(10L);
        // 不准备任何批量数据 → 所有天应补零

        AjaxResult result = controller.getDashboardTrend();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get(AjaxResult.DATA_TAG);
        @SuppressWarnings("unchecked")
        List<Object> newMembers = (List<Object>) data.get("newMembers");

        for (Object val : newMembers)
        {
            assertEquals(BigDecimal.ZERO, val, "无数据天应补零");
        }
    }

    @Test
    void trendShouldPreserveResponseFormat()
    {
        setDeptId(10L);
        prepareBatchData();

        AjaxResult result = controller.getDashboardTrend();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get(AjaxResult.DATA_TAG);
        assertTrue(data.containsKey("dates"), "应包含 dates 字段");
        assertTrue(data.containsKey("newMembers"), "应包含 newMembers 字段");
        assertTrue(data.containsKey("dailyExpense"), "应包含 dailyExpense 字段");
        assertTrue(data.containsKey("dailySale"), "应包含 dailySale 字段");
    }

    @Test
    void trendShouldReturnErrorWhenDeptIdIsNull()
    {
        setDeptId(null);

        AjaxResult result = controller.getDashboardTrend();

        assertEquals(500, result.get(AjaxResult.CODE_TAG), "deptId 为 null 时应返回错误");
        assertEquals(0, mapper.batchCallCount, "不应调用批量查询");
    }

    @Test
    void trendShouldStillFilterByDeptId()
    {
        setDeptId(42L);
        prepareBatchData();

        controller.getDashboardTrend();

        assertEquals(42L, mapper.lastBatchDeptId, "应按部门过滤");
    }

    // ── 辅助方法 ──

    private void prepareBatchData()
    {
        LocalDate today = LocalDate.now();
        mapper.batchResult = new ArrayList<>();
        for (int i = 6; i >= 0; i--)
        {
            LocalDate day = today.minusDays(i);
            Map<String, Object> row = new HashMap<>();
            row.put("stat_date", day.toString());
            row.put("new_members", new BigDecimal(i + 1));
            row.put("daily_expense", new BigDecimal((i + 1) * 100));
            row.put("daily_sale", new BigDecimal((i + 1) * 200));
            mapper.batchResult.add(row);
        }
    }

    private static void setDeptId(Long deptId)
    {
        com.junsong.system.api.model.LoginUser loginUser = new com.junsong.system.api.model.LoginUser();
        loginUser.setDeptId(deptId);
        com.junsong.common.core.context.SecurityContextHolder.set(
            com.junsong.common.core.constant.SecurityConstants.LOGIN_USER, loginUser);
    }

    private static void setField(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ── Fake 实现 ──

    /**
     * 录制型 Dashboard Mapper：记录 batch/per-date 调用次数，返回可配置趋势数据。
     */
    static class FakeMpDashboardMapper implements MemMpDashboardMapper
    {
        int batchCallCount = 0;
        int perDateCallCount = 0;
        Long lastBatchDeptId = null;
        String lastBatchStartDate = null;
        String lastBatchEndDate = null;
        List<Map<String, Object>> batchResult = new ArrayList<>();

        @Override
        public long queryCount(Long deptId, String metric)
        {
            return 0;
        }

        @Override
        public BigDecimal queryDecimal(Long deptId, String metric)
        {
            return BigDecimal.ZERO;
        }

        @Override
        public BigDecimal queryDecimalWithDate(Long deptId, String date, String metric)
        {
            perDateCallCount++;
            return BigDecimal.ZERO;
        }

        @Override
        public List<Map<String, Object>> queryTrendBatch(Long deptId, String startDate, String endDate)
        {
            batchCallCount++;
            lastBatchDeptId = deptId;
            lastBatchStartDate = startDate;
            lastBatchEndDate = endDate;
            return batchResult;
        }
    }

    static class NoOpMpRoleModuleService implements IMemMpRoleModuleService
    {
        @Override public List<com.junsong.member.domain.MemMpRoleModule> selectMpRoleModuleList(com.junsong.member.domain.MemMpRoleModule q) { return Collections.emptyList(); }
        @Override public List<String> getAccessibleModules(List<Long> roleIds, Long deptId) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectAllRoles() { return Collections.emptyList(); }
        @Override public List<Long> selectRoleIdsByRoleKeys(Set<String> roleKeys) { return Collections.emptyList(); }
        @Override public void saveRoleModules(Long roleId, Long deptId, List<String> moduleKeys) {}
        @Override public int deleteById(Long id) { return 1; }
        @Override public int deleteByRoleId(Long roleId) { return 1; }
        @Override public int deleteByRoleIdAndDeptId(Long roleId, Long deptId) { return 1; }
    }
}
