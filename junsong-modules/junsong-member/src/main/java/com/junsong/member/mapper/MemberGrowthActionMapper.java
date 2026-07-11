package com.junsong.member.mapper;

import com.junsong.member.domain.MemGrowthAction;
import com.junsong.member.domain.MemGrowthActionMember;
import com.junsong.member.domain.vo.GrowthActionCandidateVO;
import com.junsong.member.domain.vo.GrowthActionEffectVO;
import com.junsong.member.domain.vo.GrowthActionExecuteParams;
import com.junsong.member.domain.vo.GrowthActionQueryParams;
import com.junsong.member.domain.vo.GrowthActionRowVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 会员增长动作Mapper
 *
 * @author junsong
 */
public interface MemberGrowthActionMapper
{
    String selectLatestCashPressureLevel(@Param("deptId") Long deptId);

    List<GrowthActionCandidateVO> selectCandidates(@Param("params") GrowthActionQueryParams params);

    List<GrowthActionRowVO> selectRecentActions(@Param("params") GrowthActionQueryParams params);

    Integer countPendingActions(@Param("params") GrowthActionQueryParams params);

    Integer countPendingMembers(@Param("params") GrowthActionQueryParams params);

    Integer countExecutedMembers(@Param("params") GrowthActionQueryParams params);

    Integer insertAction(MemGrowthAction action);

    Integer insertActionMember(MemGrowthActionMember member);

    Integer updateActionStatus(MemGrowthAction action);

    Integer updateActionMemberExecute(@Param("params") GrowthActionExecuteParams params);

    /** 按动作ID查询会员明细（执行弹窗使用） */
    List<MemGrowthActionMember> selectActionMembers(@Param("actionId") Long actionId);

    /** 按动作ID统计已处理会员数（DONE/IGNORED 视为终态已处理；IN_PROGRESS 不计入） */
    Integer countExecutedByActionId(@Param("actionId") Long actionId);

    /** 按动作ID统计候选会员总数 */
    Integer countTotalByActionId(@Param("actionId") Long actionId);

    GrowthActionEffectVO selectEffectSummary(@Param("params") GrowthActionQueryParams params);

    /** R17-D：按 actionId 回查真实 7 天效果数据（fin_sale_record / mem_member_sign_in / 成长值变化） */
    GrowthActionEffectVO selectRealEffectByActionId(@Param("actionId") Long actionId);

    /** R17-D：回填真实效果标记位到明细表（用于看板静态汇总） */
    Integer updateMemberEffectFlags(@Param("actionId") Long actionId);

    /** R21：查询所有未删除动作的 actionId，供批量回填效果标记位 */
    List<Long> selectAllActionIds();
}
