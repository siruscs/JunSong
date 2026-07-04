package com.junsong.system.scheduler;

import com.junsong.system.api.domain.R21TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * R21 定时调度器。
 * 使用 Spring @Scheduled 为 4 个运维任务提供定时触发入口。
 * 复用 R21SchedulerExecutor 的 start→dispatch→finish 逻辑，日志统一落 sys_operation_schedule_log。
 */
@Component
public class R21ScheduledDispatcher
{
    private static final Logger log = LoggerFactory.getLogger(R21ScheduledDispatcher.class);

    private final R21SchedulerExecutor executor;

    public R21ScheduledDispatcher(R21SchedulerExecutor executor)
    {
        this.executor = executor;
    }

    /**
     * 现金流预测快照：每日 00:30 执行
     */
    @Scheduled(cron = "0 30 0 * * ?")
    public void cashflowForecastSnapshot()
    {
        runJob("R21_CASHFLOW_FORECAST_SNAPSHOT");
    }

    /**
     * 会员增长效果回填：每日 01:00 执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void memberGrowthEffectBackfill()
    {
        runJob("R21_MEMBER_GROWTH_EFFECT_BACKFILL");
    }

    /**
     * 库存每日快照：每日 02:00 执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void stockDailySnapshot()
    {
        runJob("R21_STOCK_DAILY_SNAPSHOT");
    }

    /**
     * 经营纪要草稿：每日 08:00 执行
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void operationMemoDraft()
    {
        runJob("R21_OPERATION_MEMO_DRAFT");
    }

    private void runJob(String jobCode)
    {
        log.info("[R21-Scheduled] 开始执行: {}", jobCode);
        try
        {
            R21TaskResult result = executor.execute(jobCode, "SCHEDULED");
            log.info("[R21-Scheduled] {} 完成: status={}, summary={}",
                    jobCode, result.getStatus(), result.getResultSummary());
        }
        catch (Exception e)
        {
            log.error("[R21-Scheduled] {} 异常: {}", jobCode, e.getMessage(), e);
        }
    }
}
