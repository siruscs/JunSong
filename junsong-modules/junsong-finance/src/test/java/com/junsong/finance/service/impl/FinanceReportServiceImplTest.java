package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.finance.domain.FinExpense;
import com.junsong.finance.domain.FinProfitShareDetail;
import com.junsong.finance.domain.FinProfitShareRecord;
import com.junsong.finance.domain.FinSaleRecord;
import com.junsong.finance.domain.vo.*;
import com.junsong.finance.mapper.FinExpenseMapper;
import com.junsong.finance.mapper.FinProfitShareDetailMapper;
import com.junsong.finance.mapper.FinProfitShareRecordMapper;
import com.junsong.finance.mapper.FinSaleRecordMapper;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FinanceReportServiceImplTest
{
    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.remove();
    }

    // ── 辅助方法 ──

    private static void setField(Object target, String name, Object value) throws Exception
    {
        Field field = FinanceReportServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setupAdmin()
    {
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(1L);
        loginUser.setUsername("admin");
        loginUser.setDeptId(100L);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static void setupNonAdmin(String username, Long deptId)
    {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName(username);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(2L);
        loginUser.setUsername(username);
        loginUser.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static FinanceReportServiceImpl createService(
            FinExpenseMapper expenseMapper,
            FinSaleRecordMapper saleMapper,
            FinProfitShareRecordMapper profitShareMapper,
            RemoteUserService remoteUserService) throws Exception
    {
        FinanceReportServiceImpl service = new FinanceReportServiceImpl();
        setField(service, "finExpenseMapper", expenseMapper);
        setField(service, "finSaleRecordMapper", saleMapper);
        setField(service, "finProfitShareRecordMapper", profitShareMapper);
        setField(service, "finProfitShareDetailMapper", new NoOpProfitShareDetailMapper());
        setField(service, "remoteUserService", remoteUserService);
        return service;
    }

    private static SysDept makeDept(Long deptId)
    {
        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        return dept;
    }

    // ── 原有测试 ──

    @Test
    void expenseReportUsesSentinelDeptWhenNonAdminHasNoAuthorizedDept() throws Exception
    {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("finance-user");
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, new LoginUser());

        FinanceReportServiceImpl service = new FinanceReportServiceImpl();
        RecordingFinExpenseMapper expenseMapper = new RecordingFinExpenseMapper();
        setField(service, "finExpenseMapper", expenseMapper);
        setField(service, "remoteUserService", new EmptyRemoteUserService());

        service.getExpenseReport(new ReportQueryParams());

        assertEquals(Collections.singletonList(-1L), expenseMapper.lastParams.get("deptIds"));
    }

    // ── Step 1：ExpenseReport 请求部门超出授权时回落到授权部门 ──

    @Test
    void expenseReport_requestedDeptsExceedAuthorized_fallsBackToAuthorized() throws Exception
    {
        setupNonAdmin("finance-user", 10L);
        ConfigurableRemoteUserService remoteService = new ConfigurableRemoteUserService();
        remoteService.deptListResponse = R.ok(Collections.singletonList(makeDept(10L)));

        RecordingFinExpenseMapper expenseMapper = new RecordingFinExpenseMapper();
        FinanceReportServiceImpl service = createService(expenseMapper, new NoOpSaleRecordMapper(),
                new NoOpProfitShareRecordMapper(), remoteService);

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Arrays.asList(10L, 20L)); // 20L 不在授权列表中
        service.getExpenseReport(params);

        assertEquals(Collections.singletonList(10L), expenseMapper.lastParams.get("deptIds"),
            "请求 [10,20]、授权 [10] 时，mapper 应收到 [10]");
    }

    @Test
    void expenseReport_assemblesVOFromMapperData() throws Exception
    {
        setupAdmin();
        RecordingFinExpenseMapper expenseMapper = new RecordingFinExpenseMapper();
        expenseMapper.expenseTotal = new BigDecimal("12500.50");
        List<Map<String, Object>> categoryStats = new ArrayList<>();
        Map<String, Object> cat = new HashMap<>();
        cat.put("categoryName", "办公费");
        cat.put("totalAmount", new BigDecimal("5000.00"));
        categoryStats.add(cat);
        expenseMapper.categoryStats = categoryStats;

        FinanceReportServiceImpl service = createService(expenseMapper, new NoOpSaleRecordMapper(),
                new NoOpProfitShareRecordMapper(), new ConfigurableRemoteUserService());

        ExpenseReportVO vo = service.getExpenseReport(new ReportQueryParams());

        assertEquals(new BigDecimal("12500.50"), vo.getTotalExpense(), "费用总额应为 12500.50");
        assertEquals(1, vo.getCategoryStats().size(), "分类统计应有 1 条");
        assertEquals("办公费", vo.getCategoryStats().get(0).get("categoryName"));
    }

    // ── Step 2：SaleReport 销售总额、笔数、件数、均价计算 ──

    @Test
    void saleReport_calculatesAvgPriceCorrectly() throws Exception
    {
        setupAdmin();
        FakeSaleRecordMapper saleMapper = new FakeSaleRecordMapper();
        // 构造趋势数据：总销售额 = 2000 + 1000 = 3000
        List<Map<String, Object>> trendStats = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("dateStr", "2026-06-25");
        row1.put("deptId", 1L);
        row1.put("deptName", "总部");
        row1.put("totalSales", new BigDecimal("2000.00"));
        trendStats.add(row1);
        Map<String, Object> row2 = new HashMap<>();
        row2.put("dateStr", "2026-06-26");
        row2.put("deptId", 1L);
        row2.put("deptName", "总部");
        row2.put("totalSales", new BigDecimal("1000.00"));
        trendStats.add(row2);
        saleMapper.trendStats = trendStats;
        saleMapper.saleCount = 10;  // 10 笔
        saleMapper.saleQuantity = 50; // 50 件

        FinanceReportServiceImpl service = createService(new RecordingFinExpenseMapper(), saleMapper,
                new NoOpProfitShareRecordMapper(), new ConfigurableRemoteUserService());

        SaleReportVO vo = service.getSaleReport(new ReportQueryParams());

        assertEquals(new BigDecimal("3000.00"), vo.getTotalSales(), "销售总额应为 3000.00");
        assertEquals(10, vo.getTotalCount(), "销售笔数应为 10");
        // avgPrice = totalSales / totalCount = 3000 / 10 = 300.00
        assertEquals(new BigDecimal("300.00"), vo.getAvgPrice(),
            "均价 = 总销售额 / 笔数 = 3000 / 10 = 300.00");
    }

    @Test
    void saleReport_zeroCount_avgPriceIsZero() throws Exception
    {
        setupAdmin();
        FakeSaleRecordMapper saleMapper = new FakeSaleRecordMapper();
        saleMapper.trendStats = Collections.emptyList();
        saleMapper.saleCount = 0;
        saleMapper.saleQuantity = 0;

        FinanceReportServiceImpl service = createService(new RecordingFinExpenseMapper(), saleMapper,
                new NoOpProfitShareRecordMapper(), new ConfigurableRemoteUserService());

        SaleReportVO vo = service.getSaleReport(new ReportQueryParams());

        assertEquals(BigDecimal.ZERO, vo.getTotalSales(), "无销售时总额为 0");
        assertEquals(0, vo.getTotalCount(), "无销售时笔数为 0");
        assertEquals(BigDecimal.ZERO, vo.getAvgPrice(), "无销售时均价为 0，不应除零异常");
    }

    // ── Step 3：ProfitReport 利润、利润率、回本进度 + 边界 ──

    @Test
    void profitReport_calculatesProfitRateAndRecovery() throws Exception
    {
        setupAdmin();
        // 销售趋势：totalSales = 5000
        FakeSaleRecordMapper saleMapper = new FakeSaleRecordMapper();
        List<Map<String, Object>> salesTrend = new ArrayList<>();
        Map<String, Object> saleRow = new HashMap<>();
        saleRow.put("dateStr", "2026-06-25");
        saleRow.put("deptId", 1L);
        saleRow.put("deptName", "总部");
        saleRow.put("totalSales", new BigDecimal("5000.00"));
        salesTrend.add(saleRow);
        saleMapper.trendStats = salesTrend;
        saleMapper.saleCount = 20;
        saleMapper.saleQuantity = 100;

        // 费用趋势：expenseAmount = 3000 → costTrend 的 totalAmount = 3000
        RecordingFinExpenseMapper expenseMapper = new RecordingFinExpenseMapper();
        List<Map<String, Object>> costTrend = new ArrayList<>();
        Map<String, Object> costRow = new HashMap<>();
        costRow.put("dateStr", "2026-06-25");
        costRow.put("deptId", 1L);
        costRow.put("deptName", "总部");
        costRow.put("expenseAmount", new BigDecimal("3000.00"));
        costTrend.add(costRow);
        expenseMapper.trendStats = costTrend;

        FinanceReportServiceImpl service = createService(expenseMapper, saleMapper,
                new NoOpProfitShareRecordMapper(), new ConfigurableRemoteUserService());

        ProfitReportVO vo = service.getProfitReport(new ReportQueryParams());

        // profit = 5000 - 3000 = 2000
        assertEquals(new BigDecimal("2000.00"), vo.getTotalProfit(), "利润 = 销售额 - 成本 = 2000");
        // profitRate = profit / sales * 100 = 2000 / 5000 * 100 = 40.00
        assertEquals(new BigDecimal("40.00"), vo.getProfitRate(), "利润率 = 2000/5000*100 = 40.00");
        // recoveryRate = profit / cost * 100 = 2000 / 3000 * 100 = 66.67
        assertEquals(new BigDecimal("66.67"), vo.getRecoveryRate(), "回本进度 = 2000/3000*100 = 66.67");
    }

    @Test
    void profitReport_salesZero_profitRateIsZero() throws Exception
    {
        setupAdmin();
        FakeSaleRecordMapper saleMapper = new FakeSaleRecordMapper();
        saleMapper.trendStats = Collections.emptyList();
        saleMapper.saleCount = 0;
        saleMapper.saleQuantity = 0;

        RecordingFinExpenseMapper expenseMapper = new RecordingFinExpenseMapper();
        expenseMapper.trendStats = Collections.emptyList();

        FinanceReportServiceImpl service = createService(expenseMapper, saleMapper,
                new NoOpProfitShareRecordMapper(), new ConfigurableRemoteUserService());

        ProfitReportVO vo = service.getProfitReport(new ReportQueryParams());

        assertEquals(BigDecimal.ZERO, vo.getTotalProfit(), "无销售无成本时利润为 0");
        assertEquals(BigDecimal.ZERO, vo.getProfitRate(), "销售额为 0 时利润率应为 0，不应除零");
        assertEquals(BigDecimal.ZERO, vo.getRecoveryRate(), "成本为 0 时回本进度应为 0，不应除零");
    }

    @Test
    void profitReport_costZero_recoveryRateIsZero() throws Exception
    {
        setupAdmin();
        // 有销售但无成本
        FakeSaleRecordMapper saleMapper = new FakeSaleRecordMapper();
        List<Map<String, Object>> salesTrend = new ArrayList<>();
        Map<String, Object> saleRow = new HashMap<>();
        saleRow.put("dateStr", "2026-06-25");
        saleRow.put("deptId", 1L);
        saleRow.put("deptName", "总部");
        saleRow.put("totalSales", new BigDecimal("1000.00"));
        salesTrend.add(saleRow);
        saleMapper.trendStats = salesTrend;
        saleMapper.saleCount = 5;
        saleMapper.saleQuantity = 10;

        RecordingFinExpenseMapper expenseMapper = new RecordingFinExpenseMapper();
        expenseMapper.trendStats = Collections.emptyList(); // 无成本

        FinanceReportServiceImpl service = createService(expenseMapper, saleMapper,
                new NoOpProfitShareRecordMapper(), new ConfigurableRemoteUserService());

        ProfitReportVO vo = service.getProfitReport(new ReportQueryParams());

        assertEquals(new BigDecimal("1000.00"), vo.getTotalProfit(), "无成本时利润等于销售额");
        assertEquals(new BigDecimal("100.00"), vo.getProfitRate(), "利润率 = 1000/1000*100 = 100");
        assertEquals(BigDecimal.ZERO, vo.getRecoveryRate(), "成本为 0 时回本进度应为 0");
    }

    // ── Step 4：ProfitShareReport buildQueryParams 传递 deptIds/startTime/endTime ──

    @Test
    void profitShareReport_passesQueryParamsCorrectly() throws Exception
    {
        setupAdmin();
        FakeProfitShareRecordMapper profitShareMapper = new FakeProfitShareRecordMapper();

        FinanceReportServiceImpl service = createService(new RecordingFinExpenseMapper(),
                new NoOpSaleRecordMapper(), profitShareMapper, new ConfigurableRemoteUserService());

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Arrays.asList(10L, 20L));
        Date startTime = new GregorianCalendar(2026, Calendar.JUNE, 1).getTime();
        Date endTime = new GregorianCalendar(2026, Calendar.JUNE, 30).getTime();
        params.setStartTime(startTime);
        params.setEndTime(endTime);

        service.getProfitShareReport(params);

        assertNotNull(profitShareMapper.lastParams, "分润 mapper 应收到查询参数");
        assertEquals(Arrays.asList(10L, 20L), profitShareMapper.lastParams.get("deptIds"),
            "deptIds 应原样传递");
        assertEquals(startTime, profitShareMapper.lastParams.get("startTime"),
            "startTime 应原样传递");
        assertEquals(endTime, profitShareMapper.lastParams.get("endTime"),
            "endTime 应原样传递");
    }

    @Test
    void profitShareReport_assemblesAllVofields() throws Exception
    {
        setupAdmin();
        FakeProfitShareRecordMapper profitShareMapper = new FakeProfitShareRecordMapper();
        profitShareMapper.managerProfitTotal = new BigDecimal("8000.00");
        profitShareMapper.investorProfitTotal = new BigDecimal("5000.00");
        profitShareMapper.profitShareTotal = new BigDecimal("13000.00");

        FinanceReportServiceImpl service = createService(new RecordingFinExpenseMapper(),
                new NoOpSaleRecordMapper(), profitShareMapper, new ConfigurableRemoteUserService());

        ProfitShareReportVO vo = service.getProfitShareReport(new ReportQueryParams());

        assertEquals(new BigDecimal("8000.00"), vo.getTotalManagerProfit(), "店长分润应为 8000");
        assertEquals(new BigDecimal("5000.00"), vo.getTotalInvestorProfit(), "投资人分润应为 5000");
        assertEquals(new BigDecimal("13000.00"), vo.getTotalProfitShare(), "分润总额应为 13000");
    }

    // ── R2 预警与复盘测试 ──

    @Test
    void getAlerts_nonAdmin_filtersUnauthorizedDeptIds() throws Exception
    {
        setupNonAdmin("store-mgr", 10L);
        ConfigurableRemoteUserService remoteService = new ConfigurableRemoteUserService();
        remoteService.deptListResponse = R.ok(Collections.singletonList(makeDept(10L)));

        RecordingFinExpenseMapper expenseMapper = new RecordingFinExpenseMapper();
        FakeSaleRecordMapper saleMapper = new FakeSaleRecordMapper();
        FakeProfitShareRecordMapper profitShareMapper = new FakeProfitShareRecordMapper();

        FinanceReportServiceImpl service = createService(expenseMapper, saleMapper,
                profitShareMapper, remoteService);

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Arrays.asList(10L, 20L, 30L));

        service.getAlerts(params);

        assertEquals(Collections.singletonList(10L), params.getDeptIds(),
            "非 admin 请求 [10,20,30]、授权 [10] 时，params.deptIds 应被过滤为 [10]");
    }

    @Test
    void getAlerts_noAllowedDept_usesSentinelAndReturnsNoData() throws Exception
    {
        // Non-admin with no deptId on LoginUser and empty RemoteUserService response
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName("no-dept-user");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(2L);
        loginUser.setUsername("no-dept-user");
        // No deptId set → getDeptId() returns null
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);

        ConfigurableRemoteUserService remoteService = new ConfigurableRemoteUserService();
        remoteService.deptListResponse = R.ok(Collections.emptyList());

        RecordingFinExpenseMapper expenseMapper = new RecordingFinExpenseMapper();
        FakeSaleRecordMapper saleMapper = new FakeSaleRecordMapper();

        FinanceReportServiceImpl service = createService(expenseMapper, saleMapper,
                new FakeProfitShareRecordMapper(), remoteService);

        List<FinanceAlertVO> alerts = service.getAlerts(new ReportQueryParams());

        // With sentinel deptIds [-1L], all mapper queries return 0 → no alerts triggered
        assertTrue(alerts.isEmpty(), "哨兵部门 -1L 应匹配不到数据，不触发任何预警");
    }

    @Test
    void getAlerts_generatesSixTypesWithActionAndRoute() throws Exception
    {
        setupAdmin();
        // Configure mappers to trigger all 6 alert types:
        RecordingFinExpenseMapper expenseMapper = new RecordingFinExpenseMapper();
        expenseMapper.monthTotalExpense = new BigDecimal("11000");
        expenseMapper.prevMonthTotalExpense = new BigDecimal("8000"); // +37.5% > 30%
        expenseMapper.unverifiedCount = 5;
        expenseMapper.unverifiedAmount = new BigDecimal("6000"); // > 5000

        FakeSaleRecordMapper saleMapper = new FakeSaleRecordMapper();
        saleMapper.monthTotalSales = new BigDecimal("10000");
        saleMapper.prevMonthTotalSales = new BigDecimal("13000"); // -23.1% < -20%
        saleMapper.memberSalesAmount = new BigDecimal("500"); // 500/10000=5% < 20%

        FakeProfitShareRecordMapper profitShareMapper = new FakeProfitShareRecordMapper();
        profitShareMapper.unsettledCount = 3; // > 0

        FinanceReportServiceImpl service = createService(expenseMapper, saleMapper,
                profitShareMapper, new ConfigurableRemoteUserService());

        List<FinanceAlertVO> alerts = service.getAlerts(new ReportQueryParams());

        // Verify 6 alerts generated
        assertEquals(6, alerts.size(), "应触发全部 6 类预警");

        // Verify all 6 types present
        Set<String> alertTypes = new HashSet<>();
        for (FinanceAlertVO a : alerts) {
            alertTypes.add(a.getAlertType());
        }
        assertTrue(alertTypes.contains("SALES_DROP"), "应包含销售下滑预警");
        assertTrue(alertTypes.contains("EXPENSE_SPIKE"), "应包含费用突增预警");
        assertTrue(alertTypes.contains("PROFIT_RATE_DROP"), "应包含利润率偏低预警");
        assertTrue(alertTypes.contains("PENDING_VERIFY"), "应包含未核销费用堆积预警");
        assertTrue(alertTypes.contains("PROFIT_SHARE_EXCEPTION"), "应包含分润结算异常预警");
        assertTrue(alertTypes.contains("MEMBER_CONTRIBUTION_DROP"), "应包含会员贡献偏低预警");

        // Verify each alert has action and route
        for (FinanceAlertVO a : alerts) {
            assertNotNull(a.getSuggestedAction(), "预警 " + a.getAlertType() + " 应有建议操作");
            assertFalse(a.getSuggestedAction().isEmpty(), "预警 " + a.getAlertType() + " 建议操作不应为空");
            assertNotNull(a.getTargetRoute(), "预警 " + a.getAlertType() + " 应有目标路由");
            assertFalse(a.getTargetRoute().isEmpty(), "预警 " + a.getAlertType() + " 目标路由不应为空");
            assertNotNull(a.getAlertId(), "预警应有 alertId");
            assertNotNull(a.getAlertLevel(), "预警应有 alertLevel");
        }

        // Verify sorting: HIGH before MEDIUM before LOW
        for (int i = 1; i < alerts.size(); i++) {
            String prevLevel = alerts.get(i - 1).getAlertLevel();
            String currLevel = alerts.get(i).getAlertLevel();
            int prevRank = "HIGH".equals(prevLevel) ? 0 : ("MEDIUM".equals(prevLevel) ? 1 : 2);
            int currRank = "HIGH".equals(currLevel) ? 0 : ("MEDIUM".equals(currLevel) ? 1 : 2);
            assertTrue(prevRank <= currRank,
                "预警应按 HIGH→MEDIUM→LOW 排序，位置 " + (i-1) + "=" + prevLevel + " 不应在位置 " + i + "=" + currLevel + " 之后");
        }
    }

    @Test
    void getReviewTasks_reusesAlertsAndSortsByPriorityAndImpact() throws Exception
    {
        setupAdmin();
        // Trigger multiple alerts at different levels
        RecordingFinExpenseMapper expenseMapper = new RecordingFinExpenseMapper();
        expenseMapper.monthTotalExpense = new BigDecimal("11000");
        expenseMapper.prevMonthTotalExpense = new BigDecimal("8000");
        expenseMapper.unverifiedCount = 5;
        expenseMapper.unverifiedAmount = new BigDecimal("6000");

        FakeSaleRecordMapper saleMapper = new FakeSaleRecordMapper();
        saleMapper.monthTotalSales = new BigDecimal("10000");
        saleMapper.prevMonthTotalSales = new BigDecimal("13000");
        saleMapper.memberSalesAmount = new BigDecimal("500");

        FakeProfitShareRecordMapper profitShareMapper = new FakeProfitShareRecordMapper();
        profitShareMapper.unsettledCount = 3;

        FinanceReportServiceImpl service = createService(expenseMapper, saleMapper,
                profitShareMapper, new ConfigurableRemoteUserService());

        List<FinanceReviewTaskVO> tasks = service.getReviewTasks(new ReportQueryParams());

        // Same number as alerts (6)
        assertEquals(6, tasks.size(), "复盘任务数量应等于预警数量 6");

        // Verify sorted by priority: HIGH > MEDIUM > LOW
        for (int i = 1; i < tasks.size(); i++) {
            String prevPri = tasks.get(i - 1).getPriority();
            String currPri = tasks.get(i).getPriority();
            int prevRank = priorityRank(prevPri);
            int currRank = priorityRank(currPri);
            assertTrue(prevRank <= currRank,
                "复盘任务应按优先级排序，位置 " + (i-1) + "=" + prevPri + " 不应在位置 " + i + "=" + currPri + " 之后");
        }

        // Verify each task has required fields from alert
        for (FinanceReviewTaskVO t : tasks) {
            assertNotNull(t.getTaskId(), "任务应有 taskId");
            assertNotNull(t.getTaskType(), "任务应有 taskType");
            assertNotNull(t.getTaskTitle(), "任务应有 taskTitle");
            assertNotNull(t.getSuggestedAction(), "任务应有 suggestedAction");
            assertNotNull(t.getTargetRoute(), "任务应有 targetRoute");
        }
    }

    @Test
    void getReviewTasks_noAlerts_returnsHealthyTask() throws Exception
    {
        setupAdmin();
        // All mapper defaults → no alerts triggered
        FinanceReportServiceImpl service = createService(
                new RecordingFinExpenseMapper(),
                new FakeSaleRecordMapper(),
                new FakeProfitShareRecordMapper(),
                new ConfigurableRemoteUserService());

        List<FinanceReviewTaskVO> tasks = service.getReviewTasks(new ReportQueryParams());

        assertEquals(1, tasks.size(), "无预警时应返回 1 条健康任务");
        assertEquals("HEALTHY", tasks.get(0).getTaskId(), "健康任务 taskId 应为 HEALTHY");
        assertEquals("INFO", tasks.get(0).getPriority(), "健康任务优先级应为 INFO");
        assertEquals("经营健康", tasks.get(0).getTaskTitle(), "健康任务标题应为'经营健康'");
    }

    private static int priorityRank(String priority)
    {
        switch (priority) {
            case "HIGH": return 0;
            case "MEDIUM": return 1;
            case "LOW": return 2;
            default: return 3;
        }
    }

    // ── 原有 Fake ──

    private static final class EmptyRemoteUserService implements RemoteUserService
    {
        @Override
        public R<LoginUser> getUserInfo(String username, String source)
        {
            return R.fail();
        }

        @Override
        public R<Boolean> registerUserInfo(SysUser sysUser, String source)
        {
            return R.fail();
        }

        @Override
        public R<Boolean> recordUserLogin(SysUser sysUser, String source)
        {
            return R.fail();
        }

        @Override
        public R<List<SysDept>> getUserDeptList(String username, String source)
        {
            return R.ok(Collections.emptyList());
        }

        @Override
        public R<List<String>> listUsernamesByRoleKey(String roleKey, String source)
        {
            return R.fail();
        }
    }

    // ── 新增 Fake：可配置 RemoteUserService ──

    static class ConfigurableRemoteUserService implements RemoteUserService
    {
        R<List<SysDept>> deptListResponse = R.ok(Collections.emptyList());

        @Override
        public R<LoginUser> getUserInfo(String username, String source) { return R.fail(); }

        @Override
        public R<Boolean> registerUserInfo(SysUser sysUser, String source) { return R.fail(); }

        @Override
        public R<Boolean> recordUserLogin(SysUser sysUser, String source) { return R.fail(); }

        @Override
        public R<List<SysDept>> getUserDeptList(String username, String source)
        {
            return deptListResponse;
        }

        @Override
        public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) { return R.fail(); }
    }

    // ── 原有 Fake：RecordingFinExpenseMapper（增强为可配置返回值） ──

    static class RecordingFinExpenseMapper implements FinExpenseMapper
    {
        Map<String, Object> lastParams;
        BigDecimal expenseTotal = BigDecimal.ZERO;
        List<Map<String, Object>> categoryStats = Collections.emptyList();
        List<Map<String, Object>> trendStats = Collections.emptyList();
        List<Map<String, Object>> deptStats = Collections.emptyList();

        @Override
        public FinExpense selectFinExpenseByExpenseId(Long expenseId) { return null; }

        @Override
        public List<FinExpense> selectFinExpenseList(FinExpense finExpense) { return Collections.emptyList(); }

        @Override
        public int insertFinExpense(FinExpense finExpense) { return 0; }

        @Override
        public int updateFinExpense(FinExpense finExpense) { return 0; }

        @Override
        public int deleteFinExpenseByExpenseId(Long expenseId) { return 0; }

        @Override
        public int deleteFinExpenseByExpenseIds(Long[] expenseIds) { return 0; }

        @Override
        public FinExpense checkExpenseNoUnique(String expenseNo) { return null; }

        @Override
        public int countTodayExpenses() { return 0; }

        @Override
        public BigDecimal sumUnverifiedExpenses() { return BigDecimal.ZERO; }

        @Override
        public BigDecimal sumUnverifiedExpensesByDeptId(Long deptId) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal sumAllExpenses() { return BigDecimal.ZERO; }

        @Override
        public BigDecimal sumAllExpensesByDeptId(Long deptId) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal sumAllExpensesByPeriodId(Long periodId) { return BigDecimal.ZERO; }

        @Override
        public List<FinExpense> selectFinExpenseByExpenseIds(Long[] expenseIds) { return Collections.emptyList(); }

        @Override
        public List<Map<String, Object>> selectExpenseCategoryStats(Map<String, Object> params)
        {
            lastParams = params;
            return categoryStats;
        }

        @Override
        public List<Map<String, Object>> selectExpenseTrendStats(Map<String, Object> params)
        {
            lastParams = params;
            return trendStats;
        }

        @Override
        public List<Map<String, Object>> selectExpenseDeptStats(Map<String, Object> params)
        {
            lastParams = params;
            return deptStats;
        }

        @Override
        public BigDecimal selectExpenseTotal(Map<String, Object> params)
        {
            lastParams = params;
            return expenseTotal;
        }

        @Override public BigDecimal selectTodayTotalExpense(List<Long> deptIds) { return BigDecimal.ZERO; }

        BigDecimal monthTotalExpense = BigDecimal.ZERO;
        BigDecimal prevMonthTotalExpense = BigDecimal.ZERO;
        int unverifiedCount = 0;
        BigDecimal unverifiedAmount = BigDecimal.ZERO;

        @Override public BigDecimal selectMonthTotalExpense(List<Long> deptIds) { return monthTotalExpense; }
        @Override
        public BigDecimal selectMonthTotalExpenseForPrev(List<Long> deptIds) {
            return prevMonthTotalExpense;
        }
        @Override public int countUnverifiedExpenses(List<Long> deptIds) { return unverifiedCount; }
        @Override public BigDecimal sumUnverifiedExpenseAmount(List<Long> deptIds) { return unverifiedAmount; }
        @Override public int countUnverifiedExpensesByPeriodId(List<Long> deptIds, Long periodId) { return 0; }
        @Override public BigDecimal sumUnverifiedExpenseAmountByPeriodId(List<Long> deptIds, Long periodId) { return unverifiedAmount; }
        @Override public List<Map<String, Object>> selectExpenseCategoryStatsWithPrev(List<Long> d, Date s, Date e, Date ps, Date pe) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectUnverifiedExpenseList(List<Long> deptIds) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectOcrAnomalies(List<Long> deptIds) { return Collections.emptyList(); }
    }

    // ── 新增 Fake：FinSaleRecordMapper ──

    static class FakeSaleRecordMapper implements FinSaleRecordMapper
    {
        List<Long> lastDeptIds;
        Date lastStartTime;
        Date lastEndTime;
        List<Map<String, Object>> trendStats = Collections.emptyList();
        int saleCount = 0;
        int saleQuantity = 0;

        @Override
        public FinSaleRecord selectFinSaleRecordBySaleId(Long saleId) { return null; }

        @Override
        public List<FinSaleRecord> selectFinSaleRecordList(FinSaleRecord finSaleRecord) { return Collections.emptyList(); }

        @Override
        public int insertFinSaleRecord(FinSaleRecord finSaleRecord) { return 0; }

        @Override
        public int updateFinSaleRecord(FinSaleRecord finSaleRecord) { return 0; }

        @Override
        public int deleteFinSaleRecordBySaleId(Long saleId) { return 0; }

        @Override
        public int deleteFinSaleRecordBySaleIds(Long[] saleIds) { return 0; }

        @Override
        public List<Map<String, Object>> selectSaleTrendStats(List<Long> deptIds, Date startTime, Date endTime)
        {
            this.lastDeptIds = deptIds;
            this.lastStartTime = startTime;
            this.lastEndTime = endTime;
            return trendStats;
        }

        @Override
        public int countSaleRecords(List<Long> deptIds, Date startTime, Date endTime)
        {
            this.lastDeptIds = deptIds;
            return saleCount;
        }

        @Override
        public int sumSaleQuantity(List<Long> deptIds, Date startTime, Date endTime)
        {
            this.lastDeptIds = deptIds;
            return saleQuantity;
        }

        @Override
        public FinSaleRecord checkSaleNoUnique(String saleNo) { return null; }

        @Override
        public int countTodaySales() { return 0; }

        @Override public BigDecimal selectTodayTotalSales(List<Long> deptIds) { return BigDecimal.ZERO; }

        BigDecimal monthTotalSales = BigDecimal.ZERO;
        BigDecimal prevMonthTotalSales = BigDecimal.ZERO;
        BigDecimal memberSalesAmount = BigDecimal.ZERO;

        @Override public BigDecimal selectMonthTotalSales(List<Long> deptIds) { return monthTotalSales; }
        @Override public BigDecimal selectTodayTotalSalesForPrev(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalSalesForPrev(List<Long> deptIds) { return prevMonthTotalSales; }
        @Override public List<Map<String, Object>> selectSalesByDept(List<Long> d, Date s, Date e) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectProductSalesRank(List<Long> d, Date s, Date e) { return Collections.emptyList(); }
        @Override public BigDecimal selectMemberSales(List<Long> d, Date s, Date e) { return memberSalesAmount; }
        @Override public BigDecimal selectSeckillSales(List<Long> d, Date s, Date e) { return BigDecimal.ZERO; }
    }

    static class NoOpSaleRecordMapper implements FinSaleRecordMapper
    {
        @Override
        public FinSaleRecord selectFinSaleRecordBySaleId(Long saleId) { return null; }

        @Override
        public List<FinSaleRecord> selectFinSaleRecordList(FinSaleRecord r) { return Collections.emptyList(); }

        @Override
        public int insertFinSaleRecord(FinSaleRecord r) { return 0; }

        @Override
        public int updateFinSaleRecord(FinSaleRecord r) { return 0; }

        @Override
        public int deleteFinSaleRecordBySaleId(Long saleId) { return 0; }

        @Override
        public int deleteFinSaleRecordBySaleIds(Long[] saleIds) { return 0; }

        @Override
        public List<Map<String, Object>> selectSaleTrendStats(List<Long> deptIds, Date startTime, Date endTime)
        {
            return Collections.emptyList();
        }

        @Override
        public int countSaleRecords(List<Long> deptIds, Date startTime, Date endTime) { return 0; }

        @Override
        public int sumSaleQuantity(List<Long> deptIds, Date startTime, Date endTime) { return 0; }

        @Override
        public FinSaleRecord checkSaleNoUnique(String saleNo) { return null; }

        @Override
        public int countTodaySales() { return 0; }

        @Override public BigDecimal selectTodayTotalSales(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalSales(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectTodayTotalSalesForPrev(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectMonthTotalSalesForPrev(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public List<Map<String, Object>> selectSalesByDept(List<Long> d, Date s, Date e) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectProductSalesRank(List<Long> d, Date s, Date e) { return Collections.emptyList(); }
        @Override public BigDecimal selectMemberSales(List<Long> d, Date s, Date e) { return BigDecimal.ZERO; }
        @Override public BigDecimal selectSeckillSales(List<Long> d, Date s, Date e) { return BigDecimal.ZERO; }
    }

    // ── 新增 Fake：FinProfitShareRecordMapper ──

    static class FakeProfitShareRecordMapper implements FinProfitShareRecordMapper
    {
        Map<String, Object> lastParams;
        BigDecimal managerProfitTotal = BigDecimal.ZERO;
        BigDecimal investorProfitTotal = BigDecimal.ZERO;
        BigDecimal profitShareTotal = BigDecimal.ZERO;

        @Override
        public FinProfitShareRecord selectFinProfitShareRecordByShareId(Long shareId) { return null; }

        @Override
        public FinProfitShareRecord selectFinProfitShareRecordByPeriodId(Long periodId) { return null; }

        @Override
        public List<FinProfitShareRecord> selectFinProfitShareRecordList(FinProfitShareRecord r) { return Collections.emptyList(); }

        @Override
        public int insertFinProfitShareRecord(FinProfitShareRecord r) { return 0; }

        @Override
        public int updateFinProfitShareRecord(FinProfitShareRecord r) { return 0; }

        @Override
        public int deleteFinProfitShareRecordByShareId(Long shareId) { return 0; }

        @Override
        public int deleteFinProfitShareRecordByShareIds(Long[] shareIds) { return 0; }

        @Override
        public BigDecimal selectProfitShareTotal(Map<String, Object> params)
        {
            lastParams = params;
            return profitShareTotal;
        }

        @Override
        public BigDecimal selectManagerProfitTotal(Map<String, Object> params)
        {
            lastParams = params;
            return managerProfitTotal;
        }

        @Override
        public BigDecimal selectInvestorProfitTotal(Map<String, Object> params)
        {
            lastParams = params;
            return investorProfitTotal;
        }

        @Override
        public List<Map<String, Object>> selectManagerProfitByDept(Map<String, Object> params)
        {
            lastParams = params;
            return Collections.emptyList();
        }

        @Override
        public List<Map<String, Object>> selectInvestorProfitByDept(Map<String, Object> params)
        {
            lastParams = params;
            return Collections.emptyList();
        }

        @Override
        public List<Map<String, Object>> selectProfitShareTrend(Map<String, Object> params)
        {
            lastParams = params;
            return Collections.emptyList();
        }

        int unsettledCount = 0;

        @Override public int countUnsettledRecords(List<Long> deptIds) { return unsettledCount; }
        @Override public int countUnsettledRecordsByPeriodId(List<Long> deptIds, Long periodId) { return unsettledCount; }
        @Override public List<Map<String, Object>> selectSettlementByDept(List<Long> d, Date s, Date e) { return Collections.emptyList(); }
        @Override public BigDecimal selectPaidAmount(List<Long> d, Date s, Date e) { return BigDecimal.ZERO; }
    }

    static class NoOpProfitShareRecordMapper implements FinProfitShareRecordMapper
    {
        @Override
        public FinProfitShareRecord selectFinProfitShareRecordByShareId(Long shareId) { return null; }

        @Override
        public FinProfitShareRecord selectFinProfitShareRecordByPeriodId(Long periodId) { return null; }

        @Override
        public List<FinProfitShareRecord> selectFinProfitShareRecordList(FinProfitShareRecord r) { return Collections.emptyList(); }

        @Override
        public int insertFinProfitShareRecord(FinProfitShareRecord r) { return 0; }

        @Override
        public int updateFinProfitShareRecord(FinProfitShareRecord r) { return 0; }

        @Override
        public int deleteFinProfitShareRecordByShareId(Long shareId) { return 0; }

        @Override
        public int deleteFinProfitShareRecordByShareIds(Long[] shareIds) { return 0; }

        @Override
        public BigDecimal selectProfitShareTotal(Map<String, Object> params) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectManagerProfitTotal(Map<String, Object> params) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal selectInvestorProfitTotal(Map<String, Object> params) { return BigDecimal.ZERO; }

        @Override
        public List<Map<String, Object>> selectManagerProfitByDept(Map<String, Object> params) { return Collections.emptyList(); }

        @Override
        public List<Map<String, Object>> selectInvestorProfitByDept(Map<String, Object> params) { return Collections.emptyList(); }

        @Override
        public List<Map<String, Object>> selectProfitShareTrend(Map<String, Object> params) { return Collections.emptyList(); }

        @Override public int countUnsettledRecords(List<Long> deptIds) { return 0; }
        @Override public int countUnsettledRecordsByPeriodId(List<Long> deptIds, Long periodId) { return 0; }
        @Override public List<Map<String, Object>> selectSettlementByDept(List<Long> d, Date s, Date e) { return Collections.emptyList(); }
        @Override public BigDecimal selectPaidAmount(List<Long> d, Date s, Date e) { return BigDecimal.ZERO; }
    }

    // ── NoOp Fake：FinProfitShareDetailMapper ──

    static class NoOpProfitShareDetailMapper implements FinProfitShareDetailMapper
    {
        @Override
        public List<FinProfitShareDetail> selectFinProfitShareDetailByShareId(Long shareId) { return Collections.emptyList(); }

        @Override
        public int insertFinProfitShareDetail(FinProfitShareDetail detail) { return 0; }

        @Override
        public int updateFinProfitShareDetail(FinProfitShareDetail detail) { return 0; }

        @Override
        public BigDecimal selectDetailSumByShareId(Long shareId) { return BigDecimal.ZERO; }
    }
}
