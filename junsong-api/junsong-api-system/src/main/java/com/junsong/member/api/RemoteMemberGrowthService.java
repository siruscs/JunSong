package com.junsong.member.api;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.member.api.domain.SaleGrowthAwardReq;
import com.junsong.member.api.factory.RemoteMemberGrowthFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 会员成长值远程调用接口
 *
 * @author junsong
 */
@FeignClient(contextId = "remoteMemberGrowthService", value = ServiceNameConstants.MEMBER_SERVICE, fallbackFactory = RemoteMemberGrowthFallbackFactory.class)
public interface RemoteMemberGrowthService
{
    /**
     * 销售消费奖励入账（内部调用）
     * 幂等键: SALE:{saleId}
     *
     * @param request 销售奖励入账请求
     * @param source  内部调用来源标识
     * @return 是否入账成功（已处理返回true但不重复入账）
     */
    @PostMapping("/growth/inner/awardSale")
    R<Boolean> awardSaleGrowth(@RequestBody SaleGrowthAwardReq request,
                               @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
