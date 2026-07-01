package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.finance.domain.vo.CashflowDashboardVO;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FinanceCashflowReportServiceImpl.
 * Uses hand-written fakes (no Mockito).
 */
class FinanceCashflowReportServiceImplTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.remove();
    }

    // ── Test 1: All amounts are 0 -> returns 0, not NPE ──

    @Test
    void getCashflowDashboard_allZeros_returnsZeroNotNPE() throws Exception {
        setupAdmin();
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        // All queryForObject(sql, Object.class) return BigDecimal.ZERO by default
        // All queryForObject(sql, Long.class) return 0L by default

        FinanceCashflowReportServiceImpl service = createService(jdbc, new EmptyRemoteUserService());

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Arrays.asList(1L, 2L));

        CashflowDashboardVO vo = service.getCashflowDashboard(params);

        assertNotNull(vo, "VO should not be null");
        assertEquals(BigDecimal.ZERO, vo.getNetCashInflow(), "netCashInflow should be 0");
        assertEquals(BigDecimal.ZERO, vo.getTotalReceivedSalePayment(), "totalReceivedSalePayment should be 0");
        assertEquals(BigDecimal.ZERO, vo.getTotalVerifiedExpense(), "totalVerifiedExpense should be 0");
        assertEquals(BigDecimal.ZERO, vo.getTotalUnverifiedExpense(), "totalUnverifiedExpense should be 0");
        assertEquals(BigDecimal.ZERO, vo.getTotalAdvanceBalance(), "totalAdvanceBalance should be 0");
        assertEquals(BigDecimal.ZERO, vo.getTotalPaidInvestorPayment(), "totalPaidInvestorPayment should be 0");
        assertEquals(BigDecimal.ZERO, vo.getTotalUnpaidInvestorPayment(), "totalUnpaidInvestorPayment should be 0");
        assertEquals(0, vo.getCashPressureItems(), "cashPressureItems should be 0");
    }

    // ── Test 2: Only authorized deptIds are queried ──

    @Test
    void getCashflowDashboard_nonAdmin_filtersDeptIds() throws Exception {
        setupNonAdmin("store-mgr", 10L);
        ConfigurableRemoteUserService remoteService = new ConfigurableRemoteUserService();
        SysDept dept10 = new SysDept();
        dept10.setDeptId(10L);
        remoteService.deptListResponse = R.ok(Collections.singletonList(dept10));

        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        FinanceCashflowReportServiceImpl service = createService(jdbc, remoteService);

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Arrays.asList(10L, 20L)); // 20L not authorized

        CashflowDashboardVO vo = service.getCashflowDashboard(params);

        // Verify that the SQL queries used dept_id IN (?) with only deptId=10
        assertFalse(jdbc.recordedSqls.isEmpty(), "Should have executed SQL queries");
        for (String sql : jdbc.recordedSqls) {
            assertTrue(sql.contains("dept_id IN (?)"),
                    "SQL should contain single placeholder for one authorized dept: " + sql);
            assertTrue(sql.contains("del_flag = '0'"),
                    "SQL must include del_flag = '0': " + sql);
        }
        assertEquals(Collections.singletonList(10L), vo.getDeptIds(),
                "VO.deptIds should reflect filtered authorized depts only");
    }

    @Test
    void getCashflowDashboard_admin_queriesAllRequestedDeptIds() throws Exception {
        setupAdmin();
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        FinanceCashflowReportServiceImpl service = createService(jdbc, new EmptyRemoteUserService());

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Arrays.asList(10L, 20L, 30L));

        CashflowDashboardVO vo = service.getCashflowDashboard(params);

        assertFalse(jdbc.recordedSqls.isEmpty(), "Should have executed SQL queries");
        for (String sql : jdbc.recordedSqls) {
            assertTrue(sql.contains("dept_id IN (?, ?, ?)"),
                    "Admin SQL should contain 3 placeholders: " + sql);
        }
        assertEquals(Arrays.asList(10L, 20L, 30L), vo.getDeptIds());
    }

    // ── Test 3: Net cash inflow calculation ──

    @Test
    void getCashflowDashboard_netCashInflow_isSalePaymentMinusVerifiedExpense() throws Exception {
        setupAdmin();
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        // Configure: sale payments = 5000, verified expense = 2000
        jdbc.decimalResults.put("fin_sale_payment", new BigDecimal("5000.00"));
        jdbc.decimalResults.put("fin_expense.*status = '1'", new BigDecimal("2000.00"));

        FinanceCashflowReportServiceImpl service = createService(jdbc, new EmptyRemoteUserService());

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Collections.singletonList(1L));

        CashflowDashboardVO vo = service.getCashflowDashboard(params);

        assertEquals(new BigDecimal("5000.00"), vo.getTotalReceivedSalePayment(),
                "totalReceivedSalePayment should be 5000");
        assertEquals(new BigDecimal("2000.00"), vo.getTotalVerifiedExpense(),
                "totalVerifiedExpense should be 2000");
        assertEquals(new BigDecimal("3000.00"), vo.getNetCashInflow(),
                "netCashInflow = 5000 - 2000 = 3000");
    }

    @Test
    void getCashflowDashboard_cashPressureItems_isSumOfUnverifiedAndUnpaidCounts() throws Exception {
        setupAdmin();
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        // The count queries: 3 unverified expenses + 2 unpaid investor payments = 5
        jdbc.countResults.put("fin_expense.*status = '0'", 3L);
        jdbc.countResults.put("fin_investor_payment.*payment_status = '0'", 2L);

        FinanceCashflowReportServiceImpl service = createService(jdbc, new EmptyRemoteUserService());

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Collections.singletonList(1L));

        CashflowDashboardVO vo = service.getCashflowDashboard(params);

        assertEquals(5, vo.getCashPressureItems(),
                "cashPressureItems = unverified expense count(3) + unpaid investor count(2) = 5");
    }

    @Test
    void getCashflowDashboard_negativeNetCashInflow_whenExpensesExceedPayments() throws Exception {
        setupAdmin();
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        jdbc.decimalResults.put("fin_sale_payment", new BigDecimal("1000.00"));
        jdbc.decimalResults.put("fin_expense.*status = '1'", new BigDecimal("3000.00"));

        FinanceCashflowReportServiceImpl service = createService(jdbc, new EmptyRemoteUserService());

        ReportQueryParams params = new ReportQueryParams();
        params.setDeptIds(Collections.singletonList(1L));

        CashflowDashboardVO vo = service.getCashflowDashboard(params);

        assertEquals(new BigDecimal("-2000.00"), vo.getNetCashInflow(),
                "netCashInflow should be negative when expenses exceed payments");
    }

    // ── Helper: create service via reflection ──

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        Field field = null;
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(name);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(name + " not found in " + target.getClass().getName() + " hierarchy");
        }
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

    private static FinanceCashflowReportServiceImpl createService(
            JdbcTemplate jdbcTemplate,
            RemoteUserService remoteUserService) throws Exception {
        FinanceCashflowReportServiceImpl service = new FinanceCashflowReportServiceImpl();
        setField(service, "jdbcTemplate", jdbcTemplate);
        setField(service, "remoteUserService", remoteUserService);
        return service;
    }

    // ── Fake: RecordingJdbcTemplate ──

    /**
     * Hand-written fake JdbcTemplate that records SQL queries and returns
     * configurable results. Matches SQL by substring patterns to decide
     * which result to return.
     */
    static class RecordingJdbcTemplate extends JdbcTemplate {
        final List<String> recordedSqls = new ArrayList<>();

        /**
         * Map of SQL-substring-pattern -> BigDecimal result for queryForObject(sql, Object.class, args).
         * The first matching pattern wins.
         */
        final Map<String, BigDecimal> decimalResults = new LinkedHashMap<>();

        /**
         * Map of SQL-substring-pattern -> Long result for queryForObject(sql, Long.class, args).
         */
        final Map<String, Long> countResults = new LinkedHashMap<>();

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            recordedSqls.add(sql);

            if (requiredType == Long.class || requiredType == long.class) {
                // COUNT query
                for (Map.Entry<String, Long> entry : countResults.entrySet()) {
                    if (sql.matches(".*" + entry.getKey() + ".*")) {
                        return requiredType.cast(entry.getValue());
                    }
                }
                return requiredType.cast(0L);
            }

            // Object.class -> decimal query
            for (Map.Entry<String, BigDecimal> entry : decimalResults.entrySet()) {
                if (sql.matches(".*" + entry.getKey() + ".*")) {
                    return requiredType.cast(entry.getValue());
                }
            }
            return requiredType.cast(BigDecimal.ZERO);
        }

        @Override
        public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
            recordedSqls.add(sql);
            return null;
        }
    }

    // ── Fake: RemoteUserService implementations ──

    static class EmptyRemoteUserService implements RemoteUserService {
        @Override
        public R<LoginUser> getUserInfo(String username, String source) { return R.fail(); }
        @Override
        public R<Boolean> registerUserInfo(SysUser sysUser, String source) { return R.fail(); }
        @Override
        public R<Boolean> recordUserLogin(SysUser sysUser, String source) { return R.fail(); }
        @Override
        public R<List<SysDept>> getUserDeptList(String username, String source) {
            return R.ok(Collections.emptyList());
        }
        @Override
        public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) { return R.fail(); }
    }

    static class ConfigurableRemoteUserService implements RemoteUserService {
        R<List<SysDept>> deptListResponse = R.ok(Collections.emptyList());

        @Override
        public R<LoginUser> getUserInfo(String username, String source) { return R.fail(); }
        @Override
        public R<Boolean> registerUserInfo(SysUser sysUser, String source) { return R.fail(); }
        @Override
        public R<Boolean> recordUserLogin(SysUser sysUser, String source) { return R.fail(); }
        @Override
        public R<List<SysDept>> getUserDeptList(String username, String source) {
            return deptListResponse;
        }
        @Override
        public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) { return R.fail(); }
    }
}
