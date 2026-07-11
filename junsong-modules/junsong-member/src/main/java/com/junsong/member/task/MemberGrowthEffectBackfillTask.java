package com.junsong.member.task;

import com.junsong.member.service.IMemberGrowthActionService;
import com.junsong.system.api.domain.R21TaskResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * R21 会员增长动作效果回填任务。
 * 复用 R17 mapper updateMemberEffectFlags，遍历所有未删除动作逐个回填真实效果标记位
 * （repurchased/signed_in/growth_increased）。
 * 不新建触达、动作中心或发送能力。
 */
@Component
public class MemberGrowthEffectBackfillTask
{
    public static final String JOB_CODE = "R21_MEMBER_GROWTH_EFFECT_BACKFILL";
    public static final String JOB_NAME = "会员增长动作效果回填任务";

    @Autowired
    private IMemberGrowthActionService memberGrowthActionService;

    public R21TaskResult execute()
    {
        try {
            int count = memberGrowthActionService.backfillEffectFlags();
            if (count == 0) {
                return R21TaskResult.skipped("No growth actions to backfill");
            }
            return R21TaskResult.success(count, "Backfilled effect flags for " + count + " actions");
        } catch (Exception ex) {
            return R21TaskResult.failed(ex);
        }
    }
}
