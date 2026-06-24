package com.junsong.member.api.factory;

import com.junsong.common.core.domain.R;
import com.junsong.member.api.RemoteRefundApplyService;
import com.junsong.member.api.domain.RefundWorkflowSyncReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 退款申请服务降级处理
 */
@Component
public class RemoteRefundApplyFallbackFactory implements FallbackFactory<RemoteRefundApplyService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteRefundApplyFallbackFactory.class);

    @Override
    public RemoteRefundApplyService create(Throwable throwable)
    {
        log.error("退款申请服务调用失败:{}", throwable.getMessage());
        return new RemoteRefundApplyService()
        {
            @Override
            public R<Boolean> syncWorkflowStatus(RefundWorkflowSyncReq request, String source)
            {
                return R.fail("同步退款流程状态失败:" + throwable.getMessage());
            }
        };
    }
}
