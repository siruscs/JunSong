package com.junsong.member.service;

import com.junsong.member.domain.vo.MemberActionPredictionVO;

import java.util.List;

/**
 * R24 会员动作预测服务接口。
 *
 * <p>可解释规则版本：基于 R17 会员动作、活跃度、历史效果做规则打分，
 * 不引入机器学习或黑盒评分。</p>
 */
public interface IMemberActionPredictionService {

    /**
     * 计算某门店/动作类型/时间窗口下的会员动作预测信号列表。
     */
    List<MemberActionPredictionVO> listActionPredictions(Long deptId, Integer windowDays, String actionType);
}
