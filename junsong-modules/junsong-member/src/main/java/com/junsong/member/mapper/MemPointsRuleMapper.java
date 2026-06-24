package com.junsong.member.mapper;

import com.junsong.member.domain.MemPointsRule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 积分规则Mapper接口
 */
@Mapper
public interface MemPointsRuleMapper {

    /**
     * 查询积分规则列表
     *
     * @param rule 积分规则信息
     * @return 积分规则列表
     */
    List<MemPointsRule> selectMemPointsRuleList(MemPointsRule rule);

    /**
     * 根据ID查询积分规则
     *
     * @param ruleId 积分规则ID
     * @return 积分规则信息
     */
    MemPointsRule selectMemPointsRuleById(Long ruleId);

    /**
     * 插入积分规则
     *
     * @param rule 积分规则信息
     * @return 影响行数
     */
    int insertMemPointsRule(MemPointsRule rule);

    /**
     * 更新积分规则
     *
     * @param rule 积分规则信息
     * @return 影响行数
     */
    int updateMemPointsRule(MemPointsRule rule);

    /**
     * 删除积分规则
     *
     * @param ruleId 积分规则ID
     * @return 影响行数
     */
    int deleteMemPointsRuleById(Long ruleId);

    /**
     * 批量删除积分规则
     *
     * @param ruleIds 积分规则ID数组
     * @return 影响行数
     */
    int deleteMemPointsRuleByIds(Long[] ruleIds);

    /**
     * 校验规则代码唯一性（修改时排除自身）
     *
     * @param memPointsRule 积分规则对象
     * @return 规则数量
     */
    int checkMemRuleCodeUnique(MemPointsRule memPointsRule);

    /**
     * 查询第一条生效的积分规则
     *
     * @return 生效的积分规则
     */
    MemPointsRule selectEffectiveRule();
}
