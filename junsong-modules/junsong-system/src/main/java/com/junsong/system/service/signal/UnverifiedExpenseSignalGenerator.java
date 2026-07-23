package com.junsong.system.service.signal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.junsong.common.core.context.TenantContext;
import com.junsong.system.domain.SysOperatingTask;
import com.junsong.system.mapper.SysOperatingTaskMapper;
import com.junsong.system.service.AuthorizedDeptResolver;
import com.junsong.system.service.ISysOperatingTaskService;

/**
 * 未核销费用信号生成器。
 * 扫描 fin_expense 中未核销费用（status='0'），投递经营任务。
 * 来源已核销（status='1'）时自动完成对应经营任务。
 *
 * 注意：fin_expense 表有 tenant_id 列（继承自 BaseEntity），JdbcTemplate 绕过 MyBatis
 * TenantSqlInterceptor 拦截器，需手动加 tenant_id 条件。部门范围过滤通过 AuthorizedDeptResolver 保证。
 *
 * @author junsong
 */
@Component
public class UnverifiedExpenseSignalGenerator implements OperatingTaskSignalGenerator
{
    private static final Logger log = LoggerFactory.getLogger(UnverifiedExpenseSignalGenerator.class);

    private static final String SOURCE_MODULE = "FINANCE";
    private static final String SOURCE_TYPE = "EXPENSE_VERIFY";
    private static final String SOURCE_ROUTE = "/finance/expense";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_DONE = "DONE";

    private static final String PRIORITY_HIGH = "HIGH";
    private static final String PRIORITY_MEDIUM = "MEDIUM";
    private static final String PRIORITY_LOW = "LOW";

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ISysOperatingTaskService operatingTaskService;

    @Autowired
    private SysOperatingTaskMapper operatingTaskMapper;

    @Autowired
    private AuthorizedDeptResolver authorizedDeptResolver;

    @Autowired
    private OperatingTaskDueTimeCalculator dueTimeCalculator;

    @Override
    public String generatorCode()
    {
        return "UNVERIFIED_EXPENSE";
    }

    @Override
    public int generate()
    {
        int created = 0;
        if (jdbcTemplate == null)
        {
            return 0;
        }
        try
        {
            created += scanAndCreateTasks();
            autoCompleteClosedSources();
        }
        catch (Exception e)
        {
            log.warn("未核销费用信号生成器执行失败: {}", e.getMessage(), e);
        }
        return created;
    }

    /**
     * 扫描未核销费用，投递经营任务
     */
    private int scanAndCreateTasks()
    {
        Long tenantId = TenantContext.getTenantId();
        List<Long> authorizedDeptIds = authorizedDeptResolver.resolveAuthorizedDeptIds();
        String deptFilter = buildDeptFilter(authorizedDeptIds);

        String sql = "SELECT expense_id, dept_id, expense_no, expense_date, expense_amount "
                + "FROM fin_expense "
                + "WHERE del_flag = '0' AND status = '0' AND tenant_id = ?"
                + deptFilter;

        int[] created = {0};
        jdbcTemplate.query(sql, (ResultSet rs) -> {
            Long expenseId = rs.getLong("expense_id");
            Long deptId = rs.getObject("dept_id", Long.class);
            String expenseNo = rs.getString("expense_no");
            BigDecimal expenseAmount = rs.getBigDecimal("expense_amount");
            Timestamp expenseTs = rs.getTimestamp("expense_date");
            Date occurTime = expenseTs != null ? new Date(expenseTs.getTime()) : new Date();

            SysOperatingTask task = new SysOperatingTask();
            task.setSourceModule(SOURCE_MODULE);
            task.setSourceType(SOURCE_TYPE);
            task.setSourceId(String.valueOf(expenseId));
            task.setSourceRoute(SOURCE_ROUTE);
            task.setTaskType("UNVERIFIED_EXPENSE");
            task.setDeptId(deptId);
            task.setTitle("费用待核销：" + safeStr(expenseNo, "未知") + " " + expenseAmount + "元");
            task.setPriority(resolvePriority(expenseAmount));
            task.setSeverity(PRIORITY_MEDIUM);
            task.setOccurTime(occurTime);
            task.setDueTime(dueTimeCalculator.calculateDueTime(SOURCE_TYPE, occurTime, null));
            task.setImpactAmount(expenseAmount != null ? expenseAmount.setScale(2, RoundingMode.HALF_UP) : null);

            SysOperatingTask result = operatingTaskService.createOrUpdateTask(task);
            if (result == task)
            {
                created[0]++;
            }
        }, tenantId);

        return created[0];
    }

    /**
     * 检查已核销费用（status='1'），自动完成对应经营任务
     */
    private void autoCompleteClosedSources()
    {
        Long tenantId = TenantContext.getTenantId();
        List<Long> authorizedDeptIds = authorizedDeptResolver.resolveAuthorizedDeptIds();
        String deptFilter = buildDeptFilter(authorizedDeptIds);

        String sql = "SELECT expense_id FROM fin_expense "
                + "WHERE del_flag = '0' AND status = '1' AND tenant_id = ?"
                + deptFilter;

        jdbcTemplate.query(sql, (ResultSet rs) -> {
            Long expenseId = rs.getLong("expense_id");
            String idempotencyKey = tenantId + ":" + SOURCE_MODULE + ":" + SOURCE_TYPE + ":" + expenseId;
            SysOperatingTask task = operatingTaskMapper.selectByIdempotencyKey(tenantId, idempotencyKey);
            if (task == null)
            {
                return;
            }
            if (STATUS_PENDING.equals(task.getStatus()) || STATUS_IN_PROGRESS.equals(task.getStatus()))
            {
                operatingTaskMapper.conditionalUpdateStatus(
                        task.getTaskId(), task.getStatus(), task.getVersion(), STATUS_DONE,
                        null, null, "来源已关闭，自动完成", null, null);
            }
        }, tenantId);
    }

    /**
     * 优先级映射：≥5000 → HIGH；1000-5000 → MEDIUM；<1000 → LOW
     */
    private String resolvePriority(BigDecimal expenseAmount)
    {
        double amount = expenseAmount != null ? expenseAmount.doubleValue() : 0;
        if (amount >= 5000)
        {
            return PRIORITY_HIGH;
        }
        if (amount >= 1000)
        {
            return PRIORITY_MEDIUM;
        }
        return PRIORITY_LOW;
    }

    /**
     * 构建 SQL dept_id IN (...) 过滤子句
     */
    private String buildDeptFilter(List<Long> authorizedDeptIds)
    {
        if (authorizedDeptIds == null)
        {
            return "";
        }
        if (authorizedDeptIds.isEmpty())
        {
            return " AND dept_id = -1";
        }
        String ids = authorizedDeptIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return " AND dept_id IN (" + ids + ")";
    }

    private String safeStr(String val, String defaultVal)
    {
        return (val == null || val.isEmpty()) ? defaultVal : val;
    }
}
