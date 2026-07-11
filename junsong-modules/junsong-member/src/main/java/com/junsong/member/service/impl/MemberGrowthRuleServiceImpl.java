package com.junsong.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.member.domain.MemGrowthRule;
import com.junsong.member.mapper.MemGrowthRuleMapper;
import com.junsong.member.service.IMemberGrowthRuleService;

/**
 * 会员成长规则Service实现
 *
 * @author junsong
 */
@Service
public class MemberGrowthRuleServiceImpl implements IMemberGrowthRuleService
{
    @Autowired
    private MemGrowthRuleMapper growthRuleMapper;

    @Override
    public MemGrowthRule getGrowthRule()
    {
        MemGrowthRule rule = growthRuleMapper.selectGrowthRuleByTenantId(1L);
        if (rule == null)
        {
            // 租户无规则时自动创建默认规则
            rule = new MemGrowthRule();
            rule.setTenantId(1L);
            rule.setSignInPoints(new java.math.BigDecimal("1.00"));
            rule.setSignInGrowth(5L);
            rule.setSaleGrowthRatio(new java.math.BigDecimal("1.00"));
            rule.setDecayEnabled("0");
            rule.setInactiveDays(180);
            rule.setDecayRatio(new java.math.BigDecimal("0.50"));
            rule.setCreateBy("system");
            growthRuleMapper.insertGrowthRule(rule);
        }
        return rule;
    }

    @Override
    public List<MemGrowthRule> selectDecayEnabledRules()
    {
        return growthRuleMapper.selectDecayEnabledRules();
    }

    @Override
    public int updateGrowthRule(MemGrowthRule rule)
    {
        MemGrowthRule existing = getGrowthRule();
        rule.setRuleId(existing.getRuleId());
        rule.setSignInPoints(existing.getSignInPoints());
        return growthRuleMapper.updateGrowthRule(rule);
    }
}
