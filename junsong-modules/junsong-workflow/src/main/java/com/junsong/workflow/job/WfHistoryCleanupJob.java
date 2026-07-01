package com.junsong.workflow.job;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WfHistoryCleanupJob
{
    private static final Logger log = LoggerFactory.getLogger(WfHistoryCleanupJob.class);

    private static final int RETENTION_DAYS = 90;

    @Autowired
    private HistoryService historyService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup()
    {
        Date threshold = Date.from(Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS));
        List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .startedBefore(threshold)
                .list();
        int count = 0;
        for (HistoricProcessInstance instance : instances)
        {
            historyService.deleteHistoricProcessInstance(instance.getId());
            count++;
        }
        log.info("历史数据归档清理完成，共清理 {} 条超过 {} 天的已结束流程实例历史数据", count, RETENTION_DAYS);
    }
}
