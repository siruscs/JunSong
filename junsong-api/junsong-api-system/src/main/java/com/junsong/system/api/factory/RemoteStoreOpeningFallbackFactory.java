package com.junsong.system.api.factory;

import com.junsong.common.core.domain.R;
import com.junsong.system.api.RemoteStoreOpeningService;
import com.junsong.system.api.domain.StoreOpeningWorkflowSyncReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteStoreOpeningFallbackFactory implements FallbackFactory<RemoteStoreOpeningService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteStoreOpeningFallbackFactory.class);

    @Override
    public RemoteStoreOpeningService create(Throwable throwable)
    {
        log.error("门店开业申请服务调用失败:{}", throwable.getMessage());
        return new RemoteStoreOpeningService()
        {
            @Override
            public R<Boolean> syncWorkflowStatus(StoreOpeningWorkflowSyncReq request, String source)
            {
                return R.fail("同步门店开业流程状态失败:" + throwable.getMessage());
            }
        };
    }
}
