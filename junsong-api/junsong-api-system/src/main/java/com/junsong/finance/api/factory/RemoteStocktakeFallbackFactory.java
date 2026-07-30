package com.junsong.finance.api.factory;

import com.junsong.common.core.domain.R;
import com.junsong.finance.api.RemoteStocktakeService;
import com.junsong.finance.api.domain.StocktakeWorkflowSyncReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 盘点服务降级处理
 */
@Component
public class RemoteStocktakeFallbackFactory implements FallbackFactory<RemoteStocktakeService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteStocktakeFallbackFactory.class);

    @Override
    public RemoteStocktakeService create(Throwable throwable)
    {
        log.error("盘点服务调用失败:{}", throwable.getMessage());
        return new RemoteStocktakeService()
        {
            @Override
            public R<Boolean> syncWorkflowStatus(StocktakeWorkflowSyncReq request, String source, String idempotencyKey)
            {
                return R.fail("同步盘点流程状态失败:" + throwable.getMessage());
            }
        };
    }
}
