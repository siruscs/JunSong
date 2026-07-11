package com.junsong.member.mapper;

import com.junsong.member.domain.MemGrowthRule;

/**
 * 会员成长规则Mapper接口
 *
 * @author junsong
 */
public interface MemGrowthRuleMapper
{
    /**
     * 查询租户成长规则
     */
    public MemGrowthRule selectGrowthRuleByTenantId(Long tenantId);

    /**
     * 查询所有启用了衰减的租户规则
     */
    public java.util.List<MemGrowthRule> selectDecayEnabledRules();

    /**
     * 新增成长规则
     */
    public int insertGrowthRule(MemGrowthRule rule);

    /**
     * 修改成长规则
     */
    public int updateGrowthRule(MemGrowthRule rule);
}
