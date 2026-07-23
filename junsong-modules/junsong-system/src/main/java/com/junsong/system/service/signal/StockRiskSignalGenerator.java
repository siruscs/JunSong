package com.junsong.system.service.signal;

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
 * 库存风险信号生成器。
 * 扫描 fin_stock_ledger 中负库存流水，按门店分组投递经营任务。
 * 门店无负库存时自动完成对应经营任务。
 *
 * 注意：fin_stock_ledger 表有 tenant_id 列，JdbcTemplate 绕过 MyBatis 拦截器，需手动加 tenant_id 条件。
 *
 * @author junsong
 */
@Component
public class StockRiskSignalGenerator implements OperatingTaskSignalGenerator
{
    private static final Logger log = LoggerFactory.getLogger(StockRiskSignalGenerator.class);

    private static final String SOURCE_MODULE = "STOCK";
    private static final String SOURCE_TYPE = "NEGATIVE_STOCK";
    private static final String SOURCE_ROUTE = "/finance/stock/health";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_DONE = "DONE";

    private static final String PRIORITY_URGENT = "URGENT";
    private static final String SEVERITY_HIGH = "HIGH";

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
        return "STOCK_RISK";
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
            Set<Long> activeNegStockDepts = new HashSet<>();
            created += scanAndCreateTasks(activeNegStockDepts);
            autoCompleteClosedSources(activeNegStockDepts);
        }
        catch (Exception e)
        {
            log.warn("库存风险信号生成器执行失败: {}", e.getMessage(), e);
        }
        return created;
    }

    /**
     * 扫描负库存门店，投递经营任务。
     * 同时收集当前有负库存的门店ID集合（供自动完成判断用）
     */
    private int scanAndCreateTasks(Set<Long> activeDepts)
    {
        Long tenantId = TenantContext.getTenantId();
        List<Long> authorizedDeptIds = authorizedDeptResolver.resolveAuthorizedDeptIds();
        String deptFilter = buildDeptFilter(authorizedDeptIds);

        String sql = "SELECT dept_id, COUNT(DISTINCT product_id) AS neg_cnt "
                + "FROM fin_stock_ledger "
                + "WHERE del_flag = '0' AND after_quantity < 0 AND tenant_id = ?"
                + deptFilter
                + " GROUP BY dept_id";

        int[] created = {0};
        jdbcTemplate.query(sql, (ResultSet rs) -> {
            Long deptId = rs.getObject("dept_id", Long.class);
            long negCnt = rs.getLong("neg_cnt");
            if (deptId == null)
            {
                return;
            }
            activeDepts.add(deptId);

            Date occurTime = new Date();
            SysOperatingTask task = new SysOperatingTask();
            task.setSourceModule(SOURCE_MODULE);
            task.setSourceType(SOURCE_TYPE);
            task.setSourceId(String.valueOf(deptId));
            task.setSourceRoute(SOURCE_ROUTE);
            task.setTaskType("NEGATIVE_STOCK");
            task.setDeptId(deptId);
            task.setTitle("负库存预警：门店" + deptId + " " + negCnt + "个商品");
            task.setPriority(PRIORITY_URGENT);
            task.setSeverity(SEVERITY_HIGH);
            task.setOccurTime(occurTime);
            task.setDueTime(dueTimeCalculator.calculateDueTime(SOURCE_TYPE, occurTime, null));

            SysOperatingTask result = operatingTaskService.createOrUpdateTask(task);
            if (result == task)
            {
                created[0]++;
            }
        }, tenantId);

        return created[0];
    }

    /**
     * 自动完成已无负库存的门店对应经营任务。
     * 查询 sys_operating_task 中 NEGATIVE_STOCK 类型且 PENDING/IN_PROGRESS 的任务，
     * 若其 source_id（dept_id）不在当前负库存门店集合中，则自动完成。
     */
    private void autoCompleteClosedSources(Set<Long> activeNegStockDepts)
    {
        Long tenantId = TenantContext.getTenantId();

        String sql = "SELECT task_id, status, version, source_id "
                + "FROM sys_operating_task "
                + "WHERE del_flag = '0' AND source_module = 'STOCK' AND source_type = 'NEGATIVE_STOCK' "
                + "AND status IN ('PENDING', 'IN_PROGRESS') AND tenant_id = ?";

        jdbcTemplate.query(sql, (ResultSet rs) -> {
            Long taskId = rs.getLong("task_id");
            String status = rs.getString("status");
            Integer version = rs.getObject("version", Integer.class);
            String sourceId = rs.getString("source_id");

            Long deptId = parseLong(sourceId);
            // 门店不在当前负库存集合中 → 来源已关闭
            if (deptId == null || !activeNegStockDepts.contains(deptId))
            {
                operatingTaskMapper.conditionalUpdateStatus(
                        taskId, status, version, STATUS_DONE,
                        null, null, "来源已关闭，自动完成", null, null);
            }
        }, tenantId);
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
