package com.junsong.member.service.impl;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.domain.R;
import com.junsong.member.mapper.MemMpDashboardMapper;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.model.LoginUser;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 小程序 Dashboard Service 多租户/部门边界与权限隐藏测试。
 *
 * 验证点：
 * 1. admin 不限制部门范围（deptIds 为空，SQL 不加 IN 过滤）
 * 2. 非 admin 与请求 deptIds 求交集
 * 3. 无可见部门时返回哨兵 [-1L]
 * 4. 未授权模块不返回对应分组
 * 5. 单模块失败不影响其他分组
 */
class MemMpDashboardServiceImplTest
{
    private MemMpDashboardServiceImpl service;
    private RecordingMapper mapper;
    private StubRemoteUserService remoteUserService;

    @BeforeEach
    void setUp() throws Exception
    {
        service = new MemMpDashboardServiceImpl();
        mapper = new RecordingMapper();
        remoteUserService = new StubRemoteUserService();
        setField(service, "dashboardMapper", mapper);
        setField(service, "remoteUserService", remoteUserService);
    }

    // ── 多租户与部门边界 ──

    @Test
    void adminShouldNotRestrictDeptIds()
    {
        setAdminUser();
        TenantContext.setTenantId(7L);

        service.resolveDeptIds(null);

        // admin 返回空列表（SQL 不加 dept_id IN 过滤）
        assertTrue(mapper.lastDeptIds == null || mapper.lastDeptIds.isEmpty(),
            "admin 不应限制 deptIds");
    }

    @Test
    void nonAdminShouldUseAuthorizedDeptsWhenNoRequest()
    {
        setNonAdminUser(10L);
        remoteUserService.authorizedDepts = Arrays.asList(10L, 11L);
        TenantContext.setTenantId(7L);

        List<Long> resolved = service.resolveDeptIds(null);

        assertEquals(Arrays.asList(10L, 11L), resolved, "应使用授权门店列表");
    }

    @Test
    void nonAdminShouldIntersectRequestedWithAuthorized()
    {
        setNonAdminUser(10L);
        remoteUserService.authorizedDepts = Arrays.asList(10L, 11L, 12L);
        TenantContext.setTenantId(7L);

        List<Long> resolved = service.resolveDeptIds(Arrays.asList(11L, 99L));

        // 99 不在授权范围内，应被过滤掉
        assertEquals(Collections.singletonList(11L), resolved, "应求交集");
    }

    @Test
    void nonAdminWithEmptyIntersectionShouldReturnSentinel()
    {
        setNonAdminUser(10L);
        remoteUserService.authorizedDepts = Arrays.asList(10L, 11L);
        TenantContext.setTenantId(7L);

        List<Long> resolved = service.resolveDeptIds(Arrays.asList(99L, 100L));

        assertEquals(Collections.singletonList(-1L), resolved, "交集为空应返回哨兵");
    }

    @Test
    void nonAdminWithoutAuthorizedDeptsShouldFallbackToCurrentDeptId()
    {
        setNonAdminUser(42L);
        remoteUserService.authorizedDepts = Collections.emptyList();
        TenantContext.setTenantId(7L);

        List<Long> resolved = service.resolveDeptIds(null);

        assertEquals(Collections.singletonList(42L), resolved, "应回退到当前 deptId");
    }

    @Test
    void nonAdminWithoutAnythingShouldReturnSentinel()
    {
        setNonAdminUser(null);
        remoteUserService.authorizedDepts = Collections.emptyList();
        TenantContext.setTenantId(7L);

        List<Long> resolved = service.resolveDeptIds(null);

        assertEquals(Collections.singletonList(-1L), resolved, "无任何部门应返回哨兵");
    }

    // ── 权限隐藏 ──

    @Test
    void overviewShouldOnlyReturnAuthorizedGroups()
    {
        setAdminUser();
        TenantContext.setTenantId(7L);

        // 只给 member 权限
        Map<String, Object> result = service.getOverview(Arrays.asList("member"));

        assertTrue(result.containsKey("tenantId"));
        assertTrue(result.containsKey("deptId"));
        assertTrue(result.containsKey("member"), "有 member 权限应返回会员分组");
        assertTrue(result.containsKey("growth"), "member 权限应同时返回成长体系分组");
        assertTrue(result.containsKey("level"), "member 权限应同时返回等级分布");
        assertTrue(result.containsKey("segment"), "member 权限应同时返回分层洞察");
        assertFalse(result.containsKey("points"), "无 pointsRecord/pointsExchange 权限不应返回积分分组");
        assertFalse(result.containsKey("activity"), "无 seckill 权限不应返回活动分组");
        assertFalse(result.containsKey("finance"), "无 expense/advance/sale 权限不应返回财务分组");
    }

    @Test
    void overviewShouldReturnAllGroupsWhenAllModulesAuthorized()
    {
        setAdminUser();
        TenantContext.setTenantId(7L);

        Map<String, Object> result = service.getOverview(Arrays.asList(
            "member", "pointsRecord", "pointsExchange", "seckill", "seckillRecord",
            "expense", "advance", "sale"));

        assertTrue(result.containsKey("member"));
        assertTrue(result.containsKey("growth"));
        assertTrue(result.containsKey("points"));
        assertTrue(result.containsKey("level"));
        assertTrue(result.containsKey("segment"));
        assertTrue(result.containsKey("activity"));
        assertTrue(result.containsKey("finance"));
    }

    @Test
    void overviewShouldReturnEmptyWhenNoModulesAuthorized()
    {
        setAdminUser();
        TenantContext.setTenantId(7L);

        Map<String, Object> result = service.getOverview(Collections.emptyList());

        assertFalse(result.containsKey("member"));
        assertFalse(result.containsKey("growth"));
        assertFalse(result.containsKey("points"));
        assertFalse(result.containsKey("level"));
        assertFalse(result.containsKey("segment"));
        assertFalse(result.containsKey("activity"));
        assertFalse(result.containsKey("finance"));
        // 仍应返回上下文标识
        assertTrue(result.containsKey("tenantId"));
        assertTrue(result.containsKey("deptId"));
    }

    // ── 单模块失败容错 ──

    @Test
    void overviewShouldNotFailWhenOneModuleThrows()
    {
        setAdminUser();
        TenantContext.setTenantId(7L);
        mapper.throwOnMember = true;

        Map<String, Object> result = service.getOverview(Arrays.asList("member", "pointsRecord"));

        // member 失败但应回退到空数据，不影响其他分组
        assertTrue(result.containsKey("member"), "member 失败也应返回空数据（避免前端白屏）");
        assertTrue(result.containsKey("points"), "points 应正常返回");
        Map<String, Object> memberData = (Map<String, Object>) result.get("member");
        assertEquals(0L, memberData.get("totalMembers"), "失败时应返回 0");
    }

    // ── 字段归一化 ──

    @Test
    void overviewShouldNormalizeMemberFields()
    {
        setAdminUser();
        TenantContext.setTenantId(7L);
        Map<String, Object> memberRaw = new HashMap<>();
        memberRaw.put("totalMembers", 100);
        memberRaw.put("todayMembers", 5);
        memberRaw.put("activeMembers", 30);
        mapper.memberResult = memberRaw;

        Map<String, Object> result = service.getOverview(Arrays.asList("member"));
        Map<String, Object> member = (Map<String, Object>) result.get("member");

        assertEquals(100L, member.get("totalMembers"));
        assertEquals(5L, member.get("todayMembers"));
        assertEquals(30L, member.get("activeMembers"));
        assertEquals(70L, member.get("silentMembers"), "silentMembers 应为 total - active");
    }

    @Test
    void overviewShouldComputeGrowthActionEffectRate()
    {
        setAdminUser();
        TenantContext.setTenantId(7L);
        Map<String, Object> growthRaw = new HashMap<>();
        growthRaw.put("pendingGrowthActions", 3L);
        growthRaw.put("completedGrowthActions", 7L);
        growthRaw.put("totalGrowthActions", 10L);
        mapper.growthResult = growthRaw;

        Map<String, Object> result = service.getOverview(Arrays.asList("member"));
        Map<String, Object> growth = (Map<String, Object>) result.get("growth");

        assertEquals(70L, growth.get("growthActionEffectRate"), "完成率应为 7/10=70%");
    }

    @Test
    void overviewShouldHandleNullRawMapGracefully()
    {
        setAdminUser();
        TenantContext.setTenantId(7L);
        mapper.memberResult = null;
        mapper.growthResult = null;

        Map<String, Object> result = service.getOverview(Arrays.asList("member"));

        // 不应抛 NPE
        assertTrue(result.containsKey("member"));
        assertTrue(result.containsKey("growth"));
        Map<String, Object> member = (Map<String, Object>) result.get("member");
        assertEquals(0L, member.get("totalMembers"));
    }

    // ── 辅助方法 ──

    private void setAdminUser()
    {
        LoginUser loginUser = new LoginUser();
        loginUser.setDeptId(1L);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
        SecurityContextHolder.setUserId("1"); // userId=1 → admin
    }

    private void setNonAdminUser(Long deptId)
    {
        LoginUser loginUser = new LoginUser();
        loginUser.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
        SecurityContextHolder.setUserId("999"); // 非 admin
    }

    private static void setField(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ── Fake 实现 ──

    static class RecordingMapper implements MemMpDashboardMapper
    {
        List<Long> lastDeptIds = null;
        Long lastTenantId = null;
        boolean throwOnMember = false;

        Map<String, Object> memberResult = new HashMap<>();
        Map<String, Object> growthResult = new HashMap<>();
        Map<String, Object> pointsResult = new HashMap<>();
        List<Map<String, Object>> levelResult = new ArrayList<>();
        List<Map<String, Object>> segmentResult = new ArrayList<>();
        Map<String, Object> activityResult = new HashMap<>();
        Map<String, Object> financeResult = new HashMap<>();

        @Override
        public long queryCount(Long deptId, String metric) { return 0; }

        @Override
        public BigDecimal queryDecimal(Long deptId, String metric) { return BigDecimal.ZERO; }

        @Override
        public BigDecimal queryDecimalWithDate(Long deptId, String date, String metric) { return BigDecimal.ZERO; }

        @Override
        public List<Map<String, Object>> queryTrendBatch(Long deptId, String startDate, String endDate) {
            return Collections.emptyList();
        }

        @Override
        public Map<String, Object> queryMemberOverview(Long tenantId, List<Long> deptIds) {
            lastTenantId = tenantId;
            lastDeptIds = deptIds;
            if (throwOnMember) throw new RuntimeException("mock failure");
            return memberResult;
        }

        @Override
        public Map<String, Object> queryGrowthOverview(Long tenantId, List<Long> deptIds) {
            lastTenantId = tenantId;
            lastDeptIds = deptIds;
            return growthResult;
        }

        @Override
        public Map<String, Object> queryPointsOverview(Long tenantId, List<Long> deptIds) {
            lastTenantId = tenantId;
            lastDeptIds = deptIds;
            return pointsResult;
        }

        @Override
        public List<Map<String, Object>> queryLevelDistribution(Long tenantId, List<Long> deptIds) {
            lastTenantId = tenantId;
            lastDeptIds = deptIds;
            return levelResult;
        }

        @Override
        public List<Map<String, Object>> querySegmentDistribution(Long tenantId, List<Long> deptIds) {
            lastTenantId = tenantId;
            lastDeptIds = deptIds;
            return segmentResult;
        }

        @Override
        public Map<String, Object> queryActivityOverview(Long tenantId, List<Long> deptIds) {
            lastTenantId = tenantId;
            lastDeptIds = deptIds;
            return activityResult;
        }

        @Override
        public Map<String, Object> queryFinanceOverview(Long tenantId, List<Long> deptIds) {
            lastTenantId = tenantId;
            lastDeptIds = deptIds;
            return financeResult;
        }
    }

    static class StubRemoteUserService implements RemoteUserService
    {
        List<Long> authorizedDepts = Collections.emptyList();

        @Override
        public R<java.util.List<SysDept>> getUserDeptList(String username, String source)
        {
            List<SysDept> depts = new ArrayList<>();
            for (Long id : authorizedDepts) {
                SysDept d = new SysDept();
                d.setDeptId(id);
                depts.add(d);
            }
            return R.ok(depts);
        }

        @Override
        public R<LoginUser> getUserInfo(String username, String source)
        { return R.ok(new LoginUser()); }

        @Override
        public R<Boolean> registerUserInfo(
            com.junsong.system.api.domain.SysUser sysUser, String source)
        { return R.ok(true); }

        @Override
        public R<Boolean> recordUserLogin(
            com.junsong.system.api.domain.SysUser sysUser, String source)
        { return R.ok(true); }

        @Override
        public R<java.util.List<String>> listUsernamesByRoleKey(String roleKey, String source)
        { return R.ok(Collections.emptyList()); }
    }
}
