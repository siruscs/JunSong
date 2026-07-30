package com.junsong.member.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.member.domain.vo.MemberReportQueryParams;
import com.junsong.member.domain.vo.MemberReportVO;
import com.junsong.member.domain.vo.SeckillReportVO;
import com.junsong.member.mapper.MemberReportMapper;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MemberReportServiceImpl 单元测试
 * <p>
 * 使用手写 fake 替代 Mockito，避免 inline mock maker 在 JDK 26+ 下
 * 自附加 Java agent 导致的兼容性问题。
 */
class MemberReportServiceImplTest {

    private MemberReportServiceImpl service;
    private FakeMemberReportMapper fakeMapper;
    private FakeRemoteUserService fakeRemoteUserService;

    @BeforeEach
    void setUp() throws Exception {
        service = new MemberReportServiceImpl();
        fakeMapper = new FakeMemberReportMapper();
        fakeRemoteUserService = new FakeRemoteUserService();

        setField(service, "memberReportMapper", fakeMapper);
        setField(service, "remoteUserService", fakeRemoteUserService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.remove();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = MemberReportServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ── 辅助方法：设置安全上下文 ──

    private void setupAdmin() {
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(1L);
        loginUser.setUsername("admin");
        loginUser.setDeptId(100L);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    private void setupNonAdmin(String username, Long deptId) {
        SecurityContextHolder.setUserId("999");
        SecurityContextHolder.setUserName(username);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserid(999L);
        loginUser.setUsername(username);
        loginUser.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, loginUser);
    }

    // ── Step 1：非 admin 无授权时使用哨兵 deptIds = [-1L] ──

    @Test
    void nonAdmin_noDeptAuthorization_usesSentinelDeptIds() {
        setupNonAdmin("normaluser", null);
        fakeRemoteUserService.deptListResponse = R.ok(Collections.emptyList());

        MemberReportQueryParams params = new MemberReportQueryParams();
        service.getMemberReport(params);

        assertEquals(Collections.singletonList(-1L), fakeMapper.lastDeptIds,
            "非 admin 且无授权部门时 deptIds 应为 [-1L]，防止数据泄露");
    }

    @Test
    void nonAdmin_remoteServiceFails_usesSentinelDeptIds() {
        setupNonAdmin("normaluser", null);
        fakeRemoteUserService.deptListResponse = R.fail();

        MemberReportQueryParams params = new MemberReportQueryParams();
        service.getMemberReport(params);

        assertEquals(Collections.singletonList(-1L), fakeMapper.lastDeptIds,
            "RemoteUserService 调用失败且用户无 deptId 时应使用哨兵 [-1L]");
    }

    @Test
    void nonAdmin_withCurrentDeptId_usesDeptIdWhenRemoteEmpty() {
        setupNonAdmin("normaluser", 42L);
        fakeRemoteUserService.deptListResponse = R.ok(Collections.emptyList());

        MemberReportQueryParams params = new MemberReportQueryParams();
        service.getMemberReport(params);

        assertEquals(Collections.singletonList(42L), fakeMapper.lastDeptIds,
            "RemoteUserService 返回空时回落到当前用户 deptId");
    }

    @Test
    void nonAdmin_withAuthorizedDepts_filtersRequestedDepts() {
        setupNonAdmin("normaluser", 10L);
        List<SysDept> allowedDepts = Arrays.asList(makeDept(10L), makeDept(20L));
        fakeRemoteUserService.deptListResponse = R.ok(allowedDepts);

        MemberReportQueryParams params = new MemberReportQueryParams();
        params.setDeptIds(Arrays.asList(10L, 30L)); // 30L 不在授权列表中
        service.getMemberReport(params);

        assertEquals(Collections.singletonList(10L), fakeMapper.lastDeptIds,
            "请求的 30L 不在授权列表中，应被过滤，仅保留 10L");
    }

    @Test
    void nonAdmin_allRequestedDeptsUnauthorized_fallsBackToAllowed() {
        setupNonAdmin("normaluser", 10L);
        List<SysDept> allowedDepts = Collections.singletonList(makeDept(10L));
        fakeRemoteUserService.deptListResponse = R.ok(allowedDepts);

        MemberReportQueryParams params = new MemberReportQueryParams();
        params.setDeptIds(Arrays.asList(80L, 90L)); // 全部不在授权列表
        service.getMemberReport(params);

        assertEquals(Collections.singletonList(10L), fakeMapper.lastDeptIds,
            "请求部门全部未授权时回落到授权部门列表");
    }

    @Test
    void nonAdmin_noRequestedDepts_usesFullAllowedList() {
        setupNonAdmin("normaluser", 10L);
        List<SysDept> allowedDepts = Arrays.asList(makeDept(10L), makeDept(20L));
        fakeRemoteUserService.deptListResponse = R.ok(allowedDepts);

        MemberReportQueryParams params = new MemberReportQueryParams();
        service.getMemberReport(params);

        assertEquals(Arrays.asList(10L, 20L), fakeMapper.lastDeptIds,
            "未指定请求部门时使用完整授权列表");
    }

    // ── Step 2：报表口径——VO 必须原样组装，不得返回全 0 ──

    @Test
    void getMemberReport_assemblesAllMetricsCorrectly() {
        setupAdmin();
        fakeMapper.totalMembers = 150;
        fakeMapper.todayNewMembers = 5;
        fakeMapper.activeMembers = 80;

        List<Map<String, Object>> growthStats = new ArrayList<>();
        Map<String, Object> day1 = new HashMap<>();
        day1.put("dateStr", "2026-06-25");
        day1.put("newCount", 3);
        growthStats.add(day1);
        Map<String, Object> day2 = new HashMap<>();
        day2.put("dateStr", "2026-06-26");
        day2.put("newCount", 7);
        growthStats.add(day2);
        fakeMapper.growthStats = growthStats;

        List<Map<String, Object>> statusStats = new ArrayList<>();
        Map<String, Object> normal = new HashMap<>();
        normal.put("statusName", "正常");
        normal.put("memberCount", 120);
        statusStats.add(normal);
        Map<String, Object> inactive = new HashMap<>();
        inactive.put("statusName", "无效");
        inactive.put("memberCount", 30);
        statusStats.add(inactive);
        fakeMapper.statusStats = statusStats;

        MemberReportQueryParams params = new MemberReportQueryParams();
        MemberReportVO vo = service.getMemberReport(params);

        assertEquals(150, vo.getTotalMemberCount(), "会员总数应为 150");
        assertEquals(5, vo.getTodayNewMemberCount(), "今日新增应为 5");
        assertEquals(80, vo.getActiveMemberCount(), "活跃会员应为 80");
        assertNotNull(vo.getMemberGrowthStats(), "增长趋势不应为 null");
        assertEquals(2, vo.getMemberGrowthStats().size(), "增长趋势应有 2 天");
        assertEquals("2026-06-25", vo.getMemberGrowthStats().get(0).get("dateStr"));
        assertEquals(3, vo.getMemberGrowthStats().get(0).get("newCount"));
        assertNotNull(vo.getMemberTypeStats(), "状态分布不应为 null");
        assertEquals(2, vo.getMemberTypeStats().size(), "状态分布应有 2 种状态");
        assertEquals("正常", vo.getMemberTypeStats().get(0).get("statusName"));
    }

    @Test
    void getMemberReport_mapperReturnsZeros_voReflectsZeros() {
        setupAdmin();
        fakeMapper.totalMembers = 0;
        fakeMapper.todayNewMembers = 0;
        fakeMapper.activeMembers = 0;
        fakeMapper.growthStats = Collections.emptyList();
        fakeMapper.statusStats = Collections.emptyList();

        MemberReportQueryParams params = new MemberReportQueryParams();
        MemberReportVO vo = service.getMemberReport(params);

        assertEquals(0, vo.getTotalMemberCount(), "会员总数应为 0");
        assertEquals(0, vo.getTodayNewMemberCount(), "今日新增应为 0");
        assertEquals(0, vo.getActiveMemberCount(), "活跃会员应为 0");
        assertTrue(vo.getMemberGrowthStats().isEmpty(), "增长趋势应为空列表");
        assertTrue(vo.getMemberTypeStats().isEmpty(), "状态分布应为空列表");
    }

    @Test
    void getSeckillReport_assemblesAllMetricsCorrectly() {
        setupAdmin();
        fakeMapper.totalSeckills = 10;
        fakeMapper.seckillParticipants = 200;
        fakeMapper.seckillRevenue = new BigDecimal("50000.00");

        List<Map<String, Object>> seckillStats = new ArrayList<>();
        Map<String, Object> stat = new HashMap<>();
        stat.put("seckillName", "双十一秒杀");
        stat.put("participantCount", 100);
        stat.put("revenue", new BigDecimal("30000.00"));
        seckillStats.add(stat);
        fakeMapper.seckillStats = seckillStats;

        List<Map<String, Object>> deptStats = new ArrayList<>();
        Map<String, Object> deptStat = new HashMap<>();
        deptStat.put("deptId", 1L);
        deptStat.put("deptName", "总部");
        deptStat.put("participantCount", 50);
        deptStat.put("revenue", new BigDecimal("15000.00"));
        deptStats.add(deptStat);
        fakeMapper.seckillDeptStats = deptStats;

        MemberReportQueryParams params = new MemberReportQueryParams();
        SeckillReportVO vo = service.getSeckillReport(params);

        assertEquals(10, vo.getTotalSeckillCount(), "秒杀总场次应为 10");
        assertEquals(200, vo.getTotalParticipantCount(), "参与人数应为 200");
        assertEquals(new BigDecimal("50000.00"), vo.getTotalRevenue(), "总收入应为 50000.00");
        assertEquals(1, vo.getSeckillStats().size(), "秒杀活动统计应有 1 条");
        assertEquals("双十一秒杀", vo.getSeckillStats().get(0).get("seckillName"));
        assertEquals(1, vo.getSeckillDeptStats().size(), "部门统计应有 1 条");
        assertEquals("总部", vo.getSeckillDeptStats().get(0).get("deptName"));
    }

    @Test
    void getSeckillReport_alsoAppliesDataScope() {
        setupNonAdmin("normaluser", null);
        fakeRemoteUserService.deptListResponse = R.ok(Collections.emptyList());

        MemberReportQueryParams params = new MemberReportQueryParams();
        service.getSeckillReport(params);

        assertEquals(Collections.singletonList(-1L), fakeMapper.lastDeptIds,
            "秒杀报表也应执行数据权限过滤");
    }

    // ── 辅助 ──

    private static SysDept makeDept(Long deptId) {
        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        return dept;
    }

    // ── Fake 实现 ──

    /**
     * 记录型 fake mapper：捕获最后一次调用的 deptIds 并返回可配置的值。
     */
    static class FakeMemberReportMapper implements MemberReportMapper {
        List<Long> lastDeptIds;
        String lastStartTime;
        String lastEndTime;

        int totalMembers = 0;
        int todayNewMembers = 0;
        int activeMembers = 0;
        List<Map<String, Object>> growthStats = Collections.emptyList();
        List<Map<String, Object>> statusStats = Collections.emptyList();

        int totalSeckills = 0;
        int seckillParticipants = 0;
        BigDecimal seckillRevenue = BigDecimal.ZERO;
        List<Map<String, Object>> seckillStats = Collections.emptyList();
        List<Map<String, Object>> seckillDeptStats = Collections.emptyList();

        @Override
        public int countTotalMembers(List<Long> deptIds) {
            this.lastDeptIds = deptIds;
            return totalMembers;
        }

        @Override
        public int countTodayNewMembers(List<Long> deptIds) {
            this.lastDeptIds = deptIds;
            return todayNewMembers;
        }

        @Override
        public int countActiveMembers(List<Long> deptIds) {
            this.lastDeptIds = deptIds;
            return activeMembers;
        }

        @Override
        public List<Map<String, Object>> selectMemberGrowthTrend(List<Long> deptIds, String startTime, String endTime) {
            this.lastDeptIds = deptIds;
            this.lastStartTime = startTime;
            this.lastEndTime = endTime;
            return growthStats;
        }

        @Override
        public List<Map<String, Object>> selectMemberStatusStats(List<Long> deptIds) {
            this.lastDeptIds = deptIds;
            return statusStats;
        }

        @Override
        public int countTotalSeckills(List<Long> deptIds) {
            this.lastDeptIds = deptIds;
            return totalSeckills;
        }

        @Override
        public int countSeckillParticipants(List<Long> deptIds) {
            this.lastDeptIds = deptIds;
            return seckillParticipants;
        }

        @Override
        public BigDecimal sumSeckillRevenue(List<Long> deptIds) {
            this.lastDeptIds = deptIds;
            return seckillRevenue;
        }

        @Override
        public List<Map<String, Object>> selectSeckillStats(List<Long> deptIds) {
            this.lastDeptIds = deptIds;
            return seckillStats;
        }

        @Override
        public List<Map<String, Object>> selectSeckillDeptStats(List<Long> deptIds) {
            this.lastDeptIds = deptIds;
            return seckillDeptStats;
        }

        @Override public BigDecimal sumMemberSales(List<Long> deptIds, String s, String e) { return BigDecimal.ZERO; }
        @Override public BigDecimal sumNonMemberSales(List<Long> deptIds, String s, String e) { return BigDecimal.ZERO; }
        @Override public List<com.junsong.member.domain.vo.MemberContributionTrendVO> selectContributionTrend(List<Long> deptIds, String s, String e) { return Collections.emptyList(); }
        @Override public List<com.junsong.member.domain.vo.MemberActivityContributionVO> selectActivityContributions(List<Long> deptIds) { return Collections.emptyList(); }
        @Override public BigDecimal sumPointsRedemptionCost(List<Long> deptIds) { return BigDecimal.ZERO; }
        @Override public int countRepurchaseMembers(List<Long> deptIds) { return 0; }
        @Override public int countMemberSaleRecords(List<Long> d, String s, String e) { return 0; }
        @Override public int countMembersWithMultipleSales(List<Long> d, String s, String e) { return 0; }
    }

    /**
     * 记录型 fake RemoteUserService：可配置 deptList 响应和捕获 username。
     */
    static class FakeRemoteUserService implements RemoteUserService {
        R<List<SysDept>> deptListResponse = R.ok(Collections.emptyList());
        String lastUsername;

        @Override
        public R<LoginUser> getUserInfo(String username, String source) {
            return R.fail();
        }

        @Override
        public R<LoginUser> getUserInfoById(Long userId, String source) {
            return R.fail();
        }

        @Override
        public R<Boolean> registerUserInfo(SysUser sysUser, String source) {
            return R.fail();
        }

        @Override
        public R<Boolean> recordUserLogin(SysUser sysUser, String source) {
            return R.fail();
        }

        @Override
        public R<List<SysDept>> getUserDeptList(String username, String source) {
            this.lastUsername = username;
            return deptListResponse;
        }

        @Override
        public R<Boolean> isWechatLoginEnabled(Long tenantId, String source) {
            return R.ok(false);
        }

        @Override
        public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) {
            return R.ok(Collections.emptyList());
        }
    }
}
