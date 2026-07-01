package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.vo.CashflowDashboardVO;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.service.IFinanceCashflowReportService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cashflow dashboard aggregation service.
 * Uses JdbcTemplate directly (same pattern as FinDashboardController) to
 * aggregate cross-table cash-health metrics with dept-scope enforcement.
 */
@Service
public class FinanceCashflowReportServiceImpl implements IFinanceCashflowReportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RemoteUserService remoteUserService;

    /**
     * Sentinel deptId used when a non-admin user has no authorized departments.
     * IN (-1) will never match real data, preventing data leakage.
     */
    private static final List<Long> SENTINEL_DEPT_IDS = Collections.singletonList(-1L);

    @Override
    public CashflowDashboardVO getCashflowDashboard(ReportQueryParams params) {
        applyDataScope(params);
        List<Long> deptIds = params.getDeptIds();
        if (deptIds == null || deptIds.isEmpty()) {
            deptIds = SENTINEL_DEPT_IDS;
        }

        CashflowDashboardVO vo = new CashflowDashboardVO();
        vo.setDeptIds(deptIds);

        String inClause = buildInPlaceholders(deptIds);
        Object[] args = deptIds.toArray();

        // Sale payments received
        BigDecimal totalReceivedSalePayment = queryDecimal(
                "SELECT COALESCE(SUM(payment_amount), 0) FROM fin_sale_payment WHERE dept_id IN (" + inClause + ") AND del_flag = '0'",
                args);
        vo.setTotalReceivedSalePayment(totalReceivedSalePayment);

        // Verified expenses
        BigDecimal totalVerifiedExpense = queryDecimal(
                "SELECT COALESCE(SUM(expense_amount), 0) FROM fin_expense WHERE status = '1' AND dept_id IN (" + inClause + ") AND del_flag = '0'",
                args);
        vo.setTotalVerifiedExpense(totalVerifiedExpense);

        // Unverified expenses
        BigDecimal totalUnverifiedExpense = queryDecimal(
                "SELECT COALESCE(SUM(expense_amount), 0) FROM fin_expense WHERE status = '0' AND dept_id IN (" + inClause + ") AND del_flag = '0'",
                args);
        vo.setTotalUnverifiedExpense(totalUnverifiedExpense);

        // Advance balance (unverified advances)
        BigDecimal totalAdvanceBalance = queryDecimal(
                "SELECT COALESCE(SUM(advance_amount), 0) FROM fin_advance WHERE status = '0' AND dept_id IN (" + inClause + ") AND del_flag = '0'",
                args);
        vo.setTotalAdvanceBalance(totalAdvanceBalance);

        // Paid investor payments
        BigDecimal totalPaidInvestorPayment = queryDecimal(
                "SELECT COALESCE(SUM(amount), 0) FROM fin_investor_payment WHERE payment_status = '1' AND dept_id IN (" + inClause + ") AND del_flag = '0'",
                args);
        vo.setTotalPaidInvestorPayment(totalPaidInvestorPayment);

        // Unpaid investor payments
        BigDecimal totalUnpaidInvestorPayment = queryDecimal(
                "SELECT COALESCE(SUM(amount), 0) FROM fin_investor_payment WHERE payment_status = '0' AND dept_id IN (" + inClause + ") AND del_flag = '0'",
                args);
        vo.setTotalUnpaidInvestorPayment(totalUnpaidInvestorPayment);

        // netCashInflow = received sale payments - verified expenses
        vo.setNetCashInflow(totalReceivedSalePayment.subtract(totalVerifiedExpense));

        // cashPressureItems = count of unverified expenses + count of unpaid investor payments
        int unverifiedExpenseCount = queryCount(
                "SELECT COUNT(*) FROM fin_expense WHERE status = '0' AND dept_id IN (" + inClause + ") AND del_flag = '0'",
                args);
        int unpaidInvestorPaymentCount = queryCount(
                "SELECT COUNT(*) FROM fin_investor_payment WHERE payment_status = '0' AND dept_id IN (" + inClause + ") AND del_flag = '0'",
                args);
        vo.setCashPressureItems(unverifiedExpenseCount + unpaidInvestorPaymentCount);

        return vo;
    }

    // ── JdbcTemplate helpers (same pattern as FinDashboardController) ──

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

    private int queryCount(String sql, Object... args) {
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
            return count != null ? count.intValue() : 0;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    // ── Data scope (same pattern as FinanceReportServiceImpl) ──

    private void applyDataScope(ReportQueryParams params) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            Long currentDeptId = SecurityUtils.getDeptId();
            allowed = currentDeptId != null ? Collections.singletonList(currentDeptId) : SENTINEL_DEPT_IDS;
        }
        List<Long> requested = params.getDeptIds();
        if (requested == null || requested.isEmpty()) {
            params.setDeptIds(new ArrayList<>(allowed));
            return;
        }
        List<Long> finalAllowed = allowed;
        List<Long> filtered = requested.stream()
                .filter(finalAllowed::contains)
                .collect(Collectors.toList());
        params.setDeptIds(filtered.isEmpty() ? new ArrayList<>(allowed) : filtered);
    }

    private List<Long> loadAllowedDeptIds() {
        String username = SecurityUtils.getUsername();
        if (username == null || username.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            R<List<SysDept>> response = remoteUserService.getUserDeptList(username, SecurityConstants.INNER);
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData().stream()
                    .map(SysDept::getDeptId)
                    .filter(deptId -> deptId != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ── SQL builder helper ──

    private String buildInPlaceholders(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return "?";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < deptIds.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("?");
        }
        return sb.toString();
    }
}
