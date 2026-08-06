package com.junsong.member.domain;

import java.math.BigDecimal;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;

/**
 * 会员卡类型对象 mem_member_card_type
 * 兼作会员等级配置，含 min_growth 升级门槛
 *
 * @author junsong
 */
public class MemMemberCardType extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 类型ID */
    private Long typeId;

    /** 租户ID */
    private Long tenantId;

    /** 机构ID，0表示租户级基线配置 */
    private Long deptId;

    /** 类型名称 */
    @Excel(name = "等级名称")
    private String typeName;

    /** 类型代码 */
    @Excel(name = "等级编码")
    private String typeCode;

    /** 办卡费用 */
    @Excel(name = "办卡费用", cellType = ColumnType.NUMERIC)
    private BigDecimal cardFee;

    /** 折扣率 */
    @Excel(name = "折扣率", cellType = ColumnType.NUMERIC)
    private BigDecimal discountRate;

    /** 积分倍率 */
    @Excel(name = "积分倍率", cellType = ColumnType.NUMERIC)
    private BigDecimal pointsRate;

    /** 升级所需最低成长值 */
    @Excel(name = "升级成长值", cellType = ColumnType.NUMERIC)
    private Long minGrowth;

    /** 签到奖励积分（等级专属，为空则用 mem_growth_rule.sign_in_points 兜底） */
    @Excel(name = "签到积分", cellType = ColumnType.NUMERIC)
    private BigDecimal signInPoints;

    /** 状态(0正常/1禁用) */
    @Excel(name = "状态", readConverterExp = "0=正常,1=禁用")
    private String status;

    /** 删除标志 */
    private String delFlag;

    public Long getTypeId()
    {
        return typeId;
    }

    public void setTypeId(Long typeId)
    {
        this.typeId = typeId;
    }

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getTypeName()
    {
        return typeName;
    }

    public void setTypeName(String typeName)
    {
        this.typeName = typeName;
    }

    public String getTypeCode()
    {
        return typeCode;
    }

    public void setTypeCode(String typeCode)
    {
        this.typeCode = typeCode;
    }

    public BigDecimal getCardFee()
    {
        return cardFee;
    }

    public void setCardFee(BigDecimal cardFee)
    {
        this.cardFee = cardFee;
    }

    public BigDecimal getDiscountRate()
    {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate)
    {
        this.discountRate = discountRate;
    }

    public BigDecimal getPointsRate()
    {
        return pointsRate;
    }

    public void setPointsRate(BigDecimal pointsRate)
    {
        this.pointsRate = pointsRate;
    }

    public Long getMinGrowth()
    {
        return minGrowth;
    }

    public void setMinGrowth(Long minGrowth)
    {
        this.minGrowth = minGrowth;
    }

    public BigDecimal getSignInPoints()
    {
        return signInPoints;
    }

    public void setSignInPoints(BigDecimal signInPoints)
    {
        this.signInPoints = signInPoints;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }
}
