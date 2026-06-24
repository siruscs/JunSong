package com.junsong.member.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 积分兑换记录表对象 mem_points_exchange
 *
 * @author junsong
 */
public class MemPointsExchange extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 兑换ID */
    @Excel(name = "兑换ID", cellType = ColumnType.NUMERIC)
    private Long exchangeId;

    /** 部门ID */
    private Long deptId;

    /** 兑换单号 */
    @Excel(name = "兑换单号")
    @NotBlank(message = "兑换单号不能为空")
    @Size(max = 64, message = "兑换单号长度不能超过64个字符")
    private String exchangeNo;

    /** 会员ID */
    @Excel(name = "会员ID", cellType = ColumnType.NUMERIC)
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    /** 会员编号 */
    @Excel(name = "会员编号")
    @Size(max = 64, message = "会员编号长度不能超过64个字符")
    private String memberNo;

    /** 会员姓名 */
    @Excel(name = "会员姓名")
    @Size(max = 64, message = "会员姓名长度不能超过64个字符")
    private String memberName;

    /** 物品ID */
    @Excel(name = "物品ID", cellType = ColumnType.NUMERIC)
    @NotNull(message = "物品ID不能为空")
    private Long goodsId;

    /** 物品名称 */
    @Excel(name = "物品名称")
    @Size(max = 128, message = "物品名称长度不能超过128个字符")
    private String goodsName;

    /** 兑换日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "兑换日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date exchangeDate;

    /** 兑换数量 */
    @Excel(name = "兑换数量", cellType = ColumnType.NUMERIC)
    private Integer quantity;

    /** 扣减积分 */
    @Excel(name = "扣减积分", cellType = ColumnType.NUMERIC)
    @NotNull(message = "扣减积分不能为空")
    private BigDecimal pointsDeducted;

    private BigDecimal actualPoints;

    private BigDecimal currentBalance;

    private BigDecimal newBalance;

    /** 补差价方式(现金/微信/支付宝) */
    @Excel(name = "补差价方式", readConverterExp = "现金=现金,微信=微信,支付宝=支付宝")
    @Size(max = 32, message = "补差价方式长度不能超过32个字符")
    private String paymentMethod;

    /** 补差价金额 */
    @Excel(name = "补差价金额", cellType = ColumnType.NUMERIC)
    private BigDecimal extraAmount;

    /** 状态(0待领取/1已领取/2已取消) */
    @Excel(name = "状态", readConverterExp = "0=待领取,1=已领取,2=已取消")
    private String status;

    /** 领取时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "领取时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date claimTime;

    /** 备注 */
    @Excel(name = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    /** 删除标志 */
    private String delFlag;

    public Long getExchangeId()
    {
        return exchangeId;
    }

    public void setExchangeId(Long exchangeId)
    {
        this.exchangeId = exchangeId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getExchangeNo()
    {
        return exchangeNo;
    }

    public void setExchangeNo(String exchangeNo)
    {
        this.exchangeNo = exchangeNo;
    }

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

    public Long getGoodsId()
    {
        return goodsId;
    }

    public void setGoodsId(Long goodsId)
    {
        this.goodsId = goodsId;
    }

    public String getGoodsName()
    {
        return goodsName;
    }

    public void setGoodsName(String goodsName)
    {
        this.goodsName = goodsName;
    }

    public Date getExchangeDate()
    {
        return exchangeDate;
    }

    public void setExchangeDate(Date exchangeDate)
    {
        this.exchangeDate = exchangeDate;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    public BigDecimal getPointsDeducted()
    {
        return pointsDeducted;
    }

    public void setPointsDeducted(BigDecimal pointsDeducted)
    {
        this.pointsDeducted = pointsDeducted;
    }

    public BigDecimal getActualPoints()
    {
        return actualPoints;
    }

    public void setActualPoints(BigDecimal actualPoints)
    {
        this.actualPoints = actualPoints;
    }

    public BigDecimal getCurrentBalance()
    {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance)
    {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getNewBalance()
    {
        return newBalance;
    }

    public void setNewBalance(BigDecimal newBalance)
    {
        this.newBalance = newBalance;
    }

    public String getPaymentMethod()
    {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod)
    {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getExtraAmount()
    {
        return extraAmount;
    }

    public void setExtraAmount(BigDecimal extraAmount)
    {
        this.extraAmount = extraAmount;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getClaimTime()
    {
        return claimTime;
    }

    public void setClaimTime(Date claimTime)
    {
        this.claimTime = claimTime;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    @Override
    public String toString() {
        return "MemPointsExchange{" +
            "exchangeId=" + exchangeId +
            ", exchangeNo='" + exchangeNo + '\'' +
            ", memberId=" + memberId +
            ", memberNo='" + memberNo + '\'' +
            ", memberName='" + memberName + '\'' +
            ", goodsId=" + goodsId +
            ", goodsName='" + goodsName + '\'' +
            ", exchangeDate=" + exchangeDate +
            ", quantity=" + quantity +
            ", pointsDeducted=" + pointsDeducted +
            ", actualPoints=" + actualPoints +
            ", paymentMethod='" + paymentMethod + '\'' +
            ", extraAmount=" + extraAmount +
            ", status='" + status + '\'' +
            ", claimTime=" + claimTime +
            '}';
    }
}
