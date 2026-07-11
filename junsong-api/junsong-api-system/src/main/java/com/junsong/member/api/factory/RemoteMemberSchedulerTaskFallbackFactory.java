package com.junsong.member.api.factory;

import com.junsong.common.core.domain.R;
import com.junsong.member.api.RemoteMemberSchedulerTaskService;
import com.junsong.system.api.domain.R21TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 会员调度任务服务降级处理
 */
@Component
public class RemoteMemberSchedulerTaskFallbackFactory implements FallbackFactory<RemoteMemberSchedulerTaskService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteMemberSchedulerTaskFallbackFactory.class);

    @Override
    public RemoteMemberSchedulerTaskService create(Throwable throwable)
    {
        log.error("会员调度任务服务调用失败:{}", throwable.getMessage());
        return new RemoteMemberSchedulerTaskService()
        {
            @Override
            public R<R21TaskResult> growthEffectBackfill(String source)
            {
                return R.fail("会员调度任务调用失败:" + throwable.getMessage());
            }
        };
    }
}
