package com.junsong.member.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.utils.SecurityUtils;

@RestController
@RequestMapping("/dashboard")
public class MemDashboardController extends BaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/stats")
    public AjaxResult getStats() {
        Long deptId = SecurityUtils.getDeptId();
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalMembers", queryCount(
                "SELECT COUNT(*) FROM mem_member WHERE dept_id = ? AND del_flag = '0'", deptId));

        stats.put("todayMembers", queryCount(
                "SELECT COUNT(*) FROM mem_member WHERE dept_id = ? AND del_flag = '0' AND DATE(create_time) = CURDATE()", deptId));

        stats.put("totalPointsRecords", queryCount(
                "SELECT COUNT(*) FROM mem_points_record WHERE dept_id = ? AND del_flag = '0'", deptId));

        stats.put("totalExchanges", queryCount(
                "SELECT COUNT(*) FROM mem_points_exchange WHERE dept_id = ? AND del_flag = '0'", deptId));

        stats.put("totalConsumeAmount", queryDecimal(
                "SELECT COALESCE(SUM(consume_amount),0) FROM mem_points_record WHERE dept_id = ? AND del_flag = '0'", deptId));

        stats.put("totalExchangeItems", queryDecimal(
                "SELECT COALESCE(SUM(quantity),0) FROM mem_points_exchange WHERE dept_id = ? AND del_flag = '0'", deptId));

        stats.put("activeMembers", queryCount(
                "SELECT COUNT(DISTINCT member_id) FROM mem_points_record WHERE dept_id = ? AND del_flag = '0' AND create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)", deptId));

        stats.put("totalPointsIssued", queryDecimal(
                "SELECT COALESCE(SUM(points),0) FROM mem_points_record WHERE dept_id = ? AND del_flag = '0' AND points > 0", deptId));

        stats.put("totalPointsUsed", queryDecimal(
                "SELECT COALESCE(SUM(ABS(points)),0) FROM mem_points_record WHERE dept_id = ? AND del_flag = '0' AND points < 0", deptId));

        stats.put("totalExpense", queryDecimal(
                "SELECT COALESCE(SUM(expense_amount),0) FROM fin_expense WHERE dept_id=? AND del_flag='0'", deptId));
        stats.put("totalPurchase", queryDecimal(
                "SELECT COALESCE(SUM(total_amount),0) FROM fin_purchase WHERE dept_id=? AND del_flag='0'", deptId));
        stats.put("totalSale", queryDecimal(
                "SELECT COALESCE(SUM(sale_amount),0) FROM fin_sale_record WHERE dept_id=? AND del_flag='0'", deptId));
        stats.put("totalAdvance", queryDecimal(
                "SELECT COALESCE(SUM(advance_amount),0) FROM fin_advance WHERE dept_id=? AND del_flag='0'", deptId));
        stats.put("totalSalePaid", queryDecimal(
                "SELECT COALESCE(SUM(paid_amount),0) FROM fin_sale_record WHERE dept_id=? AND del_flag='0'", deptId));
        stats.put("totalPurchasePaid", queryDecimal(
                "SELECT COALESCE(SUM(paid_amount),0) FROM fin_purchase WHERE dept_id=? AND del_flag='0'", deptId));
        stats.put("todayExpense", queryDecimal(
                "SELECT COALESCE(SUM(expense_amount),0) FROM fin_expense WHERE dept_id=? AND del_flag='0' AND DATE(expense_date)=CURDATE()", deptId));
        stats.put("todaySale", queryDecimal(
                "SELECT COALESCE(SUM(sale_amount),0) FROM fin_sale_record WHERE dept_id=? AND del_flag='0' AND DATE(sale_date)=CURDATE()", deptId));

        stats.put("unverifiedExpense", queryDecimal(
                "SELECT COALESCE(SUM(expense_amount),0) FROM fin_expense WHERE dept_id=? AND del_flag='0' AND status='0'", deptId));
        stats.put("unverifiedAdvance", queryDecimal(
                "SELECT COALESCE(SUM(advance_amount),0) FROM fin_advance WHERE dept_id=? AND del_flag='0' AND status='0'", deptId));
        stats.put("unverifiedPurchase", queryDecimal(
                "SELECT COALESCE(SUM(total_amount),0) FROM fin_purchase WHERE dept_id=? AND del_flag='0' AND status='0'", deptId));
        stats.put("returnSituation", queryDecimal(
                "SELECT COALESCE(return_situation,0) FROM fin_cost_accounting WHERE dept_id=? AND del_flag='0' ORDER BY create_time DESC LIMIT 1", deptId));
        stats.put("totalInvest", queryDecimal(
                "SELECT COALESCE(SUM(total_invest),0) FROM fin_cost_accounting WHERE dept_id=? AND del_flag='0'", deptId));

        return AjaxResult.success(stats);
    }

    @GetMapping("/trend")
    public AjaxResult getTrend() {
        Long deptId = SecurityUtils.getDeptId();
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
                    "SELECT COALESCE(COUNT(*), 0) FROM mem_member WHERE dept_id = ? AND del_flag = '0' AND DATE(create_time) = ?",
                    deptId, sqlDate));

            consumeAmounts.add(queryDecimal(
                    "SELECT COALESCE(SUM(consume_amount), 0) FROM mem_points_record WHERE dept_id = ? AND del_flag = '0' AND DATE(create_time) = ?",
                    deptId, sqlDate));

            pointsChanges.add(queryDecimal(
                    "SELECT COALESCE(SUM(points), 0) FROM mem_points_record WHERE dept_id = ? AND del_flag = '0' AND DATE(create_time) = ?",
                    deptId, sqlDate));

            dailyExpense.add(queryDecimal(
                    "SELECT COALESCE(SUM(expense_amount),0) FROM fin_expense WHERE dept_id=? AND del_flag='0' AND DATE(expense_date)=?",
                    deptId, sqlDate));

            dailySale.add(queryDecimal(
                    "SELECT COALESCE(SUM(sale_amount),0) FROM fin_sale_record WHERE dept_id=? AND del_flag='0' AND DATE(sale_date)=?",
                    deptId, sqlDate));
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

    @GetMapping("/ranking")
    public AjaxResult getRanking() {
        Long deptId = SecurityUtils.getDeptId();
        String sql = "SELECT r.member_id, r.member_no, r.member_name, r.balance " +
                "FROM mem_points_record r " +
                "INNER JOIN ( " +
                "    SELECT member_id, MAX(record_id) AS max_id " +
                "    FROM mem_points_record " +
                "    WHERE dept_id = ? AND del_flag = '0' " +
                "    GROUP BY member_id " +
                ") latest ON r.record_id = latest.max_id " +
                "ORDER BY r.balance DESC " +
                "LIMIT 10";

        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, deptId);
        return AjaxResult.success(list);
    }

    private long queryCount(String sql, Object... args) {
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
            return count != null ? count : 0L;
        } catch (EmptyResultDataAccessException e) {
            return 0L;
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
