package com.junsong.member.service;

import com.junsong.member.domain.vo.MemberActivityRoiVO;
import java.util.List;

/**
 * 会员活动 ROI 服务接口
 */
public interface IMemberActivityRoiService {
    /**
     * 获取活动 ROI 列表
     *
     * @param deptId     部门ID（可选，用于过滤特定门店的活动）
     * @param activityId 活动ID（可选，用于查询特定活动）
     * @return 活动 ROI 列表
     */
    List<MemberActivityRoiVO> getActivityRoiList(Long deptId, Long activityId);
}
