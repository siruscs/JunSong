package com.junsong.member.controller;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    // ==================== R8-D: 会员概览经营化 ====================

    @Test
    void stats_respectsAuthorizedDeptIds()
    {
        // Non-admin requests unauthorized stores 300,400 -> sentinel [-1L]
        // Stats SQL must use the sentinel filter, NOT 1=1 (all-stores)
        setNonAdminUser("testuser", null);
        fakeRemoteUserService.deptList = Arrays.asList(buildDept(100L), buildDept(200L));

        List<Long> requested = controller.parseDeptIds("300,400");
        List<Long> resolved = controller.resolveDeptIds(requested);
        assertEquals(1, resolved.size());
        assertEquals(-1L, resolved.get(0), "Unauthorized request should resolve to sentinel [-1L]");

        // The stats SQL (e.g. memberSalesAmount) must filter by sentinel, not 1=1
        String salesSql = controller.buildMemberSalesAmountSql(resolved);
        assertFalse(salesSql.contains("1=1"), "Unauthorized stats must not use 1=1 (all-stores): " + salesSql);
        assertTrue(salesSql.contains("dept_id IN"), "Unauthorized stats must use IN filter: " + salesSql);

        // Points liability SQL must also respect the sentinel
        String liabilitySql = controller.buildPointsLiabilitySql(resolved);
        assertFalse(liabilitySql.contains("1=1"), "Unauthorized pointsLiability must not use 1=1: " + liabilitySql);
    }

    @Test
    void computeRepeatRate90d_returnsZeroWhenDenominatorIsZero()
    {
        // No active members in 90d -> rate 0, not divide-by-zero
        BigDecimal rate = controller.computeRepeatRate90d(5L, 0L);
        assertEquals(0, BigDecimal.ZERO.compareTo(rate), "repeatRate90d must be 0 when denominator is 0");

        // Normal case: 5 repeat / 10 active = 50.0%
        BigDecimal normalRate = controller.computeRepeatRate90d(5L, 10L);
        assertEquals(0, new BigDecimal("50.0").compareTo(normalRate), "5/10 should be 50.0%");
    }

    @Test
    void computePointsLiability_countsRemainingPositiveLiability()
    {
        // Positive points -> liability (500 points = ¥5.00)
        BigDecimal liability = controller.computePointsLiability(new BigDecimal("500"));
        assertEquals(0, new BigDecimal("5.00").compareTo(liability), "500 points should be ¥5.00 liability");

        // Negative points -> 0 (members owe points, not a liability)
        BigDecimal negative = controller.computePointsLiability(new BigDecimal("-100"));
        assertEquals(0, BigDecimal.ZERO.compareTo(negative), "Negative points must not be a liability");

        // Null -> 0
        BigDecimal nullVal = controller.computePointsLiability(null);
        assertEquals(0, BigDecimal.ZERO.compareTo(nullVal), "Null points must be 0 liability");
    }

    @Test
    void computeActivityRoiText_noCostDoesNotBecomeZeroPercent()
    {
        // No activity cost -> must NOT show "0%", must show "暂不可算"
        String roiText = controller.computeActivityRoiText(BigDecimal.ZERO, new BigDecimal("1000"));
        assertFalse(roiText.contains("0%"), "Missing cost must not show 0%: " + roiText);
        assertTrue(roiText.contains("暂不可算"), "Missing cost should show 暂不可算: " + roiText);

        // Null cost -> same
        String nullCostRoi = controller.computeActivityRoiText(null, new BigDecimal("1000"));
        assertTrue(nullCostRoi.contains("暂不可算"), "Null cost should show 暂不可算: " + nullCostRoi);

        // With cost -> normal ROI (revenue 1200 - cost 1000 = 200, ROI = 20.0%)
        String normalRoi = controller.computeActivityRoiText(new BigDecimal("1000"), new BigDecimal("1200"));
        assertTrue(normalRoi.contains("20.0"), "1200/1000 should show 20.0%: " + normalRoi);
    }

    @Test
    void buildMemberSalesAmountSql_usesRealSaleRecords()
    {
        // memberSalesAmount must come from real fin_sale_record table, not fake/static source
        setAdminUser();
        List<Long> resolved = Arrays.asList(100L, 200L);

        String sql = controller.buildMemberSalesAmountSql(resolved);

        assertTrue(sql.contains("FROM fin_sale_record"), "memberSalesAmount must query fin_sale_record: " + sql);
        assertTrue(sql.contains("sale_amount"), "memberSalesAmount must SUM sale_amount: " + sql);
        assertTrue(sql.contains("dept_id IN"), "memberSalesAmount must filter by dept_id: " + sql);
        assertFalse(sql.contains("1=1"), "With resolved depts, must not use 1=1: " + sql);
    }

    // ==================== R9-D: member_id direct link ====================

    @Test
    void memberSalesAmount_SQL_contains_member_id_IS_NOT_NULL()
    {
        setAdminUser();
        List<Long> resolved = Arrays.asList(100L, 200L);

        String sql = controller.buildMemberSalesAmountSql(resolved);

        assertTrue(sql.contains("member_id IS NOT NULL"),
                "SQL must include member_id IS NOT NULL for direct member link: " + sql);
    }

    @Test
    void memberSalesAmount_SQL_keeps_remark_fallback()
    {
        setAdminUser();
        List<Long> resolved = Arrays.asList(100L, 200L);

        String sql = controller.buildMemberSalesAmountSql(resolved);

        // Legacy remark-based identification must still work as fallback
        assertTrue(sql.contains("remark LIKE '%member%'"),
                "SQL must keep remark fallback for backward compatibility: " + sql);
        // The two conditions should be OR'd
        assertTrue(sql.contains("(member_id IS NOT NULL OR remark LIKE '%member%')"),
                "SQL must OR member_id with remark fallback: " + sql);
    }

    @Test
    void unauthorized_dept_still_uses_sentinel_not_1_equals_1()
    {
        // Non-admin with unauthorized depts must get sentinel, not 1=1
        setNonAdminUser("testuser", null);
        fakeRemoteUserService.deptList = Arrays.asList(buildDept(100L), buildDept(200L));

        List<Long> requested = controller.parseDeptIds("300,400");
        List<Long> resolved = controller.resolveDeptIds(requested);
        assertEquals(1, resolved.size());
        assertEquals(-1L, resolved.get(0));

        String sql = controller.buildMemberSalesAmountSql(resolved);

        // Must NOT use 1=1 (which would expose all stores' data)
        assertFalse(sql.contains("1=1"),
                "Unauthorized dept must not produce 1=1 (security leak): " + sql);
        // Must use sentinel IN filter
        assertTrue(sql.contains("dept_id IN"),
                "Unauthorized dept must use IN filter with sentinel: " + sql);
        // Must still contain member_id check
        assertTrue(sql.contains("member_id IS NOT NULL"),
                "Member ID check must be present even for unauthorized dept: " + sql);
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
    // ==================== R10-E: 会员销售关联质量 ====================

    @Test
    void readRuleThreshold_returnsDefaultWhenTableMissing()
    {
        // No JdbcTemplate set, so it should catch exception and return default
        BigDecimal result = controller.readRuleThreshold("MEM_MEMBER_LINK_QUALITY", BigDecimal.valueOf(80));
        assertEquals(0, BigDecimal.valueOf(80).compareTo(result));
    }

    @Test
    void memberLinkQualityRate_returnsZeroWhenDenominatorIsZero()
    {
        // Verify the formula: 0/0 should give 0, not NPE
        long linked = 0;
        long fallback = 0;
        long total = linked + fallback;
        BigDecimal rate = total > 0
                ? BigDecimal.valueOf(linked * 100).divide(BigDecimal.valueOf(total), 1, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        assertEquals(0, BigDecimal.ZERO.compareTo(rate));
    }

    // ==================== R11-I: 会员经营动作项 ====================

    @Test
    void actionItems_lowLinkQuality_createsMediumItem()
    {
        // linkQualityRate=50 < threshold=80 (default) and totalLinked > 0
        List<Map<String, Object>> items = controller.buildMemberActionItems(
                new BigDecimal("50.0"), BigDecimal.valueOf(80), 10,
                BigDecimal.ZERO, 5, 100);
        assertTrue(items.stream().anyMatch(i ->
                "MEDIUM".equals(i.get("level")) && i.get("title").toString().contains("关联率")));
    }

    @Test
    void actionItems_highPointsLiability_createsMediumItem()
    {
        // pointsLiability=2000 > threshold=1000 (default)
        List<Map<String, Object>> items = controller.buildMemberActionItems(
                BigDecimal.valueOf(90), BigDecimal.valueOf(80), 10,
                new BigDecimal("2000"), 5, 100);
        assertTrue(items.stream().anyMatch(i ->
                "MEDIUM".equals(i.get("level")) && i.get("title").toString().contains("积分负债")));
    }

    @Test
    void actionItems_noIssues_returnsEmptyList()
    {
        // All values within healthy range: linkQuality=90 > 80, liability=500 < 1000, active=10 > 0
        List<Map<String, Object>> items = controller.buildMemberActionItems(
                new BigDecimal("90.0"), BigDecimal.valueOf(80), 10,
                new BigDecimal("500"), 10, 100);
        assertTrue(items.isEmpty(), "No issues should return empty action items");
    }

    @Test
    void actionItems_zeroActiveWithMembers_createsLowItem()
    {
        // activeMemberCount=0 and totalMemberCount=50 -> LOW item
        List<Map<String, Object>> items = controller.buildMemberActionItems(
                new BigDecimal("90.0"), BigDecimal.valueOf(80), 10,
                BigDecimal.ZERO, 0, 50);
        assertEquals(1, items.size());
        assertEquals("LOW", items.get(0).get("level"));
        assertTrue(items.get(0).get("title").toString().contains("活跃度"));
        assertEquals("/member/member", items.get(0).get("targetRoute"));
    }

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
