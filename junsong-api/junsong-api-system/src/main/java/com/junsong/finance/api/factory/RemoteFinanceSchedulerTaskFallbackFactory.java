package com.junsong.finance.api.factory;

import com.junsong.common.core.domain.R;
import com.junsong.finance.api.RemoteFinanceSchedulerTaskService;
import com.junsong.system.api.domain.R21TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 财务调度任务服务降级处理
 */
@Component
public class RemoteFinanceSchedulerTaskFallbackFactory implements FallbackFactory<RemoteFinanceSchedulerTaskService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteFinanceSchedulerTaskFallbackFactory.class);

    @Override
    public RemoteFinanceSchedulerTaskService create(Throwable throwable)
    {
        log.error("财务调度任务服务调用失败:{}", throwable.getMessage());
        return new RemoteFinanceSchedulerTaskService()
        {
            @Override
            public R<R21TaskResult> cashflowSnapshot(String source)
            {
                return R.fail("财务调度任务调用失败:" + throwable.getMessage());
            }

            @Override
            public R<R21TaskResult> stockSnapshot(String source)
            {
                return R.fail("财务调度任务调用失败:" + throwable.getMessage());
            }

            @Override
            public R<R21TaskResult> memoDraft(String periodType, String source)
            {
                return R.fail("财务调度任务调用失败:" + throwable.getMessage());
            }
        };
    }
}
