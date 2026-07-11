package com.junsong.finance.mapper;

import com.junsong.finance.domain.FinanceReviewKnowledge;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 复盘知识库Mapper接口
 *
 * @author junsong
 */
public interface FinanceReviewKnowledgeMapper {

    /**
     * 查询知识库列表
     */
    List<FinanceReviewKnowledge> selectKnowledgeList(Map<String, Object> params);

    /**
     * 根据知识ID查询
     */
    FinanceReviewKnowledge selectByKnowledgeId(Long knowledgeId);

    /**
     * 根据任务ID查询可复用知识
     */
    FinanceReviewKnowledge selectReusableByTaskId(@Param("taskId") Long taskId);

    /**
     * 新增知识
     */
    int insertKnowledge(FinanceReviewKnowledge knowledge);

    /**
     * 更新知识
     */
    int updateKnowledge(FinanceReviewKnowledge knowledge);

    /**
     * 查询最近可复用知识（按问题类型优先）
     */
    List<FinanceReviewKnowledge> selectRecentReusable(@Param("problemTypes") List<String> problemTypes,
                                                       @Param("limit") int limit);

    /**
     * 根据问题类型和授权门店推荐可复用知识
     */
    List<FinanceReviewKnowledge> selectRecommendations(@Param("problemType") String problemType,
                                                       @Param("deptId") Long deptId,
                                                       @Param("keywords") List<String> keywords,
                                                       @Param("allowedDeptIds") List<Long> allowedDeptIds,
                                                       @Param("limit") Integer limit);
}
