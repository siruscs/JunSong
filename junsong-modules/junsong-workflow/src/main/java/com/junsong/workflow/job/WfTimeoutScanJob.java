package com.junsong.workflow.job;

import com.junsong.workflow.service.timeout.WorkflowTimeoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WfTimeoutScanJob
{
    @Autowired
    private WorkflowTimeoutService timeoutService;

    @Scheduled(fixedRate = 300000) // 每5分钟扫描一次
    public void scan()
    {
        timeoutService.scanAndEscalate();
    }
}
