package com.junsong.member.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.member.domain.vo.MemberOperationMetrics;
import com.junsong.member.domain.vo.MemberOperationSuggestionVO;
import com.junsong.member.domain.vo.MemberPointsOperationSummaryVO;
import com.junsong.member.domain.vo.MemberSegmentRowVO;
import com.junsong.member.service.IMemberOperationSuggestionService;
import com.junsong.member.service.IMemberPointsOperationService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;

@RestController
@RequestMapping("/dashboard")
public class MemDashboardController extends BaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private IMemberOperationSuggestionService suggestionService;

    @Autowired
    private IMemberPointsOperationService pointsOperationService;

    // ==================== helper methods ====================

    /**
     * Parse a comma-separated string of department IDs into a deduplicated list.
     * Returns empty list for null or blank input.
     */
    List<Long> parseDeptIds(String deptIdsStr) {
        if (deptIdsStr == null || deptIdsStr.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(deptIdsStr.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Long::valueOf)
            .distinct()
            .collect(Collectors.toList());
    }

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
     * Resolve the requested department IDs against the user's allowed set.
     * - Admin: returns requestedDeptIds as-is (empty means all stores)
     * - Non-admin: intersects requested with allowed; if empty intersection returns [-1L]
     */
    List<Long> resolveDeptIds(List<Long> requestedDeptIds) {
        List<Long> allowedDeptIds = loadAllowedDeptIds();
        if (allowedDeptIds.isEmpty()) {
            // admin: no restriction
            if (requestedDeptIds != null && !requestedDeptIds.isEmpty()) {
                return requestedDeptIds;
            }
            return Collections.emptyList();
        }
        if (requestedDeptIds == null || requestedDeptIds.isEmpty()) {
            return allowedDeptIds;
        }
        Set<Long> allowed = new HashSet<>(allowedDeptIds);
        List<Long> intersection = requestedDeptIds.stream()
                .filter(allowed::contains)
                .collect(Collectors.toList());
        if (intersection.isEmpty()) {
            return Collections.singletonList(-1L);
        }
        return intersection;
    }

    /**
     * Build a SQL IN-filter clause.
     * Returns "1=1" when the list is empty (meaning no filtering / all stores),
     * or "column IN (?,?,...)" when the list has elements.
     * This prevents the dangerous IN (NULL) pattern.
     */
    String inFilter(String column, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "1=1";
        }
        return column + " IN (" + inPlaceholders(ids.size()) + ")";
    }

    /**
     * Build SQL IN-clause placeholder string like "?,?,?" for a given number of elements.
     */
    private String inPlaceholders(int count) {
        if (count <= 0) {
            return "NULL";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }

    /**
     * Combine dept IDs and extra params into a single Object[] for JdbcTemplate queries.
     * When deptIds is empty (all-stores mode), only extra params are returned.
     */
    private Object[] deptArgs(List<Long> deptIds, Object... extra) {
        if (deptIds == null || deptIds.isEmpty()) {
            return extra;
        }
        Object[] args = new Object[deptIds.size() + extra.length];
        for (int i = 0; i < deptIds.size(); i++) {
            args[i] = deptIds.get(i);
        }
        System.arraycopy(extra, 0, args, deptIds.size(), extra.length);
        return args;
    }

    // ==================== endpoints ====================

    @RequiresPermissions("member:dashboard:list")
    @GetMapping("/stats")
    public AjaxResult getStats(@RequestParam(required = false) String deptIds) {
        List<Long> requested = parseDeptIds(deptIds);
        List<Long> resolved = resolveDeptIds(requested);
        String df = inFilter("dept_id", resolved);
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalMembers", queryCount(
                "SELECT COUNT(*) FROM mem_member WHERE " + df + " AND del_flag = '0'", resolved));

        stats.put("todayMembers", queryCount(
                "SELECT COUNT(*) FROM mem_member WHERE " + df + " AND del_flag = '0' AND DATE(create_time) = CURDATE()", resolved));

        stats.put("totalPointsRecords", queryCount(
                "SELECT COUNT(*) FROM mem_points_record WHERE " + df + " AND del_flag = '0'", resolved));

        stats.put("totalExchanges", queryCount(
                "SELECT COUNT(*) FROM mem_points_exchange WHERE " + df + " AND del_flag = '0'", resolved));

        stats.put("totalConsumeAmount", queryDecimal(
                "SELECT COALESCE(SUM(consume_amount),0) FROM mem_points_record WHERE " + df + " AND del_flag = '0'", resolved));

        stats.put("totalExchangeItems", queryDecimal(
                "SELECT COALESCE(SUM(quantity),0) FROM mem_points_exchange WHERE " + df + " AND del_flag = '0'", resolved));

        stats.put("activeMembers", queryCount(
                "SELECT COUNT(DISTINCT member_id) FROM mem_points_record WHERE " + df + " AND del_flag = '0' AND create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)", resolved));

        stats.put("totalPointsIssued", queryDecimal(
                "SELECT COALESCE(SUM(points),0) FROM mem_points_record WHERE " + df + " AND del_flag = '0' AND points > 0", resolved));

        stats.put("totalPointsUsed", queryDecimal(
                "SELECT COALESCE(SUM(ABS(points)),0) FROM mem_points_record WHERE " + df + " AND del_flag = '0' AND points < 0", resolved));

        stats.put("totalExpense", queryDecimal(
                "SELECT COALESCE(SUM(expense_amount),0) FROM fin_expense WHERE " + df + " AND del_flag='0'", resolved));
        stats.put("totalPurchase", queryDecimal(
                "SELECT COALESCE(SUM(total_amount),0) FROM fin_purchase WHERE " + df + " AND del_flag='0'", resolved));
        stats.put("totalSale", queryDecimal(
                "SELECT COALESCE(SUM(sale_amount),0) FROM fin_sale_record WHERE " + df + " AND del_flag='0'", resolved));
        stats.put("totalAdvance", queryDecimal(
                "SELECT COALESCE(SUM(advance_amount),0) FROM fin_advance WHERE " + df + " AND del_flag='0'", resolved));
        stats.put("totalSalePaid", queryDecimal(
                "SELECT COALESCE(SUM(paid_amount),0) FROM fin_sale_record WHERE " + df + " AND del_flag='0'", resolved));
        stats.put("totalPurchasePaid", queryDecimal(
                "SELECT COALESCE(SUM(paid_amount),0) FROM fin_purchase WHERE " + df + " AND del_flag='0'", resolved));
        stats.put("todayExpense", queryDecimal(
                "SELECT COALESCE(SUM(expense_amount),0) FROM fin_expense WHERE " + df + " AND del_flag='0' AND DATE(expense_date)=CURDATE()", resolved));
        stats.put("todaySale", queryDecimal(
                "SELECT COALESCE(SUM(sale_amount),0) FROM fin_sale_record WHERE " + df + " AND del_flag='0' AND DATE(sale_date)=CURDATE()", resolved));

        stats.put("unverifiedExpense", queryDecimal(
                "SELECT COALESCE(SUM(expense_amount),0) FROM fin_expense WHERE " + df + " AND del_flag='0' AND status='0'", resolved));
        stats.put("unverifiedAdvance", queryDecimal(
                "SELECT COALESCE(SUM(advance_amount),0) FROM fin_advance WHERE " + df + " AND del_flag='0' AND status='0'", resolved));
        stats.put("unverifiedPurchase", queryDecimal(
                "SELECT COALESCE(SUM(total_amount),0) FROM fin_purchase WHERE " + df + " AND del_flag='0' AND status='0'", resolved));
        stats.put("returnSituation", queryDecimal(
                "SELECT COALESCE(return_situation,0) FROM fin_cost_accounting WHERE " + df + " AND del_flag='0' ORDER BY create_time DESC LIMIT 1", resolved));
        stats.put("totalInvest", queryDecimal(
                "SELECT COALESCE(SUM(total_invest),0) FROM fin_cost_accounting WHERE " + df + " AND del_flag='0'", resolved));

        return AjaxResult.success(stats);
    }

    @RequiresPermissions("member:dashboard:list")
    @GetMapping("/trend")
    public AjaxResult getTrend(@RequestParam(required = false) String deptIds) {
        List<Long> requested = parseDeptIds(deptIds);
        List<Long> resolved = resolveDeptIds(requested);
        String df = inFilter("dept_id", resolved);
        List<String> dates = new ArrayList<>();
        List<Object> newMembers = new ArrayList<>();
        List<Object> consumeAmounts = new ArrayList<>();
        List<Object> pointsChanges = new ArrayList<>();
        List<Object> dailyExpense = new ArrayList<>();
        List<Object> dailySale = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            String dateStr = day.format(fmt);
            String sqlDate = day.toString();
            dates.add(dateStr);

            newMembers.add(queryDecimal(
                    "SELECT COALESCE(COUNT(*), 0) FROM mem_member WHERE " + df + " AND del_flag = '0' AND DATE(create_time) = ?",
                    deptArgs(resolved, sqlDate)));

            consumeAmounts.add(queryDecimal(
                    "SELECT COALESCE(SUM(consume_amount), 0) FROM mem_points_record WHERE " + df + " AND del_flag = '0' AND DATE(create_time) = ?",
                    deptArgs(resolved, sqlDate)));

            pointsChanges.add(queryDecimal(
                    "SELECT COALESCE(SUM(points), 0) FROM mem_points_record WHERE " + df + " AND del_flag = '0' AND DATE(create_time) = ?",
                    deptArgs(resolved, sqlDate)));

            dailyExpense.add(queryDecimal(
                    "SELECT COALESCE(SUM(expense_amount),0) FROM fin_expense WHERE " + df + " AND del_flag='0' AND DATE(expense_date)=?",
                    deptArgs(resolved, sqlDate)));

            dailySale.add(queryDecimal(
                    "SELECT COALESCE(SUM(sale_amount),0) FROM fin_sale_record WHERE " + df + " AND del_flag='0' AND DATE(sale_date)=?",
                    deptArgs(resolved, sqlDate)));
        }

        Map<String, Object> trend = new HashMap<>();
        trend.put("dates", dates);
        trend.put("newMembers", newMembers);
        trend.put("consumeAmounts", consumeAmounts);
        trend.put("pointsChanges", pointsChanges);
        trend.put("dailyExpense", dailyExpense);
        trend.put("dailySale", dailySale);
        return AjaxResult.success(trend);
    }

    @RequiresPermissions("member:dashboard:list")
    @GetMapping("/ranking")
    public AjaxResult getRanking(@RequestParam(required = false) String deptIds) {
        List<Long> requested = parseDeptIds(deptIds);
        List<Long> resolved = resolveDeptIds(requested);
        String sql = buildRankingSql(resolved);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, resolved.toArray());
        return AjaxResult.success(list);
    }

    /**
     * Build the ranking SQL. The inner subquery operates on mem_points_record without
     * a table alias, so its filter must use the bare column name "dept_id" (not "r.dept_id").
     */
    String buildRankingSql(List<Long> resolved) {
        String innerDf = inFilter("dept_id", resolved);
        return "SELECT r.member_id, r.member_no, r.member_name, r.balance " +
                "FROM mem_points_record r " +
                "INNER JOIN ( " +
                "    SELECT member_id, MAX(record_id) AS max_id " +
                "    FROM mem_points_record " +
                "    WHERE " + innerDf + " AND del_flag = '0' " +
                "    GROUP BY member_id " +
                ") latest ON r.record_id = latest.max_id " +
                "ORDER BY r.balance DESC " +
                "LIMIT 10";
    }

    @RequiresPermissions("member:dashboard:list")
    @GetMapping("/operation")
    public AjaxResult getOperation(@RequestParam(required = false) String deptIds) {
        List<Long> requested = parseDeptIds(deptIds);
        List<Long> resolved = resolveDeptIds(requested);
        String df = inFilter("dept_id", resolved);
        String sf = inFilter("store_id", resolved);
        String rdf = inFilter("r.dept_id", resolved);
        Map<String, Object> data = new HashMap<>();

        // --- member counts ---
        long totalMembers = queryCount(
                "SELECT COUNT(*) FROM mem_member WHERE " + df + " AND del_flag = '0'", resolved);
        long todayNewMembers = queryCount(
                "SELECT COUNT(*) FROM mem_member WHERE " + df + " AND del_flag = '0' AND DATE(create_time) = CURDATE()", resolved);
        long activeMembers30d = queryCount(
                "SELECT COUNT(DISTINCT member_id) FROM mem_points_record WHERE " + df + " AND del_flag = '0' AND create_time > NOW() - INTERVAL 30 DAY", resolved);

        data.put("totalMembers", totalMembers);
        data.put("todayNewMembers", todayNewMembers);
        data.put("activeMembers30d", activeMembers30d);

        // active rate
        BigDecimal activeRate30d = BigDecimal.ZERO;
        if (totalMembers > 0) {
            activeRate30d = BigDecimal.valueOf(activeMembers30d)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalMembers), 1, BigDecimal.ROUND_HALF_UP);
        }
        data.put("activeRate30d", activeRate30d);

        // --- member sales ---
        BigDecimal memberSalesAmount = queryDecimal(
                "SELECT COALESCE(SUM(sale_amount),0) FROM fin_sale_record WHERE " + df + " AND del_flag = '0' AND remark LIKE '%member%'", resolved);
        long memberSaleOrderCount = queryCount(
                "SELECT COUNT(*) FROM fin_sale_record WHERE " + df + " AND del_flag = '0' AND remark LIKE '%member%'", resolved);

        data.put("memberSalesAmount", memberSalesAmount);
        data.put("memberSaleOrderCount", memberSaleOrderCount);

        BigDecimal avgMemberSaleAmount = BigDecimal.ZERO;
        if (memberSaleOrderCount > 0) {
            avgMemberSaleAmount = memberSalesAmount
                    .divide(BigDecimal.valueOf(memberSaleOrderCount), 2, BigDecimal.ROUND_HALF_UP);
        }
        data.put("avgMemberSaleAmount", avgMemberSaleAmount);

        // --- points ---
        BigDecimal pointsIssued = queryDecimal(
                "SELECT COALESCE(SUM(points),0) FROM mem_points_record WHERE " + df + " AND del_flag = '0' AND points > 0", resolved);
        BigDecimal pointsUsed = queryDecimal(
                "SELECT COALESCE(SUM(ABS(points)),0) FROM mem_points_record WHERE " + df + " AND del_flag = '0' AND points < 0", resolved);

        data.put("pointsIssued", pointsIssued);
        data.put("pointsUsed", pointsUsed);

        BigDecimal pointsUseRate = BigDecimal.ZERO;
        if (pointsIssued.compareTo(BigDecimal.ZERO) > 0) {
            pointsUseRate = pointsUsed
                    .multiply(BigDecimal.valueOf(100))
                    .divide(pointsIssued, 1, BigDecimal.ROUND_HALF_UP);
        }
        data.put("pointsUseRate", pointsUseRate);

        // --- exchange / refund / seckill ---
        long exchangeCount = queryCount(
                "SELECT COUNT(*) FROM mem_points_exchange WHERE " + df + " AND del_flag = '0'", resolved);
        long pendingRefundCount = queryCount(
                "SELECT COUNT(*) FROM mem_refund_apply WHERE " + sf + " AND workflow_status IN ('PENDING_STORE_APPROVAL','PENDING_FINANCE_APPROVAL') AND del_flag = '0'", resolved);
        long seckillActiveCount = queryCount(
                "SELECT COUNT(*) FROM mem_seckill WHERE " + df + " AND status = '0' AND del_flag = '0'", resolved);
        long seckillParticipantCount = queryCount(
                "SELECT COUNT(DISTINCT r.record_id) FROM mem_seckill_record r " +
                "INNER JOIN mem_seckill s ON r.seckill_id = s.seckill_id " +
                "WHERE " + rdf + " AND r.del_flag = '0' AND s.status = '0' AND s.del_flag = '0'", resolved);

        data.put("exchangeCount", exchangeCount);
        data.put("pendingRefundCount", pendingRefundCount);
        data.put("seckillActiveCount", seckillActiveCount);
        data.put("seckillParticipantCount", seckillParticipantCount);

        // --- operation warnings ---
        List<String> operationWarnings = new ArrayList<>();
        if (pendingRefundCount > 0) {
            operationWarnings.add("当前有 " + pendingRefundCount + " 笔待处理退款，请及时跟进。");
        }
        if (totalMembers > 0 && activeRate30d.compareTo(BigDecimal.valueOf(10)) < 0) {
            operationWarnings.add("近30天会员活跃率仅 " + activeRate30d + "%，低于10%警戒线。");
        }
        if (pointsIssued.compareTo(BigDecimal.ZERO) > 0 && pointsUseRate.compareTo(BigDecimal.valueOf(5)) < 0) {
            operationWarnings.add("积分使用率仅 " + pointsUseRate + "%，积分沉淀过多，建议激活消耗。");
        }
        if (seckillActiveCount == 0) {
            operationWarnings.add("当前无进行中的秒杀活动，会员活跃度可能受影响。");
        }
        if (todayNewMembers == 0) {
            operationWarnings.add("今日暂无新增会员，请关注拉新渠道。");
        }
        data.put("operationWarnings", operationWarnings);

        // --- R5 member growth metrics (real data from mem_member / mem_points_record / fin_sale_record) ---
        long silentMemberCount = Math.max(0, totalMembers - activeMembers30d);
        data.put("silentMemberCount", silentMemberCount);

        long highValueMemberCount = queryCount(
                "SELECT COUNT(*) FROM (SELECT member_id FROM mem_points_record WHERE " + df + " AND del_flag = '0' GROUP BY member_id HAVING SUM(consume_amount) >= 1000) t", resolved);
        data.put("highValueMemberCount", highValueMemberCount);

        long purchasedMemberCount = queryCount(
                "SELECT COUNT(DISTINCT member_id) FROM mem_points_record WHERE " + df + " AND del_flag = '0'", resolved);
        BigDecimal firstPurchaseRate = BigDecimal.ZERO;
        if (totalMembers > 0) {
            firstPurchaseRate = BigDecimal.valueOf(purchasedMemberCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalMembers), 1, BigDecimal.ROUND_HALF_UP);
        }
        data.put("firstPurchaseRate", firstPurchaseRate);

        long repurchaseMemberCount = queryCount(
                "SELECT COUNT(*) FROM (SELECT member_id FROM mem_points_record WHERE " + df + " AND del_flag = '0' AND create_time > NOW() - INTERVAL 30 DAY GROUP BY member_id HAVING COUNT(*) >= 2) t", resolved);
        BigDecimal repurchaseRate30d = BigDecimal.ZERO;
        if (activeMembers30d > 0) {
            repurchaseRate30d = BigDecimal.valueOf(repurchaseMemberCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(activeMembers30d), 1, BigDecimal.ROUND_HALF_UP);
        }
        data.put("repurchaseRate30d", repurchaseRate30d);

        BigDecimal totalSaleAmount = queryDecimal(
                "SELECT COALESCE(SUM(sale_amount),0) FROM fin_sale_record WHERE " + df + " AND del_flag = '0'", resolved);
        BigDecimal memberSalesRatio = BigDecimal.ZERO;
        if (totalSaleAmount.compareTo(BigDecimal.ZERO) > 0) {
            memberSalesRatio = memberSalesAmount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalSaleAmount, 1, BigDecimal.ROUND_HALF_UP);
        }
        data.put("memberSalesRatio", memberSalesRatio);
        data.put("averageMemberOrderAmount", avgMemberSaleAmount);

        BigDecimal totalAvailablePoints = queryDecimal(
                "SELECT COALESCE(SUM(r.balance),0) FROM mem_points_record r " +
                "INNER JOIN (SELECT member_id, MAX(record_id) AS max_id FROM mem_points_record WHERE " + df + " AND del_flag = '0' GROUP BY member_id) latest ON r.record_id = latest.max_id", resolved);
        BigDecimal pointsLiabilityAmount = totalAvailablePoints.divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
        data.put("totalAvailablePoints", totalAvailablePoints);
        data.put("pointsLiabilityAmount", pointsLiabilityAmount);

        long newMemberCount30d = queryCount(
                "SELECT COUNT(*) FROM mem_member WHERE " + df + " AND del_flag = '0' AND create_time > NOW() - INTERVAL 30 DAY", resolved);
        data.put("newMemberCount30d", newMemberCount30d);

        boolean activityRoiAvailable = seckillActiveCount > 0 && memberSalesAmount.compareTo(BigDecimal.ZERO) > 0;

        List<MemberSegmentRowVO> segmentRows = new ArrayList<>();
        segmentRows.add(buildSegmentRow("NEW", "新会员", newMemberCount30d, totalMembers));
        segmentRows.add(buildSegmentRow("ACTIVE", "活跃会员", activeMembers30d, totalMembers));
        segmentRows.add(buildSegmentRow("SILENT", "沉默会员", silentMemberCount, totalMembers));
        segmentRows.add(buildSegmentRow("HIGH_VALUE", "高价值会员", highValueMemberCount, totalMembers));
        data.put("segmentRows", segmentRows);

        MemberOperationMetrics metrics = new MemberOperationMetrics();
        metrics.setTotalMemberCount(totalMembers);
        metrics.setNewMemberCount(newMemberCount30d);
        metrics.setActiveMemberCount(activeMembers30d);
        metrics.setSilentMemberCount(silentMemberCount);
        metrics.setHighValueMemberCount(highValueMemberCount);
        metrics.setFirstPurchaseRate(firstPurchaseRate);
        metrics.setRepurchaseRate30d(repurchaseRate30d);
        metrics.setPointsLiabilityAmount(pointsLiabilityAmount);
        metrics.setActivityRoiAvailable(activityRoiAvailable);
        List<MemberOperationSuggestionVO> suggestions = suggestionService.generateSuggestions(metrics);
        data.put("suggestions", suggestions);

        return AjaxResult.success(data);
    }

    @RequiresPermissions("member:dashboard:list")
    @GetMapping("/points-summary")
    public AjaxResult getPointsSummary(@RequestParam(required = false) String deptIds) {
        List<Long> requested = parseDeptIds(deptIds);
        MemberPointsOperationSummaryVO vo = pointsOperationService.getPointsOperationSummary(requested);
        return AjaxResult.success(vo);
    }

    // ==================== query helpers ====================

    private MemberSegmentRowVO buildSegmentRow(String type, String name, long count, long total) {
        MemberSegmentRowVO row = new MemberSegmentRowVO();
        row.setSegmentType(type);
        row.setSegmentName(name);
        row.setMemberCount(count);
        row.setRatio(total > 0
                ? BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 1, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO);
        return row;
    }

    private long queryCount(String sql, List<Long> deptIds) {
        try {
            Object[] args = (deptIds == null || deptIds.isEmpty()) ? new Object[0] : deptIds.toArray();
            Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
            return count != null ? count : 0L;
        } catch (EmptyResultDataAccessException e) {
            return 0L;
        }
    }

    private long queryCount(String sql, Object... args) {
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
            return count != null ? count : 0L;
        } catch (EmptyResultDataAccessException e) {
            return 0L;
        }
    }

    private BigDecimal queryDecimal(String sql, List<Long> deptIds) {
        try {
            Object[] args = (deptIds == null || deptIds.isEmpty()) ? new Object[0] : deptIds.toArray();
            Object result = jdbcTemplate.queryForObject(sql, Object.class, args);
            if (result == null) return BigDecimal.ZERO;
            if (result instanceof BigDecimal) return (BigDecimal) result;
            return new BigDecimal(result.toString());
        } catch (EmptyResultDataAccessException e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal queryDecimal(String sql, Object... args) {
        try {
            Object result = jdbcTemplate.queryForObject(sql, Object.class, args);
            if (result == null) return BigDecimal.ZERO;
            if (result instanceof BigDecimal) return (BigDecimal) result;
            return new BigDecimal(result.toString());
        } catch (EmptyResultDataAccessException e) {
            return BigDecimal.ZERO;
        }
    }
}
