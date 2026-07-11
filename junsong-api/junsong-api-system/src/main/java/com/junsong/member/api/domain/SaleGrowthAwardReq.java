package com.junsong.member.api.domain;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 销售消费奖励入账请求
 *
 * @author junsong
 */
public class SaleGrowthAwardReq implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    /** 会员ID */
    private Long memberId;

    /** 会员编号 */
    private String memberNo;

    /** 会员姓名 */
    private String memberName;

    /** 部门ID */
    private Long deptId;

    /** 销售单ID（幂等来源） */
    private Long saleId;

    /** 消费金额 */
    private BigDecimal saleAmount;

    /** 操作人 */
    private String operator;

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public String getMemberNo()
    {
        return memberNo;
    }

    public void setMemberNo(String memberNo)
    {
        this.memberNo = memberNo;
    }

    public String getMemberName()
    {
        return memberName;
    }

    public void setMemberName(String memberName)
    {
        this.memberName = memberName;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getSaleId()
    {
        return saleId;
    }

    public void setSaleId(Long saleId)
    {
        this.saleId = saleId;
    }

    public BigDecimal getSaleAmount()
    {
        return saleAmount;
    }

    public void setSaleAmount(BigDecimal saleAmount)
    {
        this.saleAmount = saleAmount;
    }

    public String getOperator()
    {
        return operator;
    }

    public void setOperator(String operator)
    {
        this.operator = operator;
    }
}
