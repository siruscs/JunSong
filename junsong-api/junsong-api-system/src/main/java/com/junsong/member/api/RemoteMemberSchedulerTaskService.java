package com.junsong.member.api;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.domain.R21TaskResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * R21 会员调度任务远程调用接口（system → member）。
 */
@FeignClient(contextId = "remoteMemberSchedulerTaskService", value = ServiceNameConstants.MEMBER_SERVICE)
public interface RemoteMemberSchedulerTaskService
{
    @PostMapping("/member/inner/scheduler/growth-effect-backfill")
    R<R21TaskResult> growthEffectBackfill(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
