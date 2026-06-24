package com.junsong.system.api;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.domain.StoreOpeningWorkflowSyncReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(contextId = "remoteStoreOpeningService", value = ServiceNameConstants.SYSTEM_SERVICE)
public interface RemoteStoreOpeningService
{
    @PostMapping("/storeOpening/workflow/sync")
    R<Boolean> syncWorkflowStatus(@RequestBody StoreOpeningWorkflowSyncReq request,
                                  @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
