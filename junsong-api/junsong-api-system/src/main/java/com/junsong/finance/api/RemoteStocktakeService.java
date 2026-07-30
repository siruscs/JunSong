package com.junsong.finance.api;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.finance.api.factory.RemoteStocktakeFallbackFactory;
import com.junsong.finance.api.domain.StocktakeWorkflowSyncReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(contextId = "remoteStocktakeService", value = ServiceNameConstants.FINANCE_SERVICE, fallbackFactory = RemoteStocktakeFallbackFactory.class)
public interface RemoteStocktakeService
{
    @PostMapping("/stocktakes/internal/workflow/sync")
    R<Boolean> syncWorkflowStatus(@RequestBody StocktakeWorkflowSyncReq request,
                                  @RequestHeader(SecurityConstants.FROM_SOURCE) String source,
                                  @RequestHeader("X-Idempotency-Key") String idempotencyKey);
}
