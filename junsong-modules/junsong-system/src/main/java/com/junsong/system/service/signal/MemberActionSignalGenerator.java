package com.junsong.system.service.signal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 * 会员经营信号生成器。
 * 扫描 mem_member / mem_points_record，按门店聚合会员指标，投递经营任务。
 * - SILENT_MEMBER_HIGH: 沉默会员占比 > 30%
 * - POINTS_LIABILITY_HIGH: 积分负债估算 > 1000 元
 * 指标恢复正常时自动完成对应经营任务。
 *
 * 注意：mem_member / mem_points_record 表有 tenant_id 列（继承自 BaseEntity），JdbcTemplate 绕过
 * MyBatis TenantSqlInterceptor 拦截器，需手动加 tenant_id 条件。SQL 逻辑复用 SystemWorkbenchServiceImpl.collectMemberTasks。
 *
 * @author junsong
 */
@Component
public class MemberActionSignalGenerator implements OperatingTaskSignalGenerator
{
    private static final Logger log = LoggerFactory.getLogger(MemberActionSignalGenerator.class);

    private static final String SOURCE_MODULE = "MEMBER";
    private static final String SOURCE_TYPE_SILENT = "SILENT_MEMBER_HIGH";
    private static final String SOURCE_TYPE_POINTS = "POINTS_LIABILITY_HIGH";
    private static final String ROUTE_SILENT = "/member/segment?segmentType=SILENT";
    private static final String ROUTE_POINTS = "/member/pointsGoods";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_DONE = "DONE";

    private static final String PRIORITY_HIGH = "HIGH";
    private static final String PRIORITY_MEDIUM = "MEDIUM";
    private static final String SEVERITY_HIGH = "HIGH";

    /** 积分负债估算除数（100积分 ≈ 1元） */
    private static final BigDecimal POINTS_TO_YUAN = new BigDecimal("100");
    /** 积分负债阈值（元） */
    private static final BigDecimal LIABILITY_THRESHOLD = new BigDecimal("1000");
    /** 沉默会员占比阈值（%） */
    private static final double SILENT_RATIO_THRESHOLD = 30.0;

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
        return "MEMBER_ACTION";
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
            Set<Long> activeSilentDepts = new HashSet<>();
            Set<Long> activeLiabilityDepts = new HashSet<>();
            created += scanSilentMember(activeSilentDepts);
            created += scanPointsLiability(activeLiabilityDepts);
            autoCompleteClosedSources(activeSilentDepts, activeLiabilityDepts);
        }
        catch (Exception e)
        {
            log.warn("会员经营信号生成器执行失败: {}", e.getMessage(), e);
        }
        return created;
    }

    /**
     * 扫描沉默会员占比过高的门店
     */
    private int scanSilentMember(Set<Long> activeDepts)
    {
        Long tenantId = TenantContext.getTenantId();
        List<Long> authorizedDeptIds = authorizedDeptResolver.resolveAuthorizedDeptIds();
        String mFilter = buildDeptFilter(authorizedDeptIds, "m");

        String sql = "SELECT m.dept_id, "
                + "COUNT(*) AS total_members, "
                + "COUNT(DISTINCT CASE WHEN p.member_id IS NOT NULL THEN m.member_id END) AS active_members "
                + "FROM mem_member m "
                + "LEFT JOIN mem_points_record p ON p.member_id = m.member_id AND p.create_time > NOW() - INTERVAL 30 DAY "
                + "WHERE m.del_flag = '0' AND m.status = '0' AND m.tenant_id = ?"
                + mFilter
                + " GROUP BY m.dept_id";

        int[] created = {0};
        jdbcTemplate.query(sql, (ResultSet rs) -> {
            Long deptId = rs.getObject("dept_id", Long.class);
            if (deptId == null)
            {
                return;
            }
            long total = rs.getLong("total_members");
            long active = rs.getLong("active_members");
            if (total == 0)
            {
                return;
            }
            long silent = total - active;
            double silentRatio = silent * 100.0 / total;

            if (silentRatio > SILENT_RATIO_THRESHOLD)
            {
                activeDepts.add(deptId);

                Date occurTime = new Date();
                SysOperatingTask task = new SysOperatingTask();
                task.setSourceModule(SOURCE_MODULE);
                task.setSourceType(SOURCE_TYPE_SILENT);
                task.setSourceId(String.valueOf(deptId));
                task.setSourceRoute(ROUTE_SILENT);
                task.setTaskType(SOURCE_TYPE_SILENT);
                task.setDeptId(deptId);
                task.setTitle("沉默会员占比过高：门店" + deptId);
                task.setReason("门店" + deptId + " 沉默会员 " + silent + " 人，占比 "
                        + String.format("%.0f", silentRatio) + "%，超过30%阈值。");
                task.setSuggestion("请到会员分层查看沉默会员并制定激活方案。");
                task.setPriority(PRIORITY_HIGH);
                task.setSeverity(SEVERITY_HIGH);
                task.setOccurTime(occurTime);
                task.setDueTime(dueTimeCalculator.calculateDueTime(SOURCE_TYPE_SILENT, occurTime, null));

                SysOperatingTask result = operatingTaskService.createOrUpdateTask(task);
                if (result == task)
                {
                    created[0]++;
                }
            }
        }, tenantId);

        return created[0];
    }

    /**
     * 扫描积分负债过高的门店
     */
    private int scanPointsLiability(Set<Long> activeDepts)
    {
        Long tenantId = TenantContext.getTenantId();
        List<Long> authorizedDeptIds = authorizedDeptResolver.resolveAuthorizedDeptIds();
        String rFilter = buildDeptFilter(authorizedDeptIds, "r");

        String sql = "SELECT r.dept_id, SUM(r.balance) AS total_balance "
                + "FROM mem_points_record r "
                + "INNER JOIN (SELECT member_id, MAX(record_id) AS max_id FROM mem_points_record WHERE tenant_id = ? GROUP BY member_id) latest "
                + "ON r.record_id = latest.max_id "
                + "WHERE r.tenant_id = ?"
                + rFilter
                + " GROUP BY r.dept_id";

        int[] created = {0};
        jdbcTemplate.query(sql, (ResultSet rs) -> {
            Long deptId = rs.getObject("dept_id", Long.class);
            if (deptId == null)
            {
                return;
            }
            BigDecimal totalBalance = rs.getBigDecimal("total_balance");
            if (totalBalance == null)
            {
                return;
            }
            // 积分按 100:1 估算负债
            BigDecimal liability = totalBalance.divide(POINTS_TO_YUAN, 2, RoundingMode.HALF_UP);
            if (liability.compareTo(LIABILITY_THRESHOLD) > 0)
            {
                activeDepts.add(deptId);

                Date occurTime = new Date();
                SysOperatingTask task = new SysOperatingTask();
                task.setSourceModule(SOURCE_MODULE);
                task.setSourceType(SOURCE_TYPE_POINTS);
                task.setSourceId(String.valueOf(deptId));
                task.setSourceRoute(ROUTE_POINTS);
                task.setTaskType(SOURCE_TYPE_POINTS);
                task.setDeptId(deptId);
                task.setTitle("积分负债过高：门店" + deptId);
                task.setReason("门店" + deptId + " 积分沉淀估算负债 " + liability + " 元，超过1000元阈值。");
                task.setSuggestion("请到会员积分运营页核查积分过期策略。");
                task.setPriority(PRIORITY_MEDIUM);
                task.setSeverity(PRIORITY_MEDIUM);
                task.setOccurTime(occurTime);
                task.setDueTime(dueTimeCalculator.calculateDueTime(SOURCE_TYPE_POINTS, occurTime, null));
                task.setImpactAmount(liability.setScale(2, RoundingMode.HALF_UP));

                SysOperatingTask result = operatingTaskService.createOrUpdateTask(task);
                if (result == task)
                {
                    created[0]++;
                }
            }
        }, tenantId, tenantId);

        return created[0];
    }

    /**
     * 自动完成指标已恢复正常的门店对应经营任务。
     * 查询 sys_operating_task 中 MEMBER 类型且 PENDING/IN_PROGRESS 的任务，
     * 若其 source_type 对应的指标已恢复正常（门店不在异常集合中），则自动完成。
     */
    private void autoCompleteClosedSources(Set<Long> activeSilentDepts, Set<Long> activeLiabilityDepts)
    {
        Long tenantId = TenantContext.getTenantId();

        String sql = "SELECT task_id, status, version, source_type, source_id "
                + "FROM sys_operating_task "
                + "WHERE del_flag = '0' AND source_module = 'MEMBER' "
                + "AND source_type IN ('SILENT_MEMBER_HIGH', 'POINTS_LIABILITY_HIGH') "
                + "AND status IN ('PENDING', 'IN_PROGRESS') AND tenant_id = ?";

        jdbcTemplate.query(sql, (ResultSet rs) -> {
            Long taskId = rs.getLong("task_id");
            String status = rs.getString("status");
            Integer version = rs.getObject("version", Integer.class);
            String sourceType = rs.getString("source_type");
            String sourceId = rs.getString("source_id");

            Long deptId = parseLong(sourceId);
            if (deptId == null)
            {
                return;
            }

            boolean shouldComplete = false;
            if (SOURCE_TYPE_SILENT.equals(sourceType))
            {
                // 沉默占比恢复正常 → 门店不在异常集合中
                shouldComplete = !activeSilentDepts.contains(deptId);
            }
            else if (SOURCE_TYPE_POINTS.equals(sourceType))
            {
                // 积分负债恢复正常 → 门店不在异常集合中
                shouldComplete = !activeLiabilityDepts.contains(deptId);
            }

            if (shouldComplete)
            {
                operatingTaskMapper.conditionalUpdateStatus(
                        taskId, status, version, STATUS_DONE,
                        null, null, "来源已关闭，自动完成", null, null);
            }
        }, tenantId);
    }

    /**
     * 构建 SQL 带表别名的 dept_id IN (...) 过滤子句
     */
    private String buildDeptFilter(List<Long> authorizedDeptIds, String alias)
    {
        if (authorizedDeptIds == null)
        {
            return "";
        }
        String col = (alias != null && !alias.isEmpty()) ? alias + ".dept_id" : "dept_id";
        if (authorizedDeptIds.isEmpty())
        {
            return " AND " + col + " = -1";
        }
        String ids = authorizedDeptIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return " AND " + col + " IN (" + ids + ")";
    }

    private Long parseLong(String val)
    {
        if (val == null || val.isEmpty())
        {
            return null;
        }
        try
        {
            return Long.valueOf(val);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
}
