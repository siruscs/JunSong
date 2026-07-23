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
 * 逾期应收信号生成器。
 * 扫描 finance_receivable_collection 中未结清应收，投递经营任务。
 * 来源已 PAID 时自动完成对应经营任务。
 *
 * 注意：finance_receivable_collection 表有 tenant_id 列，JdbcTemplate 绕过 MyBatis 拦截器，
 * 需手动加 tenant_id 条件。
 *
 * @author junsong
 */
@Component
public class OverdueReceivableSignalGenerator implements OperatingTaskSignalGenerator
{
    private static final Logger log = LoggerFactory.getLogger(OverdueReceivableSignalGenerator.class);

    private static final String SOURCE_MODULE = "FINANCE";
    private static final String SOURCE_TYPE = "RECEIVABLE_COLLECTION";
    private static final String SOURCE_ROUTE = "/finance/receivable";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_DONE = "DONE";

    private static final String PRIORITY_URGENT = "URGENT";
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
        return "OVERDUE_RECEIVABLE";
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
            log.warn("逾期应收信号生成器执行失败: {}", e.getMessage(), e);
        }
        return created;
    }

    /**
     * 扫描未结清应收，投递经营任务
     */
    private int scanAndCreateTasks()
    {
        Long tenantId = TenantContext.getTenantId();
        List<Long> authorizedDeptIds = authorizedDeptResolver.resolveAuthorizedDeptIds();
        String deptFilter = buildDeptFilter(authorizedDeptIds);

        String sql = "SELECT collection_id, dept_id, customer_name, unpaid_amount, age_days, "
                + "priority_level, next_follow_time, create_time "
                + "FROM finance_receivable_collection "
                + "WHERE del_flag = '0' AND collection_status IN ('PENDING', 'PROMISED') "
                + "AND unpaid_amount > 0 AND tenant_id = ?"
                + deptFilter;

        int[] created = {0};
        jdbcTemplate.query(sql, (ResultSet rs) -> {
            Long collectionId = rs.getLong("collection_id");
            Long deptId = rs.getObject("dept_id", Long.class);
            String customerName = rs.getString("customer_name");
            BigDecimal unpaidAmount = rs.getBigDecimal("unpaid_amount");
            int ageDays = rs.getInt("age_days");
            String priorityLevel = rs.getString("priority_level");
            Timestamp nextFollowTs = rs.getTimestamp("next_follow_time");
            Date nextFollowTime = nextFollowTs != null ? new Date(nextFollowTs.getTime()) : null;
            Timestamp createTs = rs.getTimestamp("create_time");
            Date occurTime = createTs != null ? new Date(createTs.getTime()) : new Date();

            SysOperatingTask task = new SysOperatingTask();
            task.setSourceModule(SOURCE_MODULE);
            task.setSourceType(SOURCE_TYPE);
            task.setSourceId(String.valueOf(collectionId));
            task.setSourceRoute(SOURCE_ROUTE);
            task.setTaskType("OVERDUE_RECEIVABLE");
            task.setDeptId(deptId);
            task.setTitle("逾期应收跟进：" + safeStr(customerName, "未知客户") + " 未缴 " + unpaidAmount);
            task.setPriority(resolvePriority(ageDays, unpaidAmount));
            task.setSeverity(mapSeverity(priorityLevel));
            task.setOccurTime(occurTime);
            task.setDueTime(dueTimeCalculator.calculateDueTime(SOURCE_TYPE, occurTime, nextFollowTime));
            task.setImpactAmount(unpaidAmount != null ? unpaidAmount.setScale(2, RoundingMode.HALF_UP) : null);

            SysOperatingTask result = operatingTaskService.createOrUpdateTask(task);
            // 引用相等表示新建，不同引用表示已存在
            if (result == task)
            {
                created[0]++;
            }
        }, tenantId);

        return created[0];
    }

    /**
     * 检查已 PAID 的应收，自动完成对应经营任务
     */
    private void autoCompleteClosedSources()
    {
        Long tenantId = TenantContext.getTenantId();
        List<Long> authorizedDeptIds = authorizedDeptResolver.resolveAuthorizedDeptIds();
        String deptFilter = buildDeptFilter(authorizedDeptIds);

        String sql = "SELECT collection_id FROM finance_receivable_collection "
                + "WHERE del_flag = '0' AND collection_status = 'PAID' AND tenant_id = ?"
                + deptFilter;

        jdbcTemplate.query(sql, (ResultSet rs) -> {
            Long collectionId = rs.getLong("collection_id");
            String idempotencyKey = tenantId + ":" + SOURCE_MODULE + ":" + SOURCE_TYPE + ":" + collectionId;
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
     * 按设计文档 §3 规则映射优先级
     */
    private String resolvePriority(int ageDays, BigDecimal unpaidAmount)
    {
        double unpaid = unpaidAmount != null ? unpaidAmount.doubleValue() : 0;
        if (ageDays > 30 && unpaid >= 5000)
        {
            return PRIORITY_URGENT;
        }
        if (ageDays >= 15 && ageDays <= 30)
        {
            return PRIORITY_HIGH;
        }
        if (unpaid >= 2000 && unpaid < 5000)
        {
            return PRIORITY_HIGH;
        }
        if (ageDays >= 8 && ageDays <= 14)
        {
            return PRIORITY_MEDIUM;
        }
        if (unpaid >= 500 && unpaid < 2000)
        {
            return PRIORITY_MEDIUM;
        }
        return PRIORITY_LOW;
    }

    /**
     * severity 来自源 priorityLevel（CRITICAL→HIGH, HIGH→HIGH, MEDIUM→MEDIUM, LOW→LOW）
     */
    private String mapSeverity(String priorityLevel)
    {
        if (priorityLevel == null)
        {
            return PRIORITY_MEDIUM;
        }
        switch (priorityLevel)
        {
            case "CRITICAL":
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
     * 构建 SQL dept_id IN (...) 过滤子句。
     * null=不过滤（超管），非空=返回 " AND dept_id IN (...)"，空列表返回哨兵条件
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
