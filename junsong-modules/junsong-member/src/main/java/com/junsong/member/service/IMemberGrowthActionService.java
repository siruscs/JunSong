package com.junsong.member.service;

import com.junsong.member.domain.vo.GrowthActionDashboardVO;
import com.junsong.member.domain.vo.GrowthActionEffectVO;
import com.junsong.member.domain.vo.GrowthActionExecuteParams;
import com.junsong.member.domain.vo.GrowthActionGenerateParams;
import com.junsong.member.domain.MemGrowthActionMember;
import com.junsong.member.domain.vo.GrowthActionCandidateVO;
import com.junsong.member.domain.vo.GrowthActionQueryParams;

import java.util.List;

/**
 * 会员增长动作Service接口
 *
 * @author junsong
 */
public interface IMemberGrowthActionService
{
    GrowthActionDashboardVO getDashboard(GrowthActionQueryParams params);

    List<GrowthActionCandidateVO> listCandidates(GrowthActionQueryParams params);

    int generateAction(GrowthActionGenerateParams params);

    int executeAction(GrowthActionExecuteParams params);

    /** 按动作ID查询会员明细（执行弹窗使用） */
    List<MemGrowthActionMember> listActionMembers(Long actionId);

    GrowthActionEffectVO getEffect(GrowthActionQueryParams params);

    /**
     * R21：批量回填所有动作的效果标记位（repurchased/signed_in/growth_increased）。
     * 复用 R17 mapper updateMemberEffectFlags，遍历所有未删除动作逐个回填。
     *
     * @return 已处理的动作数量
     */
    int backfillEffectFlags();
}
