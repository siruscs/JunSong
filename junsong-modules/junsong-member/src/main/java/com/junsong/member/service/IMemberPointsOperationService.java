package com.junsong.member.service;

import com.junsong.member.domain.vo.MemberPointsOperationSummaryVO;

/**
 * 积分经营摘要服务：提供积分沉淀、兑换成本和高积分会员风险清单。
 */
public interface IMemberPointsOperationService {
    /**
     * 获取积分经营摘要，按授权门店过滤。
     *
     * @param deptIds 指定门店（null 表示查全部授权门店）
     * @return 积分经营摘要
     */
    MemberPointsOperationSummaryVO getPointsOperationSummary(java.util.List<Long> deptIds);
}
