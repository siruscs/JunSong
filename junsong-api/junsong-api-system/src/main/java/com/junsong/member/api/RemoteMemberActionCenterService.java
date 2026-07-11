package com.junsong.member.api;

import java.util.List;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.domain.ActionCenterSourceItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(contextId = "remoteMemberActionCenterService", value = ServiceNameConstants.MEMBER_SERVICE)
public interface RemoteMemberActionCenterService {
    @GetMapping("/member/inner/action-center/items")
    R<List<ActionCenterSourceItem>> listMemberActions(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
