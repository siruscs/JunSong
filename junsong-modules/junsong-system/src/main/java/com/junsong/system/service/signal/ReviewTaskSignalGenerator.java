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
 * 财务复盘信号生成器。
 * 扫描 finance_review_task 中未完成复盘任务，投递经营任务。
 * 复盘任务 DONE/IGNORED 时自动完成对应经营任务。
 *
 * 注意：finance_review_task 表无 tenant_id 列（依赖库级隔离），JdbcTemplate 查询不加 tenant_id 条件。
 *
 * @author junsong
 */
@Component
public class ReviewTaskSignalGenerator implements OperatingTaskSignalGenerator
{
    private static final Logger log = LoggerFactory.getLogger(ReviewTaskSignalGenerator.class);

    private static final String SOURCE_MODULE = "FINANCE";
    private static final String SOURCE_TYPE = "REVIEW_TASK";
    private static final String DEFAULT_SOURCE_ROUTE = "/finance/reviewTask";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_DONE = "DONE";

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
        return "REVIEW_TASK";
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
            log.warn("财务复盘信号生成器执行失败: {}", e.getMessage(), e);
        }
        return created;
    }

    /**
     * 扫描未完成复盘任务，投递经营任务
     */
    private int scanAndCreateTasks()
    {
        List<Long> authorizedDeptIds = authorizedDeptResolver.resolveAuthorizedDeptIds();
        String deptFilter = buildDeptFilter(authorizedDeptIds);

        String sql = "SELECT task_id, dept_id, dept_name, severity, title, "
                + "target_route, impact_amount, create_time, task_date "
                + "FROM finance_review_task "
                + "WHERE del_flag = '0' AND status IN ('PENDING', 'IN_PROGRESS')"
                + deptFilter;

        int[] created = {0};
        jdbcTemplate.query(sql, (ResultSet rs) -> {
            Long reviewTaskId = rs.getLong("task_id");
            Long deptId = rs.getObject("dept_id", Long.class);
            String deptName = rs.getString("dept_name");
            String severity = rs.getString("severity");
            String title = rs.getString("title");
            String targetRoute = rs.getString("target_route");
            BigDecimal impactAmount = rs.getBigDecimal("impact_amount");
            Timestamp createTs = rs.getTimestamp("create_time");
            Timestamp taskDateTs = rs.getTimestamp("task_date");
            Date occurTime = createTs != null ? new Date(createTs.getTime())
                    : (taskDateTs != null ? new Date(taskDateTs.getTime()) : new Date());

            SysOperatingTask task = new SysOperatingTask();
            task.setSourceModule(SOURCE_MODULE);
            task.setSourceType(SOURCE_TYPE);
            task.setSourceId(String.valueOf(reviewTaskId));
            task.setSourceRoute(safeStr(targetRoute, DEFAULT_SOURCE_ROUTE));
            task.setTaskType("REVIEW_TASK");
            task.setDeptId(deptId);
            task.setDeptName(deptName);
            task.setTitle("财务复盘：" + safeStr(title, "未命名任务"));
            task.setPriority(mapPriorityFromSeverity(severity));
            task.setSeverity(safeStr(severity, "MEDIUM"));
            task.setOccurTime(occurTime);
            task.setDueTime(dueTimeCalculator.calculateDueTime(SOURCE_TYPE, occurTime, null));
            task.setImpactAmount(impactAmount != null ? impactAmount.setScale(2, RoundingMode.HALF_UP) : null);

            SysOperatingTask result = operatingTaskService.createOrUpdateTask(task);
            if (result == task)
            {
                created[0]++;
            }
        });

        return created[0];
    }

    /**
     * 检查已完成/已忽略的复盘任务，自动完成对应经营任务
     */
    private void autoCompleteClosedSources()
    {
        Long tenantId = TenantContext.getTenantId();
        List<Long> authorizedDeptIds = authorizedDeptResolver.resolveAuthorizedDeptIds();
        String deptFilter = buildDeptFilter(authorizedDeptIds);

        String sql = "SELECT task_id FROM finance_review_task "
                + "WHERE del_flag = '0' AND status IN ('DONE', 'IGNORED')"
                + deptFilter;

        jdbcTemplate.query(sql, (ResultSet rs) -> {
            Long reviewTaskId = rs.getLong("task_id");
            String idempotencyKey = tenantId + ":" + SOURCE_MODULE + ":" + SOURCE_TYPE + ":" + reviewTaskId;
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
        });
    }

    /**
     * 优先级按 severity 映射：HIGH→HIGH, MEDIUM→MEDIUM, LOW→LOW
     */
    private String mapPriorityFromSeverity(String severity)
    {
        if (severity == null)
        {
            return "MEDIUM";
        }
        switch (severity)
        {
            case "HIGH":
                return "HIGH";
            case "MEDIUM":
                return "MEDIUM";
            case "LOW":
                return "LOW";
            default:
                return "MEDIUM";
        }
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
