package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.vo.FinanceOperationDashboardVO;
import com.junsong.finance.domain.vo.OperatingMetric;
import com.junsong.finance.domain.vo.ReportQueryParams;
import com.junsong.finance.service.IFinanceOperatingMetricService;
import com.junsong.finance.service.IFinanceReportService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 统一经营指标服务实现（Phase 5）。
 *
 * 财务指标复用 IFinanceReportService.getOperationDashboard；
 * 会员指标和任务计数通过 JdbcTemplate 跨模块读取（与 MemDashboardController
 * 和 SystemWorkbenchServiceImpl 的跨模块读取模式一致）。
 *
 * 所有指标使用相同的授权门店交集模型，保证 PC 和小程序口径一致。
 */
@Service
public class FinanceOperatingMetricServiceImpl implements IFinanceOperatingMetricService {

    private static final Logger log = LoggerFactory.getLogger(FinanceOperatingMetricServiceImpl.class);
    private static final List<Long> SENTINEL_DEPT_IDS = Collections.singletonList(-1L);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private IFinanceReportService financeReportService;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<OperatingMetric> getOperatingMetrics(ReportQueryParams params) {
        // 1. 统一授权部门解析（与 CashflowDashboardController 一致）
        List<Long> authorizedDeptIds = resolveAuthorizedDeptIds(params);
        String tenantId = String.valueOf(TenantContext.getTenantId());

        // 2. 构建查询参数（使用授权后的 deptIds）
        ReportQueryParams effectiveParams = new ReportQueryParams();
        effectiveParams.setDeptIds(authorizedDeptIds);
        effectiveParams.setTimeType(params != null ? params.getTimeType() : null);
        effectiveParams.setStartTime(params != null ? params.getStartTime() : null);
        effectiveParams.setEndTime(params != null ? params.getEndTime() : null);

        // 3. 获取财务指标（复用现有 service，保证口径一致）
        FinanceOperationDashboardVO dashboard = financeReportService.getOperationDashboard(effectiveParams);

        // 4. 构建统一指标列表
        String today = LocalDate.now().format(DATE_FMT);
        String monthStart = LocalDate.now().withDayOfMonth(1).format(DATE_FMT);
        OperatingMetric.Period todayPeriod = new OperatingMetric.Period("TODAY", today, today);
        OperatingMetric.Period monthPeriod = new OperatingMetric.Period("MONTH", monthStart, today);
        OperatingMetric.Scope scope = new OperatingMetric.Scope(authorizedDeptIds, tenantId);
        OperatingMetric.Source finSource = new OperatingMetric.Source("FINANCE", "/dashboard/operation");
        OperatingMetric.Source memberSource = new OperatingMetric.Source("MEMBER", "/member/dashboard/stats");
        OperatingMetric.Source systemSource = new OperatingMetric.Source("SYSTEM", "/operatingTask/pendingCount");

        List<OperatingMetric> metrics = new ArrayList<>();

        // 指标 1：今日销售
        metrics.add(buildMetric("todaySales", scale2(dashboard.getTodaySales()), "CNY",
                todayPeriod, scope, finSource, "/finance/sale"));

        // 指标 2：今日费用
        metrics.add(buildMetric("todayExpense", scale2(dashboard.getTodayExpense()), "CNY",
                todayPeriod, scope, finSource, "/finance/expense"));

        // 指标 3：净现金流（实收 - 费用）
        BigDecimal netCashflow = scale2(dashboard.getCurrentPeriodPaymentAmount())
                .subtract(scale2(dashboard.getTodayExpense()));
        metrics.add(buildMetric("netCashflow", netCashflow, "CNY",
                todayPeriod, scope, finSource, "/finance/cashflow"));

        // 指标 4：应收余额
        metrics.add(buildMetric("receivableBalance", scale2(dashboard.getEndingReceivableAmount()), "CNY",
                monthPeriod, scope, finSource, "/finance/receivable"));

        // 指标 5：逾期应收笔数
        metrics.add(buildMetric("overdueReceivable", BigDecimal.valueOf(dashboard.getOverdueReceivableCount()), "COUNT",
                monthPeriod, scope, finSource, "/finance/receivable"));

        // 指标 6：库存风险（负库存门店数）
        int negativeStockCount = countNegativeStock(authorizedDeptIds);
        metrics.add(buildMetric("inventoryRisk", BigDecimal.valueOf(negativeStockCount), "COUNT",
                todayPeriod, scope, finSource, "/finance/stock/health"));

        // 指标 7：今日会员新增（跨模块读 mem_member）
        int todayNewMembers = countTodayNewMembers(authorizedDeptIds);
        metrics.add(buildMetric("todayNewMembers", BigDecimal.valueOf(todayNewMembers), "COUNT",
                todayPeriod, scope, memberSource, "/member/member"));

        // 指标 8：活跃会员（近30天有消费记录）
        int activeMembers = countActiveMembers(authorizedDeptIds);
        metrics.add(buildMetric("activeMembers", BigDecimal.valueOf(activeMembers), "COUNT",
                monthPeriod, scope, memberSource, "/member/segment"));

        // 指标 9：待核销金额（费用 + 借支）
        BigDecimal unverifiedTotal = scale2(dashboard.getUnverifiedExpenseAmount())
                .add(scale2(dashboard.getUnverifiedAdvanceAmount()));
        metrics.add(buildMetric("unverifiedAmount", unverifiedTotal, "CNY",
                todayPeriod, scope, finSource, "/finance/expense"));

        // 指标 10：待办任务数（跨模块读 sys_operating_task）
        int pendingTaskCount = countPendingTasks(authorizedDeptIds);
        metrics.add(buildMetric("pendingTaskCount", BigDecimal.valueOf(pendingTaskCount), "COUNT",
                todayPeriod, scope, systemSource, "/system/operatingTask"));

        return metrics;
    }

    // ── 授权部门解析（与 CashflowDashboardController 模式一致）──

    private List<Long> resolveAuthorizedDeptIds(ReportQueryParams params) {
        if (SecurityUtils.isAdmin()) {
            List<Long> requested = params != null ? params.getDeptIds() : null;
            return (requested != null && !requested.isEmpty()) ? requested : null;
        }

        List<Long> allowed = loadAllowedDeptIds();
        if (allowed.isEmpty()) {
            return SENTINEL_DEPT_IDS;
        }

        List<Long> requested = params != null ? params.getDeptIds() : null;
        if (requested == null || requested.isEmpty()) {
            return new ArrayList<>(allowed);
        }

        List<Long> finalAllowed = allowed;
        List<Long> intersection = requested.stream()
                .filter(finalAllowed::contains)
                .collect(Collectors.toList());

        return intersection.isEmpty() ? SENTINEL_DEPT_IDS : intersection;
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
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("获取用户授权门店列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── 跨模块查询（JdbcTemplate，与现有模式一致）──

    private int countNegativeStock(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return 0;
        String inClause = buildInClause(deptIds.size());
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT dept_id) FROM fin_stock_ledger WHERE dept_id IN (" + inClause + ") " +
                            "AND after_quantity < 0 AND del_flag='0'", Integer.class, deptIds.toArray());
            return count != null ? count : 0;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    private int countTodayNewMembers(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return 0;
        String inClause = buildInClause(deptIds.size());
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM mem_member WHERE dept_id IN (" + inClause + ") " +
                            "AND del_flag='0' AND DATE(create_time)=CURDATE()", Integer.class, deptIds.toArray());
            return count != null ? count : 0;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    private int countActiveMembers(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return 0;
        String inClause = buildInClause(deptIds.size());
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT m.member_id) FROM mem_member m " +
                            "INNER JOIN fin_sale_record s ON s.member_id = m.member_id " +
                            "WHERE m.dept_id IN (" + inClause + ") AND m.del_flag='0' " +
                            "AND s.del_flag='0' AND s.sale_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)",
                    Integer.class, deptIds.toArray());
            return count != null ? count : 0;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    private int countPendingTasks(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return 0;
        String inClause = buildInClause(deptIds.size());
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_operating_task WHERE dept_id IN (" + inClause + ") " +
                            "AND del_flag='0' AND status IN ('PENDING','IN_PROGRESS','REOPENED')",
                    Integer.class, deptIds.toArray());
            return count != null ? count : 0;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    // ── 工具方法 ──

    private static String buildInClause(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }

    private static BigDecimal scale2(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static OperatingMetric buildMetric(String code, BigDecimal value, String unit,
                                               OperatingMetric.Period period, OperatingMetric.Scope scope,
                                               OperatingMetric.Source source, String drillDownRoute) {
        OperatingMetric m = new OperatingMetric();
        m.setCode(code);
        m.setValue(value);
        m.setUnit(unit);
        m.setPeriod(period);
        m.setScope(scope);
        m.setSource(source);
        m.setDrillDownRoute(drillDownRoute);
        return m;
    }
}
