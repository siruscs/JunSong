package com.junsong.system.scheduler;

import java.util.List;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.finance.api.RemoteFinanceSchedulerTaskService;
import com.junsong.member.api.RemoteMemberSchedulerTaskService;
import com.junsong.system.api.domain.R21TaskResult;
import com.junsong.system.domain.vo.OperationScheduleLogVO;
import com.junsong.system.service.ISysOperationScheduleLogService;
import org.springframework.stereotype.Component;

/**
 * R21 调度任务执行器。
 * 封装 start→dispatch→finish 逻辑，供手动触发（controller）和定时触发（dispatcher）复用。
 */
@Component
public class R21SchedulerExecutor
{
    private final ISysOperationScheduleLogService service;
    private final RemoteFinanceSchedulerTaskService financeSchedulerTaskService;
    private final RemoteMemberSchedulerTaskService memberSchedulerTaskService;

    public R21SchedulerExecutor(ISysOperationScheduleLogService service,
                                RemoteFinanceSchedulerTaskService financeSchedulerTaskService,
                                RemoteMemberSchedulerTaskService memberSchedulerTaskService)
    {
        this.service = service;
        this.financeSchedulerTaskService = financeSchedulerTaskService;
        this.memberSchedulerTaskService = memberSchedulerTaskService;
    }

    /**
     * 执行调度任务：创建 RUNNING 日志 → Feign 派发 → 根据结果落 SUCCESS/FAILED/PARTIAL/SKIPPED 日志。
     *
     * @param jobCode     任务编码
     * @param triggerType 触发类型（MANUAL / SCHEDULED）
     * @return 任务执行结果（含最终状态）
     */
    public R21TaskResult execute(String jobCode, String triggerType)
    {
        OperationScheduleLogVO logVO = service.start(jobCode, jobCode, triggerType);
        Long logId = logVO.getLogId();
        try
        {
            R21TaskResult result = dispatch(jobCode);
            if (result == null)
            {
                service.finishFailed(logId, new RuntimeException("调度任务返回空结果"));
                return failedResult("调度任务返回空结果");
            }
            switch (result.getStatus())
            {
                case "SUCCESS":
                    service.finishSuccess(logId, result.getProcessedCount(), result.getResultSummary());
                    break;
                case "SKIPPED":
                    service.finishSkipped(logId, result.getResultSummary());
                    break;
                case "PARTIAL":
                    service.finishPartial(logId, result.getProcessedCount(),
                            result.getResultSummary(), result.getErrorMessage());
                    break;
                case "FAILED":
                    service.finishFailed(logId, new RuntimeException(result.getErrorMessage()));
                    break;
                default:
                    service.finishFailed(logId, new RuntimeException("未知状态: " + result.getStatus()));
                    return failedResult("未知状态: " + result.getStatus());
            }
            return result;
        }
        catch (Exception ex)
        {
            service.finishFailed(logId, ex);
            return R21TaskResult.failed(ex);
        }
    }

    /**
     * 根据 jobCode 路由到对应微服务的内部 task endpoint。
     */
    private R21TaskResult dispatch(String jobCode)
    {
        switch (jobCode)
        {
            case "R21_CASHFLOW_FORECAST_SNAPSHOT":
                return financeSchedulerTaskService.cashflowSnapshot(SecurityConstants.INNER).getData();
            case "R21_STOCK_DAILY_SNAPSHOT":
                return financeSchedulerTaskService.stockSnapshot(SecurityConstants.INNER).getData();
            case "R21_OPERATION_MEMO_DRAFT":
                return financeSchedulerTaskService.memoDraft("DAILY", SecurityConstants.INNER).getData();
            case "R21_MEMBER_GROWTH_EFFECT_BACKFILL":
                return memberSchedulerTaskService.growthEffectBackfill(SecurityConstants.INNER).getData();
            default:
                throw new IllegalArgumentException("无效的任务编码: " + jobCode);
        }
    }

    private R21TaskResult failedResult(String message)
    {
        return R21TaskResult.failed(new RuntimeException(message));
    }
}
