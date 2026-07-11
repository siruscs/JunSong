package com.junsong.member.domain;

import java.math.BigDecimal;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 会员成长规则对象 mem_growth_rule
 * 含签到规则、消费成长倍率、衰减规则
 *
 * @author junsong
 */
public class MemGrowthRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 规则ID */
    private Long ruleId;

    /** 租户ID */
    private Long tenantId;

    /** 签到获得积分 */
    @Excel(name = "签到积分", cellType = ColumnType.NUMERIC)
    private BigDecimal signInPoints;

    /** 签到获得成长值 */
    @Excel(name = "签到成长值", cellType = ColumnType.NUMERIC)
    private Long signInGrowth;

    /** 消费成长值倍率 */
    @Excel(name = "消费成长倍率", cellType = ColumnType.NUMERIC)
    private BigDecimal saleGrowthRatio;

    /** 是否启用衰减 0否 1是 */
    @Excel(name = "启用衰减", readConverterExp = "0=否,1=是")
    private String decayEnabled;

    /** 不活跃天数阈值 */
    @Excel(name = "不活跃天数", cellType = ColumnType.NUMERIC)
    private Integer inactiveDays;

    /** 衰减比例 */
    @Excel(name = "衰减比例", cellType = ColumnType.NUMERIC)
    private BigDecimal decayRatio;

    public Long getRuleId()
    {
        return ruleId;
    }

    public void setRuleId(Long ruleId)
    {
        this.ruleId = ruleId;
    }

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    public BigDecimal getSignInPoints()
    {
        return signInPoints;
    }

    public void setSignInPoints(BigDecimal signInPoints)
    {
        this.signInPoints = signInPoints;
    }

    public Long getSignInGrowth()
    {
        return signInGrowth;
    }

    public void setSignInGrowth(Long signInGrowth)
    {
        this.signInGrowth = signInGrowth;
    }

    public BigDecimal getSaleGrowthRatio()
    {
        return saleGrowthRatio;
    }

    public void setSaleGrowthRatio(BigDecimal saleGrowthRatio)
    {
        this.saleGrowthRatio = saleGrowthRatio;
    }

    public String getDecayEnabled()
    {
        return decayEnabled;
    }

    public void setDecayEnabled(String decayEnabled)
    {
        this.decayEnabled = decayEnabled;
    }

    public Integer getInactiveDays()
    {
        return inactiveDays;
    }

    public void setInactiveDays(Integer inactiveDays)
    {
        this.inactiveDays = inactiveDays;
    }

    public BigDecimal getDecayRatio()
    {
        return decayRatio;
    }

    public void setDecayRatio(BigDecimal decayRatio)
    {
        this.decayRatio = decayRatio;
    }
}
