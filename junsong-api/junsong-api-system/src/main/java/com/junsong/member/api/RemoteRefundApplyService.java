package com.junsong.member.api;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.member.api.factory.RemoteRefundApplyFallbackFactory;
import com.junsong.member.api.domain.RefundWorkflowSyncReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(contextId = "remoteRefundApplyService", value = ServiceNameConstants.MEMBER_SERVICE, fallbackFactory = RemoteRefundApplyFallbackFactory.class)
public interface RemoteRefundApplyService
{
    @PostMapping("/refund/workflow/sync")
    R<Boolean> syncWorkflowStatus(@RequestBody RefundWorkflowSyncReq request,
                                  @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
