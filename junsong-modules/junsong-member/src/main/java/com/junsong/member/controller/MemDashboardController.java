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

        // --- R8-D: 经营化字段（增长、活跃、复购、积分负债、活动贡献、会员销售贡献）---
        stats.put("newMemberCount30d", queryCount(
                "SELECT COUNT(*) FROM mem_member WHERE " + df + " AND del_flag = '0' AND create_time > NOW() - INTERVAL 30 DAY", resolved));

        long activeMemberCount30d = queryCount(
                "SELECT COUNT(DISTINCT member_id) FROM mem_points_record WHERE " + df + " AND del_flag = '0' AND create_time > NOW() - INTERVAL 30 DAY", resolved);
        stats.put("activeMemberCount30d", activeMemberCount30d);

        long repeatMemberCount90d = queryCount(
                "SELECT COUNT(*) FROM (SELECT member_id FROM mem_points_record WHERE " + df + " AND del_flag = '0' AND create_time > NOW() - INTERVAL 90 DAY GROUP BY member_id HAVING COUNT(*) >= 2) t", resolved);
        stats.put("repeatMemberCount90d", repeatMemberCount90d);

        long activeMemberCount90d = queryCount(
                "SELECT COUNT(DISTINCT member_id) FROM mem_points_record WHERE " + df + " AND del_flag = '0' AND create_time > NOW() - INTERVAL 90 DAY", resolved);
        stats.put("repeatRate90d", computeRepeatRate90d(repeatMemberCount90d, activeMemberCount90d));

        // 积分负债：只统计剩余正积分（成员欠公司的积分）
        BigDecimal totalPositivePoints = queryDecimal(buildPointsLiabilitySql(resolved), resolved);
        stats.put("pointsLiability", computePointsLiability(totalPositivePoints));

        // 已兑换积分成本（积分 < 0 的绝对值 / 100 转金额）
        BigDecimal totalRedeemedPoints = queryDecimal(
                "SELECT COALESCE(SUM(ABS(points)),0) FROM mem_points_record WHERE " + df + " AND del_flag = '0' AND points < 0", resolved);
        stats.put("pointsRedeemedCost", computePointsRedeemedCost(totalRedeemedPoints));

        // 活动贡献：秒杀已领取记录的成交金额
        BigDecimal activityContributionAmount = queryDecimal(
                "SELECT COALESCE(SUM(total_amount),0) FROM mem_seckill_record WHERE " + df + " AND del_flag = '0' AND status = '1'", resolved);
        stats.put("activityContributionAmount", activityContributionAmount);

        // 会员销售贡献：来自真实销售记录 fin_sale_record
        stats.put("memberSalesAmount", queryDecimal(buildMemberSalesAmountSql(resolved), resolved));
        stats.put("memberSalesOrderCount", queryCount(
                "SELECT COUNT(*) FROM fin_sale_record WHERE " + df + " AND del_flag = '0' AND (member_id IS NOT NULL OR remark LIKE '%member%')", resolved));

        // 活动成本暂无真实表，ROI 不显示 0% 而是提示暂不可算
        stats.put("activityRoiText", computeActivityRoiText(BigDecimal.ZERO, activityContributionAmount));

        // R10-E: 会员销售关联质量
        long memberLinkedSaleCount = queryCount(
                "SELECT COUNT(*) FROM fin_sale_record WHERE " + df + " AND del_flag='0' AND member_id IS NOT NULL", resolved);
        long memberRemarkFallbackSaleCount = queryCount(
                "SELECT COUNT(*) FROM fin_sale_record WHERE " + df + " AND del_flag='0' AND member_id IS NULL AND remark LIKE '%member%'", resolved);
        long totalLinked = memberLinkedSaleCount + memberRemarkFallbackSaleCount;
        BigDecimal linkQualityRate = totalLinked > 0
                ? BigDecimal.valueOf(memberLinkedSaleCount * 100).divide(BigDecimal.valueOf(totalLinked), 1, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        stats.put("memberLinkedSaleCount", memberLinkedSaleCount);
        stats.put("memberRemarkFallbackSaleCount", memberRemarkFallbackSaleCount);
        stats.put("memberLinkQualityRate", linkQualityRate);

        BigDecimal linkThreshold = readRuleThreshold("MEM_MEMBER_LINK_QUALITY", BigDecimal.valueOf(80));
        String linkSuggestion = "";
        if (totalLinked > 0 && linkQualityRate.compareTo(linkThreshold) < 0) {
            linkSuggestion = "会员销售精确关联率偏低，建议销售录入时选择会员。";
        }
        stats.put("memberLinkQualitySuggestion", linkSuggestion);

        // R11-I: 会员经营动作项
        stats.put("memberActionItems", buildMemberActionItems(
                linkQualityRate, linkThreshold, totalLinked,
                (BigDecimal) stats.get("pointsLiability"),
                (long) stats.get("activeMemberCount30d"),
                (long) stats.get("totalMembers")));

        return AjaxResult.success(stats);
    }

    /**
     * Build member action items based on health thresholds (R11-I).
     * Each item: { level, title, reason, suggestion, targetRoute }
     */
    List<Map<String, Object>> buildMemberActionItems(
            BigDecimal linkQualityRate, BigDecimal linkThreshold, long totalLinked,
            BigDecimal pointsLiability, long activeMemberCount, long totalMemberCount)
    {
        List<Map<String, Object>> items = new ArrayList<>();

        // Rule 1: 会员销售关联率低于阈值 -> MEDIUM
        if (totalLinked > 0 && linkQualityRate.compareTo(linkThreshold) < 0) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("level", "MEDIUM");
            item.put("title", "会员销售精确关联率偏低");
            item.put("reason", "当前关联率 " + linkQualityRate + "%，低于阈值 " + linkThreshold + "%。");
            item.put("suggestion", "销售录入时选择会员，提高精确关联率。");
            item.put("targetRoute", "/finance/sale");
            items.add(item);
        }

        // Rule 2: 积分负债超过阈值 -> MEDIUM
        BigDecimal liabilityThreshold = readRuleThreshold("MEM_POINTS_LIABILITY", BigDecimal.valueOf(1000));
        if (pointsLiability != null && pointsLiability.compareTo(liabilityThreshold) > 0) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("level", "MEDIUM");
            item.put("title", "积分负债偏高");
            item.put("reason", "当前积分负债 ¥" + pointsLiability + "，超过阈值 ¥" + liabilityThreshold + "。");
            item.put("suggestion", "关注积分兑换压力，可考虑引导会员消耗积分。");
            item.put("targetRoute", "/member/pointsRule");
            items.add(item);
        }

        // Rule 3: 有会员但30天无活跃 -> LOW
        if (activeMemberCount == 0 && totalMemberCount > 0) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("level", "LOW");
            item.put("title", "会员活跃度为零");
            item.put("reason", "共有 " + totalMemberCount + " 名会员，但近 30 天无活跃记录。");
            item.put("suggestion", "可通过促销活动或积分激励唤醒沉睡会员。");
            item.put("targetRoute", "/member/member");
            items.add(item);
        }

        return items;
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

    // ==================== R8-D: 经营化计算方法（可测试，纯逻辑无 JDBC）====================

    /**
     * 计算 90 日复购率 = 复购会员数 / 活跃会员数 * 100。
     * 分母为 0 时返回 0（避免除零异常）。
     */
    protected BigDecimal computeRepeatRate90d(long repeatMemberCount90d, long activeMemberCount90d) {
        if (activeMemberCount90d <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(repeatMemberCount90d)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(activeMemberCount90d), 1, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 计算积分负债金额：剩余正积分 / 100 转金额。
     * 仅统计正积分（成员欠公司的积分），负积分不计入负债。
     */
    protected BigDecimal computePointsLiability(BigDecimal totalPositivePoints) {
        if (totalPositivePoints == null || totalPositivePoints.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalPositivePoints.divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 计算已兑换积分成本：已使用积分绝对值 / 100 转金额。
     */
    protected BigDecimal computePointsRedeemedCost(BigDecimal totalRedeemedPoints) {
        if (totalRedeemedPoints == null || totalRedeemedPoints.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalRedeemedPoints.divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 计算活动 ROI 文本。
     * 活动成本缺失时不显示 0%，而是提示"暂不可算"。
     */
    protected String computeActivityRoiText(BigDecimal activityCost, BigDecimal activityRevenue) {
        if (activityCost == null || activityCost.compareTo(BigDecimal.ZERO) <= 0) {
            return "ROI 暂不可算 / 缺少活动成本";
        }
        if (activityRevenue == null) {
            return "ROI 暂不可算 / 缺少活动收入";
        }
        BigDecimal roi = activityRevenue.subtract(activityCost)
                .multiply(BigDecimal.valueOf(100))
                .divide(activityCost, 1, BigDecimal.ROUND_HALF_UP);
        return roi + "%";
    }

    /**
     * 构建会员销售金额 SQL（来源：真实销售记录表 fin_sale_record）。
     */
    String buildMemberSalesAmountSql(List<Long> resolved) {
        String df = inFilter("dept_id", resolved);
        return "SELECT COALESCE(SUM(sale_amount),0) FROM fin_sale_record WHERE " + df
                + " AND del_flag = '0' AND (member_id IS NOT NULL OR remark LIKE '%member%')";
    }

    /**
     * 构建积分负债 SQL：只统计成员最新余额为正的积分（负债）。
     */
    String buildPointsLiabilitySql(List<Long> resolved) {
        String df = inFilter("dept_id", resolved);
        return "SELECT COALESCE(SUM(CASE WHEN r.balance > 0 THEN r.balance ELSE 0 END),0) " +
                "FROM mem_points_record r " +
                "INNER JOIN (SELECT member_id, MAX(record_id) AS max_id FROM mem_points_record WHERE " + df + " AND del_flag = '0' GROUP BY member_id) latest ON r.record_id = latest.max_id";
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
                "SELECT COALESCE(SUM(sale_amount),0) FROM fin_sale_record WHERE " + df + " AND del_flag = '0' AND (member_id IS NOT NULL OR remark LIKE '%member%')", resolved);
        long memberSaleOrderCount = queryCount(
                "SELECT COUNT(*) FROM fin_sale_record WHERE " + df + " AND del_flag = '0' AND (member_id IS NOT NULL OR remark LIKE '%member%')", resolved);

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

    /** R10-E: 读取规则阈值，失败返回默认值 */
    protected BigDecimal readRuleThreshold(String ruleCode, BigDecimal defaultValue) {
        try {
            BigDecimal val = jdbcTemplate.queryForObject(
                "SELECT threshold_value FROM sys_health_rule_config WHERE rule_code = ? AND enabled = '1'",
                BigDecimal.class, ruleCode);
            return val != null ? val : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
