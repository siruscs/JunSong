package com.junsong.member.service;

import com.junsong.member.domain.vo.MemberOperationMetrics;
import com.junsong.member.domain.vo.MemberOperationSuggestionVO;

import java.util.List;

/**
 * 会员经营建议服务，基于确定性规则生成建议，供概览建议区和复盘任务使用。
 */
public interface IMemberOperationSuggestionService {
    List<MemberOperationSuggestionVO> generateSuggestions(MemberOperationMetrics metrics);
}
