package com.junsong.member.api.factory;

import com.junsong.common.core.domain.R;
import com.junsong.member.api.RemoteMemberGrowthService;
import com.junsong.member.api.domain.SaleGrowthAwardReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 会员成长值服务降级处理
 *
 * @author junsong
 */
@Component
public class RemoteMemberGrowthFallbackFactory implements FallbackFactory<RemoteMemberGrowthService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteMemberGrowthFallbackFactory.class);

    @Override
    public RemoteMemberGrowthService create(Throwable throwable)
    {
        log.error("会员成长值服务调用失败:{}", throwable.getMessage());
        return new RemoteMemberGrowthService()
        {
            @Override
            public R<Boolean> awardSaleGrowth(SaleGrowthAwardReq request, String source)
            {
                return R.fail("会员成长值入账失败:" + throwable.getMessage());
            }
        };
    }
}
