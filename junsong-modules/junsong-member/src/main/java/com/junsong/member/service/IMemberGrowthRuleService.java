package com.junsong.member.service;

import java.util.List;
import com.junsong.member.domain.MemGrowthRule;

/**
 * 会员成长规则Service接口
 *
 * @author junsong
 */
public interface IMemberGrowthRuleService
{
    /**
     * 查询当前租户成长规则
     */
    public MemGrowthRule getGrowthRule();

    /**
     * 查询所有启用了衰减的租户规则
     */
    public List<MemGrowthRule> selectDecayEnabledRules();

    /**
     * 修改成长规则
     */
    public int updateGrowthRule(MemGrowthRule rule);
}
