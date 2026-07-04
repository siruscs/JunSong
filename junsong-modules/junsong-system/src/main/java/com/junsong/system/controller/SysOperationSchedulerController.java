package com.junsong.system.controller;

import java.util.List;

import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.system.api.domain.R21TaskResult;
import com.junsong.system.domain.vo.OperationScheduleLogVO;
import com.junsong.system.domain.vo.OperationScheduleTriggerResultVO;
import com.junsong.system.scheduler.R21SchedulerExecutor;
import com.junsong.system.service.ISysOperationScheduleLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * R21 运维调度管理控制器。
 * 提供调度看板、最近日志查询和手动触发接口。
 * trigger 委托给 R21SchedulerExecutor 执行 dispatch+log，统一在 system 侧记录调度日志。
 * 业务 FAILED/PARTIAL 时 HTTP 返回 error，日志仍落 FAILED/PARTIAL。
 */
@RestController
@RequestMapping("/operation-scheduler")
public class SysOperationSchedulerController
{
    private static final java.util.Set<String> VALID_JOB_CODES = java.util.Set.of(
            "R21_CASHFLOW_FORECAST_SNAPSHOT",
            "R21_MEMBER_GROWTH_EFFECT_BACKFILL",
            "R21_STOCK_DAILY_SNAPSHOT",
            "R21_OPERATION_MEMO_DRAFT"
    );

    private final ISysOperationScheduleLogService service;
    private final R21SchedulerExecutor executor;

    public SysOperationSchedulerController(ISysOperationScheduleLogService service,
                                           R21SchedulerExecutor executor)
    {
        this.service = service;
        this.executor = executor;
    }

    @RequiresPermissions("system:operation-scheduler:view")
    @GetMapping("/dashboard")
    public AjaxResult dashboard()
    {
        return AjaxResult.success(service.getDashboard());
    }

    @RequiresPermissions("system:operation-scheduler:view")
    @GetMapping("/recent")
    public AjaxResult recent(
            @RequestParam(required = false) String jobCode,
            @RequestParam(defaultValue = "20") int limit)
    {
        return AjaxResult.success(service.listRecent(jobCode, limit));
    }

    /**
     * 手动触发指定任务。
     * jobCode 白名单校验，触发失败落 FAILED，不绕过权限。
     * 业务 FAILED/PARTIAL 时 HTTP 返回 error（日志仍落 FAILED/PARTIAL）。
     */
    @RequiresPermissions("system:operation-scheduler:trigger")
    @PostMapping("/{jobCode}/trigger")
    public AjaxResult trigger(@PathVariable String jobCode)
    {
        if (!VALID_JOB_CODES.contains(jobCode))
        {
            return AjaxResult.error("无效的任务编码: " + jobCode);
        }

        R21TaskResult result = executor.execute(jobCode, "MANUAL");
        OperationScheduleTriggerResultVO vo = buildTriggerResult(jobCode);

        if (result != null && ("FAILED".equals(result.getStatus()) || "PARTIAL".equals(result.getStatus())))
        {
            return AjaxResult.error("调度任务执行失败: " + (result.getErrorMessage() != null ? result.getErrorMessage() : result.getResultSummary()), vo);
        }
        return AjaxResult.success(vo);
    }

    private OperationScheduleTriggerResultVO buildTriggerResult(String jobCode)
    {
        List<OperationScheduleLogVO> recent = service.listRecent(jobCode, 1);
        OperationScheduleTriggerResultVO vo = new OperationScheduleTriggerResultVO();
        vo.setJobCode(jobCode);
        if (recent != null && !recent.isEmpty())
        {
            OperationScheduleLogVO latest = recent.get(0);
            vo.setLogId(latest.getLogId());
            vo.setStatus(latest.getStatus());
            vo.setResultSummary(latest.getResultSummary());
            vo.setErrorMessage(latest.getErrorMessage());
        }
        else
        {
            vo.setStatus("UNKNOWN");
        }
        return vo;
    }
}
