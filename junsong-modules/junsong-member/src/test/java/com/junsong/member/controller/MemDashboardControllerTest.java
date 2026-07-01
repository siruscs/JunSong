package com.junsong.member.controller;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.model.LoginUser;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemDashboardController multi-store dept-resolution tests.
 *
 * Verifies:
 * - loadAllowedDeptIds() correctly resolves user's authorized departments
 * - resolveDeptIds() correctly intersects requested vs allowed departments
 *
 * Uses hand-written fakes (not Mockito) to avoid JDK 26+ compatibility issues.
 */
class MemDashboardControllerTest
{
    private MemDashboardController controller;
    private FakeRemoteUserService fakeRemoteUserService;

    @BeforeEach
    void setUp() throws Exception
    {
        controller = new MemDashboardController();
        fakeRemoteUserService = new FakeRemoteUserService();
        setField(controller, "remoteUserService", fakeRemoteUserService);
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.remove();
    }

    // ==================== loadAllowedDeptIds ====================

    @Test
    void loadAllowedDeptIds_adminReturnsEmptyList()
    {
        setAdminUser();
        List<Long> result = controller.loadAllowedDeptIds();
        assertTrue(result.isEmpty(), "Admin should have no dept restriction (empty list)");
    }

    @Test
    void loadAllowedDeptIds_nonAdminWithAuthorizedDepts()
    {
        setNonAdminUser("testuser", null);
        fakeRemoteUserService.deptList = Arrays.asList(buildDept(100L), buildDept(200L));

        List<Long> result = controller.loadAllowedDeptIds();

        assertEquals(2, result.size());
        assertTrue(result.contains(100L));
        assertTrue(result.contains(200L));
    }

    @Test
    void loadAllowedDeptIds_nonAdminWithNoDeptsAndNoCurrentDept_returnsSentinel()
    {
        setNonAdminUser("testuser", null);
        fakeRemoteUserService.deptList = Collections.emptyList();

        List<Long> result = controller.loadAllowedDeptIds();

        assertEquals(1, result.size());
        assertEquals(-1L, result.get(0), "Should return sentinel -1L when no dept context");
    }

    @Test
    void loadAllowedDeptIds_nonAdminWithNoDeptsButHasCurrentDept()
    {
        setNonAdminUser("testuser", 42L);
        fakeRemoteUserService.deptList = Collections.emptyList();

        List<Long> result = controller.loadAllowedDeptIds();

        assertEquals(1, result.size());
        assertEquals(42L, result.get(0), "Should fall back to current deptId");
    }

    @Test
    void loadAllowedDeptIds_nonAdminRemoteFailure_fallsBackToCurrentDept()
    {
        setNonAdminUser("testuser", 55L);
        fakeRemoteUserService.shouldFail = true;

        List<Long> result = controller.loadAllowedDeptIds();

        assertEquals(1, result.size());
        assertEquals(55L, result.get(0), "Should fall back to current deptId on remote failure");
    }

    @Test
    void loadAllowedDeptIds_nonAdminRemoteFailureNoCurrentDept_returnsSentinel()
    {
        setNonAdminUser("testuser", null);
        fakeRemoteUserService.shouldFail = true;

        List<Long> result = controller.loadAllowedDeptIds();

        assertEquals(1, result.size());
        assertEquals(-1L, result.get(0), "Should return sentinel -1L when everything fails");
    }

    // ==================== resolveDeptIds ====================

    @Test
    void resolveDeptIds_adminWithNoRequest_returnsEmpty()
    {
        setAdminUser();
        List<Long> result = controller.resolveDeptIds(null);
        assertTrue(result.isEmpty(), "Admin with no request should return empty (all stores)");
    }

    @Test
    void resolveDeptIds_adminWithRequest_returnsAsIs()
    {
        setAdminUser();
        List<Long> requested = Arrays.asList(100L, 200L, 300L);
        List<Long> result = controller.resolveDeptIds(requested);
        assertEquals(3, result.size());
        assertTrue(result.containsAll(requested));
    }

    @Test
    void resolveDeptIds_nonAdminIntersection()
    {
        setNonAdminUser("testuser", null);
        fakeRemoteUserService.deptList = Arrays.asList(buildDept(100L), buildDept(200L));

        List<Long> requested = Arrays.asList(100L, 200L, 300L);
        List<Long> result = controller.resolveDeptIds(requested);

        assertEquals(2, result.size());
        assertTrue(result.contains(100L));
        assertTrue(result.contains(200L));
        assertFalse(result.contains(300L), "300 should be filtered out");
    }

    @Test
    void resolveDeptIds_nonAdminNoIntersection_returnsSentinel()
    {
        setNonAdminUser("testuser", null);
        fakeRemoteUserService.deptList = Arrays.asList(buildDept(100L), buildDept(200L));

        List<Long> requested = Arrays.asList(300L, 400L);
        List<Long> result = controller.resolveDeptIds(requested);

        assertEquals(1, result.size());
        assertEquals(-1L, result.get(0), "Empty intersection should return sentinel [-1L]");
    }

    @Test
    void resolveDeptIds_nonAdminNoRequest_returnsAllowed()
    {
        setNonAdminUser("testuser", null);
        fakeRemoteUserService.deptList = Arrays.asList(buildDept(100L), buildDept(200L));

        List<Long> result = controller.resolveDeptIds(null);

        assertEquals(2, result.size());
        assertTrue(result.contains(100L));
        assertTrue(result.contains(200L));
    }

    @Test
    void resolveDeptIds_nonAdminEmptyRequest_returnsAllowed()
    {
        setNonAdminUser("testuser", null);
        fakeRemoteUserService.deptList = Arrays.asList(buildDept(100L), buildDept(200L));

        List<Long> result = controller.resolveDeptIds(Collections.emptyList());

        assertEquals(2, result.size());
        assertTrue(result.contains(100L));
        assertTrue(result.contains(200L));
    }

    @Test
    void resolveDeptIds_adminWithoutSelectionDoesNotReturnInNull()
    {
        // Admin user, no deptIds passed
        setAdminUser();
        List<Long> result = controller.resolveDeptIds(null);

        // resolveDeptIds should NOT produce a list that leads to IN (NULL)
        assertNotNull(result, "Result must not be null");
        assertFalse(result.contains(null), "Result must not contain null elements");

        // Verify the SQL filter generated from the result does NOT contain IN (NULL)
        String filter = controller.inFilter("dept_id", result);
        assertFalse(filter.contains("NULL"), "Filter must not produce IN (NULL): " + filter);
        // For admin with empty list, should be "1=1" (no filtering = all stores)
        assertEquals("1=1", filter, "Admin with no selection should skip dept filtering");
    }

    @Test
    void resolveDeptIds_parsesCommaSeparatedDeptIds()
    {
        // Input: deptIds="100,200"
        setAdminUser();

        // Test parseDeptIds utility
        List<Long> parsed = controller.parseDeptIds("100,200");
        assertEquals(2, parsed.size());
        assertEquals(100L, parsed.get(0));
        assertEquals(200L, parsed.get(1));

        // Test with extra whitespace
        List<Long> parsedWithSpaces = controller.parseDeptIds(" 100 , 200 ");
        assertEquals(2, parsedWithSpaces.size());
        assertEquals(100L, parsedWithSpaces.get(0));
        assertEquals(200L, parsedWithSpaces.get(1));

        // Test null and empty
        assertTrue(controller.parseDeptIds(null).isEmpty());
        assertTrue(controller.parseDeptIds("").isEmpty());
        assertTrue(controller.parseDeptIds("  ").isEmpty());

        // Test duplicates are removed
        List<Long> parsedDedup = controller.parseDeptIds("100,200,100");
        assertEquals(2, parsedDedup.size());

        // Feed parsed result through resolveDeptIds
        List<Long> result = controller.resolveDeptIds(parsed);
        assertEquals(2, result.size());
        assertTrue(result.contains(100L));
        assertTrue(result.contains(200L));
    }

    // ==================== buildRankingSql (TRAE-R3-01) ====================

    @Test
    void buildRankingSql_innerSubqueryMustNotUseRAliasForDeptId()
    {
        setAdminUser();
        List<Long> resolved = Arrays.asList(100L, 200L);

        String sql = controller.buildRankingSql(resolved);

        // The inner subquery "FROM mem_points_record" has no table alias,
        // so "WHERE r.dept_id" inside it would be an SQL error.
        // Locate the inner subquery and assert it does NOT contain "WHERE r.dept_id".
        int innerFrom = sql.indexOf("FROM mem_points_record ");
        assertTrue(innerFrom >= 0, "SQL must contain inner FROM mem_points_record");

        // The inner subquery is the segment between "FROM mem_points_record" (without alias r)
        // and the closing ") latest". Find the "GROUP BY" inside the inner subquery.
        int groupBy = sql.indexOf("GROUP BY member_id");
        assertTrue(groupBy > innerFrom, "GROUP BY must appear after inner FROM");

        String innerSubquery = sql.substring(innerFrom, groupBy);
        assertFalse(innerSubquery.contains("r.dept_id"),
                "Inner subquery must NOT reference r.dept_id (no r alias in scope): " + innerSubquery);
        assertTrue(innerSubquery.contains("dept_id"),
                "Inner subquery must filter by bare dept_id: " + innerSubquery);
    }

    @Test
    void buildRankingSql_withDeptFilterHasCorrectPlaceholderCount()
    {
        setAdminUser();
        List<Long> resolved = Arrays.asList(100L, 200L, 300L);

        String sql = controller.buildRankingSql(resolved);

        // Count "?" placeholders in the IN clause inside the inner subquery.
        int inStart = sql.indexOf("dept_id IN (");
        assertTrue(inStart >= 0, "SQL must contain 'dept_id IN (' filter: " + sql);
        int inEnd = sql.indexOf(")", inStart);
        assertTrue(inEnd > inStart, "IN clause must be closed");
        String inClause = sql.substring(inStart, inEnd);
        long placeholderCount = inClause.chars().filter(c -> c == '?').count();
        assertEquals(3L, placeholderCount, "IN clause must have 3 placeholders for 3 deptIds: " + inClause);
    }

    @Test
    void buildRankingSql_emptyResolvedDoesNotProduceInNull()
    {
        setAdminUser();
        List<Long> resolved = Collections.emptyList();

        String sql = controller.buildRankingSql(resolved);

        // Empty resolved means "all stores" -> inFilter returns "1=1", must NOT produce IN (NULL).
        assertFalse(sql.contains("IN (NULL)"), "Empty deptIds must not generate IN (NULL): " + sql);
        assertTrue(sql.contains("1=1"), "Empty deptIds should produce 1=1 (no filter): " + sql);
    }

    // ==================== String deptIds parsing (TRAE-R3-02) ====================

    @Test
    void parseAndResolve_commaStringParsesToLongList()
    {
        setAdminUser();
        // Simulate the new flow: String deptIds -> parseDeptIds -> resolveDeptIds
        List<Long> requested = controller.parseDeptIds("100,200");
        List<Long> resolved = controller.resolveDeptIds(requested);
        assertEquals(2, resolved.size());
        assertTrue(resolved.contains(100L));
        assertTrue(resolved.contains(200L));
    }

    @Test
    void parseAndResolve_emptyStringDoesNotProduceInNull()
    {
        setAdminUser();
        // deptIds="" or null must not produce IN (NULL)
        List<Long> requestedEmpty = controller.parseDeptIds("");
        List<Long> resolvedEmpty = controller.resolveDeptIds(requestedEmpty);
        String filterEmpty = controller.inFilter("dept_id", resolvedEmpty);
        assertFalse(filterEmpty.contains("NULL"), "Empty deptIds must not produce IN (NULL): " + filterEmpty);
        assertEquals("1=1", filterEmpty);

        List<Long> requestedNull = controller.parseDeptIds(null);
        List<Long> resolvedNull = controller.resolveDeptIds(requestedNull);
        String filterNull = controller.inFilter("dept_id", resolvedNull);
        assertFalse(filterNull.contains("NULL"), "Null deptIds must not produce IN (NULL): " + filterNull);
    }

    @Test
    void parseAndResolve_nonAdminUnauthorizedReturnsSentinel()
    {
        setNonAdminUser("testuser", null);
        fakeRemoteUserService.deptList = Arrays.asList(buildDept(100L), buildDept(200L));

        // Request stores 300,400 which are not authorized -> intersection empty -> sentinel [-1L]
        List<Long> requested = controller.parseDeptIds("300,400");
        List<Long> resolved = controller.resolveDeptIds(requested);
        assertEquals(1, resolved.size());
        assertEquals(-1L, resolved.get(0), "Unauthorized request should return sentinel [-1L]");

        // The sentinel must produce a valid IN filter (not IN (NULL))
        String filter = controller.inFilter("dept_id", resolved);
        assertTrue(filter.contains("dept_id IN"), "Sentinel should produce IN filter: " + filter);
        assertFalse(filter.contains("NULL"), "Sentinel filter must not be IN (NULL): " + filter);
    }

    @Test
    void parseAndResolve_nonAdminAuthorizedIntersection()
    {
        setNonAdminUser("testuser", null);
        fakeRemoteUserService.deptList = Arrays.asList(buildDept(100L), buildDept(200L));

        // Request 100,200,300 where 100,200 are authorized -> intersection [100,200]
        List<Long> requested = controller.parseDeptIds("100,200,300");
        List<Long> resolved = controller.resolveDeptIds(requested);
        assertEquals(2, resolved.size());
        assertTrue(resolved.contains(100L));
        assertTrue(resolved.contains(200L));
        assertFalse(resolved.contains(300L));
    }

    // ==================== helpers ====================

    private static SysDept buildDept(Long deptId)
    {
        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        dept.setDeptName("Store-" + deptId);
        return dept;
    }

    private static void setAdminUser()
    {
        SecurityContextHolder.setUserId("1");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(1L);
        loginUser.setUsername("admin");
        loginUser.setDeptId(null);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static void setNonAdminUser(String username, Long deptId)
    {
        SecurityContextHolder.setUserId("2");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(2L);
        loginUser.setUsername(username);
        loginUser.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private static void setField(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ==================== Fake implementations ====================

    /**
     * Hand-written fake for RemoteUserService.
     * Returns configurable dept list and can simulate failures.
     */
    static class FakeRemoteUserService implements RemoteUserService
    {
        List<SysDept> deptList = Collections.emptyList();
        boolean shouldFail = false;

        @Override
        public R<List<SysDept>> getUserDeptList(String username, String source)
        {
            if (shouldFail) {
                return R.fail("simulated failure");
            }
            return R.ok(deptList);
        }

        @Override
        public R<LoginUser> getUserInfo(String username, String source)
        {
            return R.ok(null);
        }

        @Override
        public R<Boolean> registerUserInfo(SysUser sysUser, String source)
        {
            return R.ok(true);
        }

        @Override
        public R<Boolean> recordUserLogin(SysUser sysUser, String source)
        {
            return R.ok(true);
        }

        @Override
        public R<List<String>> listUsernamesByRoleKey(String roleKey, String source)
        {
            return R.ok(Collections.emptyList());
        }
    }
}
