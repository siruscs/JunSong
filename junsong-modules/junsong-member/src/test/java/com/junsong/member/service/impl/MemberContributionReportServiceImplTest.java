package com.junsong.member.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.SecurityContextHolder;
import com.junsong.common.core.domain.R;
import com.junsong.member.domain.vo.*;
import com.junsong.member.mapper.MemberReportMapper;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MemberContributionReportServiceImplTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.remove();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = MemberReportServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setupAdmin() {
        SecurityContextHolder.setUserId("1");
        SecurityContextHolder.setUserName("admin");
        LoginUser lu = new LoginUser();
        lu.setUserid(1L); lu.setUsername("admin"); lu.setDeptId(100L);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, lu);
    }

    private static void setupNonAdmin(String username, Long deptId) {
        SecurityContextHolder.setUserId("2");
        SecurityContextHolder.setUserName(username);
        LoginUser lu = new LoginUser();
        lu.setUserid(2L); lu.setUsername(username); lu.setDeptId(deptId);
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, lu);
    }

    private static MemberReportQueryParams makeParams(List<Long> deptIds) {
        MemberReportQueryParams p = new MemberReportQueryParams();
        p.setDeptIds(deptIds);
        p.setStartTime(new Date());
        p.setEndTime(new Date());
        return p;
    }

    private MemberReportServiceImpl createService(FakeMemberReportMapper mapper, RemoteUserService remoteUserService) throws Exception {
        MemberReportServiceImpl svc = new MemberReportServiceImpl();
        setField(svc, "memberReportMapper", mapper);
        setField(svc, "remoteUserService", remoteUserService);
        return svc;
    }

    // ── Tests ──

    @Test
    void getContributionReport_memberCounts() throws Exception {
        setupAdmin();
        FakeMemberReportMapper mapper = new FakeMemberReportMapper();
        mapper.newMemberCount = 15;
        mapper.activeMemberCount = 120;
        mapper.repurchaseCount = 30;

        MemberReportServiceImpl svc = createService(mapper, new FakeRemoteUserService(Collections.emptyList()));

        MemberContributionReportVO vo = svc.getContributionReport(makeParams(List.of(100L)));

        assertEquals(15, vo.getNewMemberCount());
        assertEquals(120, vo.getActiveMemberCount());
        assertEquals(30, vo.getRepurchaseCount());
    }

    @Test
    void getContributionReport_memberAndNonMemberSales() throws Exception {
        setupAdmin();
        FakeMemberReportMapper mapper = new FakeMemberReportMapper();
        mapper.memberSalesTotal = new BigDecimal("8000.00");
        mapper.nonMemberSalesTotal = new BigDecimal("2000.00");

        MemberReportServiceImpl svc = createService(mapper, new FakeRemoteUserService(Collections.emptyList()));

        MemberContributionReportVO vo = svc.getContributionReport(makeParams(List.of(100L)));

        assertEquals(new BigDecimal("8000.00"), vo.getMemberSales());
        assertEquals(new BigDecimal("2000.00"), vo.getNonMemberSales());
        BigDecimal totalSales = vo.getMemberSales().add(vo.getNonMemberSales());
        assertEquals(new BigDecimal("10000.00"), totalSales);
        // memberSalesRatio = 8000/10000*100 = 80.00
        assertEquals(new BigDecimal("80.00"), vo.getMemberSalesRatio());
    }

    @Test
    void getContributionReport_pointsRedemptionCost() throws Exception {
        setupAdmin();
        FakeMemberReportMapper mapper = new FakeMemberReportMapper();
        mapper.pointsRedemptionCost = new BigDecimal("1500.00");

        MemberReportServiceImpl svc = createService(mapper, new FakeRemoteUserService(Collections.emptyList()));

        MemberContributionReportVO vo = svc.getContributionReport(makeParams(List.of(100L)));

        assertEquals(new BigDecimal("1500.00"), vo.getPointsRedemptionCost());
    }

    @Test
    void getContributionReport_seckillActivitySalesCostProfit() throws Exception {
        setupAdmin();
        FakeMemberReportMapper mapper = new FakeMemberReportMapper();
        mapper.seckillRevenue = new BigDecimal("3000.00");

        MemberReportServiceImpl svc = createService(mapper, new FakeRemoteUserService(Collections.emptyList()));

        MemberContributionReportVO vo = svc.getContributionReport(makeParams(List.of(100L)));

        assertEquals(new BigDecimal("3000.00"), vo.getSeckillSales());
        assertEquals(BigDecimal.ZERO, vo.getSeckillCost());
        assertEquals(new BigDecimal("3000.00"), vo.getSeckillProfit(),
                "seckillProfit = seckillSales - seckillCost = 3000 - 0 = 3000");
    }

    @Test
    void getContributionReport_nonAdminWithNoDepts_sentinelDeptId() throws Exception {
        // Non-admin with null deptId and empty authorized list
        SecurityContextHolder.setUserId("3");
        SecurityContextHolder.setUserName("no-dept-user");
        LoginUser lu = new LoginUser();
        lu.setUserid(3L); lu.setUsername("no-dept-user");
        SecurityContextHolder.set(SecurityConstants.LOGIN_USER, lu);

        FakeMemberReportMapper mapper = new FakeMemberReportMapper();
        MemberReportServiceImpl svc = createService(mapper, new FakeRemoteUserService(Collections.emptyList()));

        MemberContributionReportVO vo = svc.getContributionReport(makeParams(null));

        // sentinel -1L should be used
        assertEquals(Collections.singletonList(-1L), mapper.lastDeptIds);
        // counts should be 0 since no data matches sentinel
        assertEquals(0, vo.getNewMemberCount());
        assertEquals(0, vo.getActiveMemberCount());
    }

    @Test
    void getContributionReport_nonAdminFiltering() throws Exception {
        setupNonAdmin("store-mgr", 100L);
        FakeMemberReportMapper mapper = new FakeMemberReportMapper();
        MemberReportServiceImpl svc = createService(mapper, new FakeRemoteUserService(List.of(100L, 200L)));

        MemberReportQueryParams params = makeParams(List.of(100L, 200L, 999L));
        svc.getContributionReport(params);

        assertEquals(List.of(100L, 200L), params.getDeptIds());
    }

    @Test
    void getContributionReport_zeroSalesZeroRatio() throws Exception {
        setupAdmin();
        FakeMemberReportMapper mapper = new FakeMemberReportMapper();
        mapper.memberSalesTotal = BigDecimal.ZERO;
        mapper.nonMemberSalesTotal = BigDecimal.ZERO;

        MemberReportServiceImpl svc = createService(mapper, new FakeRemoteUserService(Collections.emptyList()));

        MemberContributionReportVO vo = svc.getContributionReport(makeParams(List.of(100L)));

        assertEquals(BigDecimal.ZERO, vo.getMemberSales());
        assertEquals(BigDecimal.ZERO, vo.getNonMemberSales());
        assertEquals(BigDecimal.ZERO, vo.getMemberSalesRatio(),
                "memberSalesRatio should be 0 when totalSales = 0 (no division by zero)");
    }

    @Test
    void getContributionReport_trendsAndActivitiesReturned() throws Exception {
        setupAdmin();
        FakeMemberReportMapper mapper = new FakeMemberReportMapper();

        MemberContributionTrendVO trend = new MemberContributionTrendVO();
        trend.setDateStr("2026-06-01");
        trend.setNewMemberCount(5);
        mapper.contributionTrends = List.of(trend);

        MemberActivityContributionVO activity = new MemberActivityContributionVO();
        activity.setActivityName("Flash Sale");
        activity.setParticipantCount(100);
        mapper.activityContributions = List.of(activity);

        MemberReportServiceImpl svc = createService(mapper, new FakeRemoteUserService(Collections.emptyList()));

        MemberContributionReportVO vo = svc.getContributionReport(makeParams(List.of(100L)));

        assertNotNull(vo.getTrends());
        assertEquals(1, vo.getTrends().size());
        assertNotNull(vo.getActivityContributions());
        assertEquals(1, vo.getActivityContributions().size());
        assertEquals("Flash Sale", vo.getActivityContributions().get(0).getActivityName());
    }

    // ── R2 Tests ──

    @Test
    void getContributionReport_memberSaleCountAndAvgAmount() throws Exception {
        setupAdmin();
        FakeMemberReportMapper mapper = new FakeMemberReportMapper();
        mapper.memberSalesTotal = new BigDecimal("12000.00");
        mapper.memberSaleCountVal = 40;

        MemberReportServiceImpl svc = createService(mapper, new FakeRemoteUserService(Collections.emptyList()));
        MemberContributionReportVO vo = svc.getContributionReport(makeParams(List.of(100L)));

        assertEquals(40, vo.getMemberSaleCount());
        assertEquals(new BigDecimal("300.00"), vo.getAvgMemberSaleAmount(),
                "avgMemberSaleAmount = 12000/40 = 300.00");
    }

    @Test
    void getContributionReport_firstPurchaseRate() throws Exception {
        setupAdmin();
        FakeMemberReportMapper mapper = new FakeMemberReportMapper();
        mapper.newMemberCount = 20;
        mapper.memberSaleCountVal = 15;

        MemberReportServiceImpl svc = createService(mapper, new FakeRemoteUserService(Collections.emptyList()));
        MemberContributionReportVO vo = svc.getContributionReport(makeParams(List.of(100L)));

        // firstPurchaseRate = 15/20 * 100 = 75.00
        assertEquals(new BigDecimal("75.00"), vo.getNewMemberFirstPurchaseRate());
    }

    @Test
    void getContributionReport_firstPurchaseRate_cappedAt100() throws Exception {
        setupAdmin();
        FakeMemberReportMapper mapper = new FakeMemberReportMapper();
        mapper.newMemberCount = 5;
        mapper.memberSaleCountVal = 20; // more sales than new members

        MemberReportServiceImpl svc = createService(mapper, new FakeRemoteUserService(Collections.emptyList()));
        MemberContributionReportVO vo = svc.getContributionReport(makeParams(List.of(100L)));

        // raw rate = 20/5 * 100 = 400, capped at 100
        assertEquals(0, new BigDecimal("100").compareTo(vo.getNewMemberFirstPurchaseRate()),
                "First purchase rate should be capped at 100");
    }

    @Test
    void getContributionReport_repeatPurchaseRate() throws Exception {
        setupAdmin();
        FakeMemberReportMapper mapper = new FakeMemberReportMapper();
        mapper.memberSaleCountVal = 50;
        mapper.repeatPurchaseCountVal = 10;

        MemberReportServiceImpl svc = createService(mapper, new FakeRemoteUserService(Collections.emptyList()));
        MemberContributionReportVO vo = svc.getContributionReport(makeParams(List.of(100L)));

        assertEquals(10, vo.getRepeatPurchaseCount());
        // repeatPurchaseRate = 10/50 * 100 = 20.00
        assertEquals(new BigDecimal("20.00"), vo.getRepeatPurchaseRate());
    }

    @Test
    void getContributionReport_dataNotePresent() throws Exception {
        setupAdmin();
        FakeMemberReportMapper mapper = new FakeMemberReportMapper();
        MemberReportServiceImpl svc = createService(mapper, new FakeRemoteUserService(Collections.emptyList()));
        MemberContributionReportVO vo = svc.getContributionReport(makeParams(List.of(100L)));

        assertNotNull(vo.getDataNote(), "dataNote should describe data model and limitations");
        assertTrue(vo.getDataNote().contains("remark"), "dataNote should mention remark-based identification");
        assertTrue(vo.getDataNote().contains("积分兑换成本"), "dataNote should mention points cost scope");
    }

    // ── Fakes ──

    static class FakeRemoteUserService implements RemoteUserService {
        private final List<Long> deptIds;
        FakeRemoteUserService(List<Long> deptIds) { this.deptIds = deptIds; }
        @Override public R<LoginUser> getUserInfo(String username, String source) { return null; }
        @Override public R<Boolean> registerUserInfo(com.junsong.system.api.domain.SysUser user, String source) { return null; }
        @Override public R<Boolean> recordUserLogin(com.junsong.system.api.domain.SysUser user, String source) { return null; }
        @Override public R<List<SysDept>> getUserDeptList(String username, String source) {
            List<SysDept> list = deptIds.stream().map(id -> {
                SysDept d = new SysDept(); d.setDeptId(id); d.setDeptName("Store" + id); return d;
            }).collect(Collectors.toList());
            return R.ok(list);
        }
        @Override public R<List<String>> listUsernamesByRoleKey(String roleKey, String source) { return null; }
    }

    static class FakeMemberReportMapper implements MemberReportMapper {
        int newMemberCount = 0;
        int activeMemberCount = 0;
        int repurchaseCount = 0;
        BigDecimal memberSalesTotal = BigDecimal.ZERO;
        BigDecimal nonMemberSalesTotal = BigDecimal.ZERO;
        BigDecimal pointsRedemptionCost = BigDecimal.ZERO;
        BigDecimal seckillRevenue = BigDecimal.ZERO;
        int memberSaleCountVal = 0;
        int repeatPurchaseCountVal = 0;
        List<MemberContributionTrendVO> contributionTrends = Collections.emptyList();
        List<MemberActivityContributionVO> activityContributions = Collections.emptyList();
        List<Long> lastDeptIds;

        @Override public int countTotalMembers(List<Long> deptIds) { this.lastDeptIds = deptIds; return 0; }
        @Override public int countTodayNewMembers(List<Long> deptIds) { this.lastDeptIds = deptIds; return newMemberCount; }
        @Override public int countActiveMembers(List<Long> deptIds) { this.lastDeptIds = deptIds; return activeMemberCount; }
        @Override public List<Map<String, Object>> selectMemberGrowthTrend(List<Long> d, String s, String e) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectMemberStatusStats(List<Long> d) { return Collections.emptyList(); }
        @Override public int countTotalSeckills(List<Long> d) { return 0; }
        @Override public int countSeckillParticipants(List<Long> d) { return 0; }
        @Override public BigDecimal sumSeckillRevenue(List<Long> deptIds) { this.lastDeptIds = deptIds; return seckillRevenue; }
        @Override public List<Map<String, Object>> selectSeckillStats(List<Long> d) { return Collections.emptyList(); }
        @Override public List<Map<String, Object>> selectSeckillDeptStats(List<Long> d) { return Collections.emptyList(); }
        @Override public BigDecimal sumMemberSales(List<Long> deptIds, String s, String e) { this.lastDeptIds = deptIds; return memberSalesTotal; }
        @Override public BigDecimal sumNonMemberSales(List<Long> deptIds, String s, String e) { this.lastDeptIds = deptIds; return nonMemberSalesTotal; }
        @Override public List<MemberContributionTrendVO> selectContributionTrend(List<Long> deptIds, String s, String e) { return contributionTrends; }
        @Override public List<MemberActivityContributionVO> selectActivityContributions(List<Long> deptIds) { return activityContributions; }
        @Override public BigDecimal sumPointsRedemptionCost(List<Long> deptIds) { return pointsRedemptionCost; }
        @Override public int countRepurchaseMembers(List<Long> deptIds) { return repurchaseCount; }
        @Override public int countMemberSaleRecords(List<Long> d, String s, String e) { return memberSaleCountVal; }
        @Override public int countMembersWithMultipleSales(List<Long> d, String s, String e) { return repeatPurchaseCountVal; }
    }
}
