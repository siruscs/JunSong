package com.junsong.finance.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;

@RestController
@RequestMapping("/dashboard")
public class FinDashboardController
{
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @RequiresPermissions("finance:dashboard:list")
    @GetMapping("/stats")
    public AjaxResult getStats()
    {
        Long deptId = SecurityUtils.getDeptId();
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalExpense", queryDecimal(
                "SELECT COALESCE(SUM(expense_amount),0) FROM fin_expense WHERE dept_id=? AND del_flag='0'", deptId));
        stats.put("totalSale", queryDecimal(
                "SELECT COALESCE(SUM(sale_amount),0) FROM fin_sale_record WHERE dept_id=? AND del_flag='0'", deptId));
        stats.put("totalPurchase", queryDecimal(
                "SELECT COALESCE(SUM(total_amount),0) FROM fin_purchase WHERE dept_id=? AND del_flag='0'", deptId));
        stats.put("totalAdvance", queryDecimal(
                "SELECT COALESCE(SUM(advance_amount),0) FROM fin_advance WHERE dept_id=? AND del_flag='0'", deptId));

        stats.put("unverifiedExpense", queryDecimal(
                "SELECT COALESCE(SUM(expense_amount),0) FROM fin_expense WHERE dept_id=? AND del_flag='0' AND status='0'", deptId));
        stats.put("unverifiedAdvance", queryDecimal(
                "SELECT COALESCE(SUM(advance_amount),0) FROM fin_advance WHERE dept_id=? AND del_flag='0' AND status='0'", deptId));

        stats.put("todaySale", queryDecimal(
                "SELECT COALESCE(SUM(sale_amount),0) FROM fin_sale_record WHERE dept_id=? AND del_flag='0' AND DATE(sale_date)=CURDATE()", deptId));
        stats.put("todayExpense", queryDecimal(
                "SELECT COALESCE(SUM(expense_amount),0) FROM fin_expense WHERE dept_id=? AND del_flag='0' AND DATE(expense_date)=CURDATE()", deptId));

        stats.put("expenseCount", queryCount(
                "SELECT COUNT(*) FROM fin_expense WHERE dept_id=? AND del_flag='0'", deptId));
        stats.put("saleCount", queryCount(
                "SELECT COUNT(*) FROM fin_sale_record WHERE dept_id=? AND del_flag='0'", deptId));
        stats.put("purchaseCount", queryCount(
                "SELECT COUNT(*) FROM fin_purchase WHERE dept_id=? AND del_flag='0'", deptId));
        stats.put("advanceCount", queryCount(
                "SELECT COUNT(*) FROM fin_advance WHERE dept_id=? AND del_flag='0'", deptId));

        stats.put("openPeriodCount", queryCount(
                "SELECT COUNT(*) FROM fin_accounting_period WHERE dept_id=? AND del_flag='0' AND status='0'", deptId));

        return AjaxResult.success(stats);
    }

    private long queryCount(String sql, Object... args)
    {
        try
        {
            Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
            return count != null ? count : 0L;
        }
        catch (EmptyResultDataAccessException e)
        {
            return 0L;
        }
    }

    private BigDecimal queryDecimal(String sql, Object... args)
    {
        try
        {
            Object result = jdbcTemplate.queryForObject(sql, Object.class, args);
            if (result == null) return BigDecimal.ZERO;
            if (result instanceof BigDecimal) return (BigDecimal) result;
            return new BigDecimal(result.toString());
        }
        catch (EmptyResultDataAccessException e)
        {
            return BigDecimal.ZERO;
        }
    }
}
