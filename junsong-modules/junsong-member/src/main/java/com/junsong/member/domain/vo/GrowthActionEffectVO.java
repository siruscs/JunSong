package com.junsong.member.domain.vo;

import java.math.BigDecimal;

/**
 * 增长动作效果VO
 *
 * @author junsong
 */
public class GrowthActionEffectVO
{
    private Integer totalMemberCount;
    private Integer repurchaseMemberCount;
    private Integer signInMemberCount;
    private Integer growthIncreasedMemberCount;
    private Integer effectiveMemberCount;
    private BigDecimal effectRate;

    public Integer getTotalMemberCount() { return totalMemberCount; }
    public void setTotalMemberCount(Integer totalMemberCount) { this.totalMemberCount = totalMemberCount; }

    public Integer getRepurchaseMemberCount() { return repurchaseMemberCount; }
    public void setRepurchaseMemberCount(Integer repurchaseMemberCount) { this.repurchaseMemberCount = repurchaseMemberCount; }

    public Integer getSignInMemberCount() { return signInMemberCount; }
    public void setSignInMemberCount(Integer signInMemberCount) { this.signInMemberCount = signInMemberCount; }

    public Integer getGrowthIncreasedMemberCount() { return growthIncreasedMemberCount; }
    public void setGrowthIncreasedMemberCount(Integer growthIncreasedMemberCount) { this.growthIncreasedMemberCount = growthIncreasedMemberCount; }

    public Integer getEffectiveMemberCount() { return effectiveMemberCount; }
    public void setEffectiveMemberCount(Integer effectiveMemberCount) { this.effectiveMemberCount = effectiveMemberCount; }

    public BigDecimal getEffectRate() { return effectRate; }
    public void setEffectRate(BigDecimal effectRate) { this.effectRate = effectRate; }
}
