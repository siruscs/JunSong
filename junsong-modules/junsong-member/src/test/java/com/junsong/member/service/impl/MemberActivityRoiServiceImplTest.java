package com.junsong.member.service.impl;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.member.domain.vo.MemberActivityRoiVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemberActivityRoiServiceImpl 单元测试
 *
 * 使用手写 fake（子类覆盖 package-private 查询方法），不使用 Mockito。
 */
class MemberActivityRoiServiceImplTest {

    private TestableMemberActivityRoiServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TestableMemberActivityRoiServiceImpl();
    }

    // ── Test 1: No activities → empty list ──

    @Test
    void noActivities_returnsEmptyList() {
        service.activities = Collections.emptyList();

        List<MemberActivityRoiVO> result = service.getActivityRoiList(null, null);

        assertNotNull(result, "结果不应为 null");
        assertTrue(result.isEmpty(), "无活动时应返回空列表");
    }

    // ── Test 2: Activity with sales → correct totalSalesAmount ──

    @Test
    void activityWithSales_correctTotalSalesAmount() {
        Long seckillId = 100L;
        service.activities = Collections.singletonList(
                makeActivity(seckillId, "春季秒杀", 10L, "2026-03-01", "2026-03-31",
                        100, 20, "0")
        );
        service.salesStatsMap.put(seckillId, makeSalesStats(new BigDecimal("15000.00"), 30));

        List<MemberActivityRoiVO> result = service.getActivityRoiList(null, null);

        assertEquals(1, result.size(), "应返回 1 条活动");
        MemberActivityRoiVO vo = result.get(0);
        assertEquals(seckillId, vo.getActivityId());
        assertEquals("春季秒杀", vo.getActivityName());
        assertEquals("SECKILL", vo.getActivityType());
        assertEquals(new BigDecimal("15000.00"), vo.getTotalSalesAmount(), "销售总额应为 15000.00");
        assertEquals(30, vo.getTotalOrders(), "订单数应为 30");
    }

    // ── Test 3: New customer count uses join_date correctly ──

    @Test
    void newCustomerCount_usesJoinDateCorrectly() {
        Long seckillId = 200L;
        Long deptId = 10L;
        service.activities = Collections.singletonList(
                makeActivity(seckillId, "夏季活动", deptId, "2026-06-01", "2026-06-30",
                        50, 10, "1")
        );
        service.salesStatsMap.put(seckillId, makeSalesStats(BigDecimal.ZERO, 0));
        service.newCustomerCountMap.put(deptId, 15);

        List<MemberActivityRoiVO> result = service.getActivityRoiList(deptId, null);

        assertEquals(1, result.size());
        MemberActivityRoiVO vo = result.get(0);
        assertEquals(15, vo.getNewCustomerCount(), "新会员数应为 15（基于 join_date 统计）");
        // Verify that the query was called with the correct dept and date range
        assertEquals(deptId, service.lastNewCustomerDeptId, "查询新会员时应使用活动的 dept_id");
        assertNotNull(service.lastNewCustomerStart, "查询新会员时开始日期不应为 null");
        assertNotNull(service.lastNewCustomerEnd, "查询新会员时结束日期不应为 null");
    }

    // ── Test 4: Discount cost unavailable → discountCostStatus = "UNAVAILABLE" ──

    @Test
    void discountCostUnavailable_statusIsUnavailable() {
        Long seckillId = 300L;
        service.activities = Collections.singletonList(
                makeActivity(seckillId, "秋季秒杀", 10L, "2026-09-01", "2026-09-30",
                        80, 30, "0")
        );
        service.salesStatsMap.put(seckillId, makeSalesStats(new BigDecimal("8000.00"), 20));

        List<MemberActivityRoiVO> result = service.getActivityRoiList(null, null);

        assertEquals(1, result.size());
        MemberActivityRoiVO vo = result.get(0);
        assertEquals("UNAVAILABLE", vo.getDiscountCostStatus(),
                "折扣成本不可用时状态应为 UNAVAILABLE");
        assertNull(vo.getDiscountCost(), "折扣成本不可用时值应为 null");
    }

    // ── Test 5: ROI unavailable when cost unavailable → roiStatus = "UNAVAILABLE" ──

    @Test
    void roiUnavailableWhenCostUnavailable_statusIsUnavailable() {
        Long seckillId = 400L;
        service.activities = Collections.singletonList(
                makeActivity(seckillId, "冬季秒杀", 10L, "2026-12-01", "2026-12-31",
                        200, 50, "1")
        );
        service.salesStatsMap.put(seckillId, makeSalesStats(new BigDecimal("50000.00"), 100));
        // cost not set in costMap → null → MISSING_ACTIVITY_COST

        List<MemberActivityRoiVO> result = service.getActivityRoiList(null, null);

        assertEquals(1, result.size());
        MemberActivityRoiVO vo = result.get(0);
        assertEquals("UNAVAILABLE", vo.getRoiStatus(),
                "成本不可用时 ROI 状态应为 UNAVAILABLE");
        assertNull(vo.getRoi(), "成本不可用时 ROI 值应为 null");
        assertEquals("MISSING_ACTIVITY_COST", vo.getUnavailableReason(),
                "成本缺失时原因应为 MISSING_ACTIVITY_COST");
        assertNotNull(vo.getSuggestion(), "成本缺失时应给出操作建议文案");
        assertTrue(vo.getSuggestion().contains("成本"),
                "成本缺失建议文案应说明补充活动成本，实际: " + vo.getSuggestion());
        // Even though sales are large, ROI should not be computed without cost
        assertEquals(new BigDecimal("50000.00"), vo.getTotalSalesAmount(),
                "销售额应正常计算");
    }

    // ── R5-D: ROI READY when cost and sales both available ──

    @Test
    void roiReadyWhenCostAndSalesAvailable() {
        Long seckillId = 700L;
        service.activities = Collections.singletonList(
                makeActivity(seckillId, "ROI 可算活动", 10L, "2026-04-01", "2026-04-30",
                        100, 10, "1")
        );
        service.salesStatsMap.put(seckillId, makeSalesStats(new BigDecimal("20000.00"), 50));
        service.costMap.put(seckillId, new BigDecimal("5000.00"));
        service.participantCountMap.put(seckillId, 30);
        service.firstPurchaseMap.put(seckillId, 8);
        service.repurchaseMap.put(seckillId, 22);

        List<MemberActivityRoiVO> result = service.getActivityRoiList(null, null);

        assertEquals(1, result.size());
        MemberActivityRoiVO vo = result.get(0);
        assertEquals("READY", vo.getRoiStatus(), "有成本和销售时 ROI 应为 READY");
        // roi = (20000 - 5000) / 5000 * 100 = 300.00
        assertEquals(new BigDecimal("300.00"), vo.getRoi(), "ROI 应为 300.00%");
        assertEquals(new BigDecimal("15000.00"), vo.getGrossProfitAmount(), "毛利应为 15000.00");
        assertEquals(new BigDecimal("5000.00"), vo.getActivityCostAmount(), "活动成本应为 5000.00");
        assertEquals(new BigDecimal("20000.00"), vo.getRelatedSalesAmount(), "关联销售应为 20000.00");
        assertEquals(30L, vo.getParticipantCount(), "参与人数应为 30");
        assertEquals(8L, vo.getFirstPurchaseMemberCount(), "首购会员应为 8");
        assertEquals(22L, vo.getRepurchaseMemberCount(), "复购会员应为 22");
        assertNull(vo.getUnavailableReason(), "READY 时原因为 null");
        assertNull(vo.getSuggestion(), "READY 时不应有不可算建议文案");
    }

    // ── R5-D: No related sales → UNAVAILABLE/NO_RELATED_SALES ──

    @Test
    void roiUnavailableWhenNoRelatedSales() {
        Long seckillId = 800L;
        service.activities = Collections.singletonList(
                makeActivity(seckillId, "零销售活动", 10L, "2026-05-01", "2026-05-31",
                        100, 100, "0")
        );
        service.salesStatsMap.put(seckillId, makeSalesStats(BigDecimal.ZERO, 0));
        service.costMap.put(seckillId, new BigDecimal("3000.00"));

        List<MemberActivityRoiVO> result = service.getActivityRoiList(null, null);

        assertEquals(1, result.size());
        MemberActivityRoiVO vo = result.get(0);
        assertEquals("UNAVAILABLE", vo.getRoiStatus(), "无关联销售时 ROI 应为 UNAVAILABLE");
        assertEquals("NO_RELATED_SALES", vo.getUnavailableReason(),
                "无销售时原因应为 NO_RELATED_SALES");
        assertNull(vo.getRoi(), "无销售时 ROI 值应为 null");
    }

    // ── Additional: Sell through rate calculation ──

    @Test
    void sellThroughRate_calculatedCorrectly() {
        Long seckillId = 500L;
        // total_shares=100, remain_shares=25 → sold 75 → rate = 75%
        service.activities = Collections.singletonList(
                makeActivity(seckillId, "高售罄率活动", 10L, "2026-01-01", "2026-01-31",
                        100, 25, "1")
        );
        service.salesStatsMap.put(seckillId, makeSalesStats(new BigDecimal("1000.00"), 10));

        List<MemberActivityRoiVO> result = service.getActivityRoiList(null, null);

        assertEquals(1, result.size());
        MemberActivityRoiVO vo = result.get(0);
        assertEquals(new BigDecimal("75.00"), vo.getSellThroughRate(),
                "售罄率应为 75.00%（(100-25)/100*100）");
    }

    @Test
    void sellThroughRate_zeroTotalShares_returnsZero() {
        Long seckillId = 600L;
        service.activities = Collections.singletonList(
                makeActivity(seckillId, "零份额活动", 10L, "2026-02-01", "2026-02-28",
                        0, 0, "0")
        );
        service.salesStatsMap.put(seckillId, makeSalesStats(BigDecimal.ZERO, 0));

        List<MemberActivityRoiVO> result = service.getActivityRoiList(null, null);

        assertEquals(1, result.size());
        MemberActivityRoiVO vo = result.get(0);
        assertEquals(BigDecimal.ZERO, vo.getSellThroughRate(),
                "总份额为 0 时售罄率应为 0");
    }

    // ── Authorization: dept filtering ──

    @Test
    void getActivityRoiList_withoutDeptId_usesOnlyAuthorizedDeptIds() {
        // Non-admin user with authorized depts [100, 200]
        service.allowedDeptIds = Arrays.asList(100L, 200L);

        Long seckill1 = 1001L;
        Long seckill2 = 1002L;
        Long seckill3 = 1003L;
        service.activities = Arrays.asList(
                makeActivity(seckill1, "活动A", 100L, "2026-03-01", "2026-03-31",
                        100, 20, "0"),
                makeActivity(seckill2, "活动B", 200L, "2026-03-01", "2026-03-31",
                        80, 30, "0"),
                makeActivity(seckill3, "活动C", 300L, "2026-03-01", "2026-03-31",
                        50, 10, "0")
        );
        service.salesStatsMap.put(seckill1, makeSalesStats(new BigDecimal("5000"), 10));
        service.salesStatsMap.put(seckill2, makeSalesStats(new BigDecimal("3000"), 8));
        service.salesStatsMap.put(seckill3, makeSalesStats(new BigDecimal("9000"), 20));

        List<MemberActivityRoiVO> result = service.getActivityRoiList(null, null);

        // Verify: SQL uses dept_id IN (100, 200), NOT unbounded query
        assertNotNull(service.lastQueryDeptIds, "deptIds 不应为 null");
        assertEquals(2, service.lastQueryDeptIds.size(), "应仅包含授权的部门 ID");
        assertTrue(service.lastQueryDeptIds.contains(100L), "应包含部门 100");
        assertTrue(service.lastQueryDeptIds.contains(200L), "应包含部门 200");

        // Only activities from dept 100 and 200 should be returned
        assertEquals(2, result.size(), "应仅返回授权部门的活动");
        Set<Long> returnedIds = new HashSet<>();
        for (MemberActivityRoiVO vo : result) {
            returnedIds.add(vo.getActivityId());
        }
        assertTrue(returnedIds.contains(seckill1));
        assertTrue(returnedIds.contains(seckill2));
        assertFalse(returnedIds.contains(seckill3), "部门 300 的活动不应返回");
    }

    @Test
    void getActivityRoiList_rejectsUnauthorizedDeptId() {
        // Non-admin user with authorized depts [100]
        service.allowedDeptIds = Collections.singletonList(100L);

        // Call getActivityRoiList(999L, null) — dept 999 is NOT authorized
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.getActivityRoiList(999L, null),
                "访问未授权部门应抛出 ServiceException");
        assertTrue(ex.getMessage().contains("无权"),
                "异常消息应包含'无权'，实际: " + ex.getMessage());
    }

    // ── Helper methods ──

    private Map<String, Object> makeActivity(Long seckillId, String name, Long deptId,
                                              String startDate, String endDate,
                                              int totalShares, int remainShares, String status) {
        Map<String, Object> map = new HashMap<>();
        map.put("seckill_id", seckillId);
        map.put("seckill_name", name);
        map.put("dept_id", deptId);
        map.put("seckill_date", parseDate(startDate));
        map.put("end_date", parseDate(endDate));
        map.put("total_shares", totalShares);
        map.put("remain_shares", remainShares);
        map.put("status", status);
        return map;
    }

    private Map<String, Object> makeSalesStats(BigDecimal totalSales, int orderCount) {
        Map<String, Object> map = new HashMap<>();
        map.put("total_sales", totalSales);
        map.put("order_count", orderCount);
        return map;
    }

    private Date parseDate(String dateStr) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    // ── Testable subclass (hand-written fake) ──

    /**
     * 覆盖 MemberActivityRoiServiceImpl 的 package-private 查询方法，
     * 返回可配置的测试数据，无需真实数据库。
     */
    static class TestableMemberActivityRoiServiceImpl extends MemberActivityRoiServiceImpl {
        List<Map<String, Object>> activities = Collections.emptyList();
        Map<Long, Map<String, Object>> salesStatsMap = new HashMap<>();
        Map<Long, Integer> newCustomerCountMap = new HashMap<>();
        Map<Long, BigDecimal> costMap = new HashMap<>();
        Map<Long, Integer> participantCountMap = new HashMap<>();
        Map<Long, Integer> firstPurchaseMap = new HashMap<>();
        Map<Long, Integer> repurchaseMap = new HashMap<>();

        // Authorization: allowedDeptIds defaults to empty (admin = no restriction)
        List<Long> allowedDeptIds = Collections.emptyList();

        // Captured parameters for assertion
        Long lastNewCustomerDeptId;
        Date lastNewCustomerStart;
        Date lastNewCustomerEnd;
        List<Long> lastQueryDeptIds;
        Long lastQueryActivityId;

        @Override
        List<Long> loadAllowedDeptIds() {
            return allowedDeptIds;
        }

        @Override
        List<Map<String, Object>> queryActivities(List<Long> deptIds, Long activityId) {
            this.lastQueryDeptIds = deptIds;
            this.lastQueryActivityId = activityId;

            List<Map<String, Object>> filtered = new ArrayList<>();
            for (Map<String, Object> a : activities) {
                boolean deptMatch = (deptIds == null || deptIds.isEmpty())
                        || deptIds.contains(a.get("dept_id"));
                boolean activityMatch = (activityId == null)
                        || activityId.equals(a.get("seckill_id"));
                if (deptMatch && activityMatch) {
                    filtered.add(a);
                }
            }
            return filtered;
        }

        @Override
        Map<String, Object> querySalesStats(Long seckillId) {
            Map<String, Object> stats = salesStatsMap.get(seckillId);
            return stats != null ? stats : Collections.emptyMap();
        }

        @Override
        int queryNewCustomers(Long deptId, Date startTime, Date endTime) {
            this.lastNewCustomerDeptId = deptId;
            this.lastNewCustomerStart = startTime;
            this.lastNewCustomerEnd = endTime;
            Integer count = newCustomerCountMap.get(deptId);
            return count != null ? count : 0;
        }

        @Override
        BigDecimal queryActivityCost(Long seckillId) {
            return costMap.get(seckillId);
        }

        @Override
        int queryParticipantCount(Long seckillId) {
            return participantCountMap.getOrDefault(seckillId, 0);
        }

        @Override
        int queryFirstPurchaseCount(Long seckillId, Date activityStart) {
            return firstPurchaseMap.getOrDefault(seckillId, 0);
        }

        @Override
        int queryRepurchaseCount(Long seckillId, Date activityStart) {
            return repurchaseMap.getOrDefault(seckillId, 0);
        }
    }
}
