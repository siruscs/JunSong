package com.junsong.finance.api;

import java.util.List;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.domain.ActionCenterSourceItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(contextId = "remoteFinanceActionCenterService", value = ServiceNameConstants.FINANCE_SERVICE)
public interface RemoteFinanceActionCenterService {
    @GetMapping("/finance/inner/action-center/items")
    R<List<ActionCenterSourceItem>> listFinanceActions(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
