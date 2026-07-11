package com.junsong.member.controller;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.member.task.MemberGrowthEffectBackfillTask;
import com.junsong.system.api.domain.R21TaskResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * R21 会员调度任务内部接口（供 system 模块通过 Feign 调用）。
 */
@RestController
@RequestMapping("/member/inner/scheduler")
public class MemberSchedulerInnerController
{
    @Autowired
    private MemberGrowthEffectBackfillTask memberGrowthEffectBackfillTask;

    @InnerAuth
    @PostMapping("/growth-effect-backfill")
    public R<R21TaskResult> growthEffectBackfill(@RequestHeader(SecurityConstants.FROM_SOURCE) String source)
    {
        return R.ok(memberGrowthEffectBackfillTask.execute());
    }
}
