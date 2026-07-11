package com.junsong.member.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.vo.MemberActivityRoiVO;
import com.junsong.member.service.IMemberActivityRoiService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 会员活动 ROI 服务实现
 * 使用 JdbcTemplate 直接查询，避免为一次性报表创建额外 Mapper
 */
@Service
public class MemberActivityRoiServiceImpl implements IMemberActivityRoiService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RemoteUserService remoteUserService;

    @Override
    public List<MemberActivityRoiVO> getActivityRoiList(Long deptId, Long activityId) {
        List<Long> deptIds = resolveDeptIds(deptId);
        List<Map<String, Object>> activities = queryActivities(deptIds, activityId);
        if (activities == null || activities.isEmpty()) {
            return Collections.emptyList();
        }

        List<MemberActivityRoiVO> result = new ArrayList<>();
        for (Map<String, Object> activity : activities) {
            MemberActivityRoiVO vo = buildRoiVO(activity);
            result.add(vo);
        }
        return result;
    }

    // ── Authorization helpers (package-private for testability) ──

    /**
     * Load the list of department IDs the current user is allowed to view.
     * - Admin: returns empty list (no restriction)
     * - Non-admin with authorized depts: returns the dept IDs
     * - Non-admin with no authorized depts but has current dept: returns [currentDeptId]
     * - Non-admin with nothing: returns [-1L] (sentinel that matches no rows)
     */
    List<Long> loadAllowedDeptIds() {
        if (SecurityUtils.isAdmin()) {
            return Collections.emptyList();
        }
        try {
            R<List<SysDept>> result = remoteUserService.getUserDeptList(
                    SecurityUtils.getUsername(), SecurityConstants.INNER);
            if (R.isSuccess(result) && result.getData() != null && !result.getData().isEmpty()) {
                return result.getData().stream()
                        .map(SysDept::getDeptId)
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {
            // remote call failed, fall through
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId != null) {
            return Collections.singletonList(deptId);
        }
        return Collections.singletonList(-1L);
    }

    /**
     * Resolve the requested department ID against the user's allowed set.
     * - Admin: returns [deptId] if specified, or empty list (no restriction)
     * - Non-admin without deptId: returns all allowed dept IDs
     * - Non-admin with deptId in allowed set: returns [deptId]
     * - Non-admin with deptId NOT in allowed set: throws ServiceException
     */
    List<Long> resolveDeptIds(Long deptId) {
        List<Long> allowedDeptIds = loadAllowedDeptIds();
        if (allowedDeptIds.isEmpty()) {
            // admin: no restriction
            if (deptId != null) {
                return Collections.singletonList(deptId);
            }
            return Collections.emptyList();
        }
        // non-admin
        if (deptId == null) {
            return allowedDeptIds;
        }
        if (!allowedDeptIds.contains(deptId)) {
            throw new ServiceException("无权访问该部门数据");
        }
        return Collections.singletonList(deptId);
    }

    private MemberActivityRoiVO buildRoiVO(Map<String, Object> activity) {
        MemberActivityRoiVO vo = new MemberActivityRoiVO();

        Long seckillId = toLong(activity.get("seckill_id"));
        vo.setActivityId(seckillId);
        vo.setActivityName((String) activity.get("seckill_name"));
        vo.setActivityType("SECKILL");
        vo.setStartTime(toDate(activity.get("seckill_date")));
        vo.setEndTime(toDate(activity.get("end_date")));
        vo.setStatus(String.valueOf(activity.get("status")));

        Long activityDeptId = toLong(activity.get("dept_id"));

        // Sales stats from mem_seckill_record
        Map<String, Object> salesStats = querySalesStats(seckillId);
        BigDecimal totalSales = toBigDecimal(salesStats.get("total_sales"));
        int totalOrders = toInt(salesStats.get("order_count"));
        vo.setTotalSalesAmount(totalSales);
        vo.setTotalOrders(totalOrders);
        vo.setRelatedSalesAmount(totalSales);

        // R5-D: participant count (distinct members)
        vo.setParticipantCount((long) queryParticipantCount(seckillId));

        // R5-D: first purchase & repurchase member counts
        Date startTime = vo.getStartTime();
        if (startTime != null) {
            vo.setFirstPurchaseMemberCount((long) queryFirstPurchaseCount(seckillId, startTime));
            vo.setRepurchaseMemberCount((long) queryRepurchaseCount(seckillId, startTime));
        } else {
            vo.setFirstPurchaseMemberCount(0L);
            vo.setRepurchaseMemberCount(0L);
        }

        // New customers: members who joined during the activity period
        Date endTime = vo.getEndTime();
        if (activityDeptId != null && startTime != null && endTime != null) {
            vo.setNewCustomerCount(queryNewCustomers(activityDeptId, startTime, endTime));
        } else {
            vo.setNewCustomerCount(0);
        }

        // Sell through rate: (total_shares - remain_shares) / total_shares * 100
        Integer totalShares = toInteger(activity.get("total_shares"));
        Integer remainShares = toInteger(activity.get("remain_shares"));
        if (totalShares != null && totalShares > 0 && remainShares != null) {
            BigDecimal soldShares = new BigDecimal(totalShares - remainShares);
            BigDecimal rate = soldShares.multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(totalShares), 2, RoundingMode.HALF_UP);
            vo.setSellThroughRate(rate);
        } else {
            vo.setSellThroughRate(BigDecimal.ZERO);
        }

        // Discount cost: UNAVAILABLE - original prices not reliably available
        vo.setDiscountCost(null);
        vo.setDiscountCostStatus("UNAVAILABLE");

        // R5-D: ROI computation with explicit unavailable reasons
        BigDecimal activityCost = queryActivityCost(seckillId);
        vo.setActivityCostAmount(activityCost);
        computeRoi(vo, activityCost, totalSales);

        return vo;
    }

    /**
     * R5-D: 纯逻辑 ROI 计算，可独立测试。
     * - 成本缺失 → UNAVAILABLE/MISSING_ACTIVITY_COST
     * - 无关联销售 → UNAVAILABLE/NO_RELATED_SALES
     * - 成本和销售均可用 → READY，计算 roi 和 grossProfit
     */
    void computeRoi(MemberActivityRoiVO vo, BigDecimal cost, BigDecimal sales) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
            vo.setRoi(null);
            vo.setRoiStatus("UNAVAILABLE");
            vo.setUnavailableReason("MISSING_ACTIVITY_COST");
            vo.setSuggestion("当前活动未维护成本，暂不能计算 ROI，请先补充活动成本或在报表中按销售贡献查看。");
            vo.setGrossProfitAmount(null);
            return;
        }
        if (sales == null || sales.compareTo(BigDecimal.ZERO) <= 0) {
            vo.setRoi(null);
            vo.setRoiStatus("UNAVAILABLE");
            vo.setUnavailableReason("NO_RELATED_SALES");
            vo.setSuggestion("当前活动暂无关联销售，暂不能计算 ROI，请确认活动是否已产生成交或稍后再查看。");
            vo.setGrossProfitAmount(null);
            return;
        }
        BigDecimal grossProfit = sales.subtract(cost);
        BigDecimal roiValue = grossProfit.multiply(new BigDecimal("100"))
                .divide(cost, 2, RoundingMode.HALF_UP);
        vo.setGrossProfitAmount(grossProfit);
        vo.setRoi(roiValue);
        vo.setRoiStatus("READY");
        vo.setUnavailableReason(null);
        vo.setSuggestion(null);
    }

    // ── Package-private query methods (overridable in tests) ──

    /**
     * 查询秒杀活动列表
     */
    List<Map<String, Object>> queryActivities(List<Long> deptIds, Long activityId) {
        StringBuilder sql = new StringBuilder(
                "SELECT seckill_id, seckill_name, dept_id, seckill_date, end_date, "
                + "total_shares, remain_shares, status "
                + "FROM mem_seckill "
                + "WHERE del_flag = '0' AND status IN ('0', '1')"
        );
        List<Object> params = new ArrayList<>();

        if (deptIds != null && !deptIds.isEmpty()) {
            sql.append(" AND dept_id IN (");
            for (int i = 0; i < deptIds.size(); i++) {
                if (i > 0) sql.append(',');
                sql.append('?');
                params.add(deptIds.get(i));
            }
            sql.append(')');
        }
        if (activityId != null) {
            sql.append(" AND seckill_id = ?");
            params.add(activityId);
        }
        sql.append(" ORDER BY seckill_date DESC");

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /**
     * 查询某活动的销售统计
     */
    Map<String, Object> querySalesStats(Long seckillId) {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) AS total_sales, COUNT(*) AS order_count "
                + "FROM mem_seckill_record "
                + "WHERE seckill_id = ? AND status = '1' AND del_flag = '0'";
        try {
            return jdbcTemplate.queryForMap(sql, seckillId);
        } catch (EmptyResultDataAccessException e) {
            return Collections.emptyMap();
        }
    }

    /**
     * 查询活动期间新入会会员数
     */
    int queryNewCustomers(Long deptId, Date startTime, Date endTime) {
        String sql = "SELECT COUNT(*) FROM mem_member "
                + "WHERE dept_id = ? AND join_date BETWEEN ? AND ? AND del_flag = '0'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, deptId, startTime, endTime);
        return count != null ? count : 0;
    }

    /**
     * R5-D: 查询活动成本。当前 mem_seckill 表无成本列时返回 null（MISSING_ACTIVITY_COST）。
     */
    BigDecimal queryActivityCost(Long seckillId) {
        try {
            String sql = "SELECT cost_amount FROM mem_seckill WHERE seckill_id = ? AND del_flag = '0'";
            return jdbcTemplate.queryForObject(sql, BigDecimal.class, seckillId);
        } catch (Exception e) {
            // 列不存在或查询失败，视为成本缺失
            return null;
        }
    }

    /**
     * R5-D: 查询活动参与人数（去重 member_id）
     */
    int queryParticipantCount(Long seckillId) {
        String sql = "SELECT COUNT(DISTINCT member_id) FROM mem_seckill_record "
                + "WHERE seckill_id = ? AND status = '1' AND del_flag = '0'";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, seckillId);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * R5-D: 首购会员数——活动期间首次消费的会员（活动开始前无消费记录）
     */
    int queryFirstPurchaseCount(Long seckillId, Date activityStart) {
        String sql = "SELECT COUNT(DISTINCT sr.member_id) FROM mem_seckill_record sr "
                + "WHERE sr.seckill_id = ? AND sr.status = '1' AND sr.del_flag = '0' "
                + "AND NOT EXISTS (SELECT 1 FROM mem_points_record pr "
                + "WHERE pr.member_id = sr.member_id AND pr.del_flag = '0' "
                + "AND pr.create_time < ?)";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, seckillId, activityStart);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * R5-D: 复购会员数——活动前已有消费记录的参与会员
     */
    int queryRepurchaseCount(Long seckillId, Date activityStart) {
        String sql = "SELECT COUNT(DISTINCT sr.member_id) FROM mem_seckill_record sr "
                + "WHERE sr.seckill_id = ? AND sr.status = '1' AND sr.del_flag = '0' "
                + "AND EXISTS (SELECT 1 FROM mem_points_record pr "
                + "WHERE pr.member_id = sr.member_id AND pr.del_flag = '0' "
                + "AND pr.create_time < ?)";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, seckillId, activityStart);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ── Type conversion helpers ──

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int toInt(Object obj) {
        Integer val = toInteger(obj);
        return val != null ? val : 0;
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return new BigDecimal(obj.toString());
        try {
            return new BigDecimal(obj.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Date toDate(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Date) return (Date) obj;
        if (obj instanceof Timestamp) return new Date(((Timestamp) obj).getTime());
        return null;
    }
}
