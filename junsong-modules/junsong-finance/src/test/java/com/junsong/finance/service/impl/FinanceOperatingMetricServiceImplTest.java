package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.vo.FinanceOperationDashboardVO;
import com.junsong.finance.domain.vo.OperatingMetric;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.service.IFinanceReportService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.model.LoginUser;
import com.junsong.common.core.context.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 统一经营指标服务测试（Phase 5）。
 *
 * 覆盖：
 * - 10 个指标全部返回且结构完整
 * - 金额精度 scale=2 HALF_UP
 * - admin 用户不限制部门
 * - 非 admin 用户取授权门店交集
 * - 无授权门店 fail-closed（返回 SENTINEL [-1]）
 * - 跨部门请求取交集
 * - drillDownRoute 不指向无权限页面
 */
class FinanceOperatingMetricServiceImplTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.remove();
    }

    // ── helpers ──

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = FinanceOperatingMetricServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

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

    private FinanceOperationDashboardVO createDashboard() {
        FinanceOperationDashboardVO vo = new FinanceOperationDashboardVO();
        vo.setTodaySales(new BigDecimal("1234.567"));
        vo.setTodayExpense(new BigDecimal("500.005"));
        vo.setCurrentPeriodPaymentAmount(new BigDecimal("2000.00"));
        vo.setEndingReceivableAmount(new BigDecimal("8000.00"));
        vo.setOverdueReceivableCount(3);
        vo.setUnverifiedExpenseAmount(new BigDecimal("300.00"));
        vo.setUnverifiedAdvanceAmount(new BigDecimal("200.00"));
        return vo;
    }

    private FinanceOperatingMetricServiceImpl createService(
            IFinanceReportService reportService,
            RemoteUserService remoteUserService,
            JdbcTemplate jdbcTemplate) throws Exception {
        FinanceOperatingMetricServiceImpl svc = new FinanceOperatingMetricServiceImpl();
        setField(svc, "financeReportService", reportService);
        setField(svc, "remoteUserService", remoteUserService);
        setField(svc, "jdbcTemplate", jdbcTemplate);
        return svc;
    }

    private RemoteUserService createMockRemoteUserService(List<Long> deptIds) {
        RemoteUserService mock = mock(RemoteUserService.class);
        List<SysDept> depts = new ArrayList<>();
        for (Long id : deptIds) {
            SysDept d = new SysDept();
            d.setDeptId(id);
            depts.add(d);
        }
        when(mock.getUserDeptList(anyString(), anyString()))
                .thenReturn(R.ok(depts));
        return mock;
    }

    private JdbcTemplate createMockJdbcTemplate() {
        JdbcTemplate mock = mock(JdbcTemplate.class);
        // 负库存门店数
        when(mock.queryForObject(contains("fin_stock_ledger"), eq(Integer.class), any(Object[].class)))
                .thenReturn(2);
        // 今日新增会员
        when(mock.queryForObject(contains("mem_member WHERE"), eq(Integer.class), any(Object[].class)))
                .thenReturn(5);
        // 活跃会员
        when(mock.queryForObject(contains("COUNT(DISTINCT m.member_id)"), eq(Integer.class), any(Object[].class)))
                .thenReturn(42);
        // 待办任务
        when(mock.queryForObject(contains("sys_operating_task"), eq(Integer.class), any(Object[].class)))
                .thenReturn(7);
        return mock;
    }

    // ── 测试用例 ──

    @Test
    void returnsAllTenMetricsWithCompleteStructure() throws Exception {
        setupAdmin();
        IFinanceReportService reportService = mock(IFinanceReportService.class);
        when(reportService.getOperationDashboard(any())).thenReturn(createDashboard());

        FinanceOperatingMetricServiceImpl svc = createService(
                reportService, mock(RemoteUserService.class), createMockJdbcTemplate());

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Arrays.asList(100L, 101L));

        List<OperatingMetric> metrics = svc.getOperatingMetrics(params);

        assertEquals(10, metrics.size(), "应返回 10 个指标");

        // 验证所有指标码
        Set<String> codes = new HashSet<>();
        for (OperatingMetric m : metrics) {
            codes.add(m.getCode());
            assertNotNull(m.getValue(), "value 不能为 null: " + m.getCode());
            assertNotNull(m.getUnit(), "unit 不能为 null: " + m.getCode());
            assertNotNull(m.getPeriod(), "period 不能为 null: " + m.getCode());
            assertNotNull(m.getScope(), "scope 不能为 null: " + m.getCode());
            assertNotNull(m.getSource(), "source 不能为 null: " + m.getCode());
            assertNotNull(m.getDrillDownRoute(), "drillDownRoute 不能为 null: " + m.getCode());
        }
        assertTrue(codes.contains("todaySales"));
        assertTrue(codes.contains("todayExpense"));
        assertTrue(codes.contains("netCashflow"));
        assertTrue(codes.contains("receivableBalance"));
        assertTrue(codes.contains("overdueReceivable"));
        assertTrue(codes.contains("inventoryRisk"));
        assertTrue(codes.contains("todayNewMembers"));
        assertTrue(codes.contains("activeMembers"));
        assertTrue(codes.contains("unverifiedAmount"));
        assertTrue(codes.contains("pendingTaskCount"));
    }

    @Test
    void amountsUseScale2HalfUp() throws Exception {
        setupAdmin();
        IFinanceReportService reportService = mock(IFinanceReportService.class);
        FinanceOperationDashboardVO vo = createDashboard();
        // 1234.567 → 应舍入为 1234.57
        vo.setTodaySales(new BigDecimal("1234.567"));
        // 500.005 → 应舍入为 500.01
        vo.setTodayExpense(new BigDecimal("500.005"));
        when(reportService.getOperationDashboard(any())).thenReturn(vo);

        FinanceOperatingMetricServiceImpl svc = createService(
                reportService, mock(RemoteUserService.class), createMockJdbcTemplate());

        List<OperatingMetric> metrics = svc.getOperatingMetrics(null);

        OperatingMetric sales = metrics.stream().filter(m -> "todaySales".equals(m.getCode())).findFirst().orElse(null);
        assertNotNull(sales);
        assertEquals(2, sales.getValue().scale(), "金额精度应为 2");
        assertEquals(new BigDecimal("1234.57"), sales.getValue(), "HALF_UP 舍入");

        OperatingMetric expense = metrics.stream().filter(m -> "todayExpense".equals(m.getCode())).findFirst().orElse(null);
        assertNotNull(expense);
        assertEquals(new BigDecimal("500.01"), expense.getValue(), "HALF_UP 舍入");
    }

    @Test
    void adminUserNotRestrictedByDept() throws Exception {
        setupAdmin();
        IFinanceReportService reportService = mock(IFinanceReportService.class);
        when(reportService.getOperationDashboard(any())).thenReturn(createDashboard());

        FinanceOperatingMetricServiceImpl svc = createService(
                reportService, mock(RemoteUserService.class), createMockJdbcTemplate());

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Arrays.asList(100L, 101L));

        List<OperatingMetric> metrics = svc.getOperatingMetrics(params);

        // admin 请求的 deptIds 应直接使用
        OperatingMetric first = metrics.get(0);
        assertEquals(Arrays.asList(100L, 101L), first.getScope().getDeptIds());
    }

    @Test
    void nonAdminUserIntersectsAuthorizedDepts() throws Exception {
        setupNonAdmin("store_manager", 100L);
        IFinanceReportService reportService = mock(IFinanceReportService.class);
        when(reportService.getOperationDashboard(any())).thenReturn(createDashboard());

        // 用户授权门店 [100, 101]
        RemoteUserService remoteUserService = createMockRemoteUserService(Arrays.asList(100L, 101L));

        FinanceOperatingMetricServiceImpl svc = createService(
                reportService, remoteUserService, createMockJdbcTemplate());

        // 请求 [100, 102] — 只有 100 在授权列表中
        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Arrays.asList(100L, 102L));

        List<OperatingMetric> metrics = svc.getOperatingMetrics(params);

        OperatingMetric first = metrics.get(0);
        assertEquals(Arrays.asList(100L), first.getScope().getDeptIds(),
                "非 admin 应取请求与授权的交集");
    }

    @Test
    void noAuthorizedDeptsFailsClosed() throws Exception {
        setupNonAdmin("new_user", 999L);
        IFinanceReportService reportService = mock(IFinanceReportService.class);
        when(reportService.getOperationDashboard(any())).thenReturn(createDashboard());

        // 用户无授权门店
        RemoteUserService remoteUserService = createMockRemoteUserService(Collections.emptyList());

        FinanceOperatingMetricServiceImpl svc = createService(
                reportService, remoteUserService, createMockJdbcTemplate());

        List<OperatingMetric> metrics = svc.getOperatingMetrics(null);

        OperatingMetric first = metrics.get(0);
        assertEquals(Collections.singletonList(-1L), first.getScope().getDeptIds(),
                "无授权门店应 fail-closed 返回 SENTINEL [-1]");
    }

    @Test
    void netCashflowCalculatedAsPaymentsMinusExpense() throws Exception {
        setupAdmin();
        IFinanceReportService reportService = mock(IFinanceReportService.class);
        FinanceOperationDashboardVO vo = createDashboard();
        // 实收 2000.00 - 费用 500.01 = 1499.99
        vo.setCurrentPeriodPaymentAmount(new BigDecimal("2000.00"));
        vo.setTodayExpense(new BigDecimal("500.005"));
        when(reportService.getOperationDashboard(any())).thenReturn(vo);

        FinanceOperatingMetricServiceImpl svc = createService(
                reportService, mock(RemoteUserService.class), createMockJdbcTemplate());

        List<OperatingMetric> metrics = svc.getOperatingMetrics(null);

        OperatingMetric cashflow = metrics.stream()
                .filter(m -> "netCashflow".equals(m.getCode())).findFirst().orElse(null);
        assertNotNull(cashflow);
        assertEquals(new BigDecimal("1499.99"), cashflow.getValue(),
                "净现金流 = 实收 - 费用");
    }

    @Test
    void unverifiedAmountIsExpensePlusAdvance() throws Exception {
        setupAdmin();
        IFinanceReportService reportService = mock(IFinanceReportService.class);
        FinanceOperationDashboardVO vo = createDashboard();
        vo.setUnverifiedExpenseAmount(new BigDecimal("300.00"));
        vo.setUnverifiedAdvanceAmount(new BigDecimal("200.50"));
        when(reportService.getOperationDashboard(any())).thenReturn(vo);

        FinanceOperatingMetricServiceImpl svc = createService(
                reportService, mock(RemoteUserService.class), createMockJdbcTemplate());

        List<OperatingMetric> metrics = svc.getOperatingMetrics(null);

        OperatingMetric unverified = metrics.stream()
                .filter(m -> "unverifiedAmount".equals(m.getCode())).findFirst().orElse(null);
        assertNotNull(unverified);
        assertEquals(new BigDecimal("500.50"), unverified.getValue(),
                "待核销金额 = 未核销费用 + 未核销借支");
    }

    @Test
    void drillDownRoutesPointToValidPages() throws Exception {
        setupAdmin();
        IFinanceReportService reportService = mock(IFinanceReportService.class);
        when(reportService.getOperationDashboard(any())).thenReturn(createDashboard());

        FinanceOperatingMetricServiceImpl svc = createService(
                reportService, mock(RemoteUserService.class), createMockJdbcTemplate());

        List<OperatingMetric> metrics = svc.getOperatingMetrics(null);

        for (OperatingMetric m : metrics) {
            String route = m.getDrillDownRoute();
            assertNotNull(route);
            assertTrue(route.startsWith("/"), "drillDownRoute 应以 / 开头: " + m.getCode());
            // 不应指向无权限的系统页面
            assertNotEquals("/system/role", route);
            assertNotEquals("/monitor/logininfor", route);
        }
    }

    @Test
    void periodAndScopeAreConsistentAcrossMetrics() throws Exception {
        setupAdmin();
        IFinanceReportService reportService = mock(IFinanceReportService.class);
        when(reportService.getOperationDashboard(any())).thenReturn(createDashboard());

        FinanceOperatingMetricServiceImpl svc = createService(
                reportService, mock(RemoteUserService.class), createMockJdbcTemplate());

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Arrays.asList(100L, 101L));

        List<OperatingMetric> metrics = svc.getOperatingMetrics(params);

        // 所有指标应返回相同的 scope
        OperatingMetric.Scope firstScope = metrics.get(0).getScope();
        for (OperatingMetric m : metrics) {
            assertEquals(firstScope.getDeptIds(), m.getScope().getDeptIds(),
                    "所有指标的 deptIds 应一致: " + m.getCode());
            assertEquals(firstScope.getTenantId(), m.getScope().getTenantId(),
                    "所有指标的 tenantId 应一致: " + m.getCode());
        }
    }
}
