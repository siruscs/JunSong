package com.junsong.finance.service;

import com.junsong.finance.domain.FinanceReviewKnowledge;

import java.util.List;
import java.util.Map;

/**
 * 复盘知识库Service接口
 *
 * @author junsong
 */
public interface IFinanceReviewKnowledgeService {

    /**
     * 查询知识库列表
     */
    List<FinanceReviewKnowledge> listKnowledge(Map<String, Object> params);

    /**
     * 根据ID查询知识
     */
    FinanceReviewKnowledge getByKnowledgeId(Long knowledgeId);

    /**
     * 新增知识
     */
    int addKnowledge(FinanceReviewKnowledge knowledge);

    /**
     * 更新知识
     */
    int updateKnowledge(FinanceReviewKnowledge knowledge);

    /**
     * 从复盘任务沉淀知识
     *
     * @param taskId 复盘任务ID
     * @param body   用户输入（problemType/rootCause/resultSummary 覆盖）
     * @return 新建知识
     */
    FinanceReviewKnowledge createFromTask(Long taskId, Map<String, String> body);

    /**
     * 查询最近可复用知识
     */
    List<FinanceReviewKnowledge> getRecentReusable(List<String> problemTypes, int limit);

    /**
     * 为指定任务推荐历史知识
     *
     * @param taskId 复盘任务ID
     * @return 推荐的知识列表（最多5条）
     */
    List<FinanceReviewKnowledge> recommendForTask(Long taskId);
}
