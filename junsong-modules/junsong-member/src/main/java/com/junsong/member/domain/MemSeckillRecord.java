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
 * 秒杀记录对象 mem_seckill_record
 *
 * @author junsong
 */
public class MemSeckillRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @Excel(name = "记录ID", cellType = ColumnType.NUMERIC)
    private Long recordId;

    /** 部门ID */
    private Long deptId;

    /** 秒杀ID */
    @Excel(name = "秒杀ID", cellType = ColumnType.NUMERIC)
    private Long seckillId;

    /** 会员ID */
    @Excel(name = "会员ID", cellType = ColumnType.NUMERIC)
    private Long memberId;

    /** 会员编号 */
    @Excel(name = "会员编号")
    @NotBlank(message = "会员编号不能为空")
    @Size(max = 64, message = "会员编号长度不能超过64个字符")
    private String memberNo;

    /** 会员姓名 */
    @Excel(name = "会员姓名")
    @NotBlank(message = "会员姓名不能为空")
    @Size(max = 64, message = "会员姓名长度不能超过64个字符")
    private String memberName;

    /** 秒杀日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "秒杀日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date seckillDate;

    /** 付款方式 */
    @Excel(name = "付款方式")
    @Size(max = 32, message = "付款方式长度不能超过32个字符")
    private String paymentMethod;

    /** 秒杀份额 */
    @Excel(name = "秒杀份额", cellType = ColumnType.NUMERIC)
    private Integer shares;

    /** 已领取份额（非数据库字段，用于分批领取展示） */
    private Integer claimedShares;

    /** 待领取份额（非数据库字段，用于分批领取展示） */
    private Integer remainingShares;

    /** 总金额 */
    @Excel(name = "总金额", cellType = ColumnType.NUMERIC)
    private BigDecimal totalAmount;

    /** 状态(0待领取/1已领取/2已取消) */
    @Excel(name = "状态", readConverterExp = "0=待领取,1=已领取,2=已取消")
    private String status;

    /** 领取时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "领取时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date claimTime;

    /** 领取人 */
    @Excel(name = "领取人")
    @Size(max = 64, message = "领取人长度不能超过64个字符")
    private String claimBy;

    /** 备注 */
    @Excel(name = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 秒杀活动名称（非数据库字段，用于列表展示） */
    private String seckillName;

    /** 秒杀单价（非数据库字段，用于列表展示） */
    private BigDecimal seckillPrice;

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getSeckillId()
    {
        return seckillId;
    }

    public void setSeckillId(Long seckillId)
    {
        this.seckillId = seckillId;
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

    public Date getSeckillDate()
    {
        return seckillDate;
    }

    public void setSeckillDate(Date seckillDate)
    {
        this.seckillDate = seckillDate;
    }

    public String getPaymentMethod()
    {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod)
    {
        this.paymentMethod = paymentMethod;
    }

    public Integer getShares()
    {
        return shares;
    }

    public void setShares(Integer shares)
    {
        this.shares = shares;
    }

    public Integer getClaimedShares()
    {
        return claimedShares;
    }

    public void setClaimedShares(Integer claimedShares)
    {
        this.claimedShares = claimedShares;
    }

    public Integer getRemainingShares()
    {
        return remainingShares;
    }

    public void setRemainingShares(Integer remainingShares)
    {
        this.remainingShares = remainingShares;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
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

    public String getClaimBy()
    {
        return claimBy;
    }

    public void setClaimBy(String claimBy)
    {
        this.claimBy = claimBy;
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

    public String getSeckillName()
    {
        return seckillName;
    }

    public void setSeckillName(String seckillName)
    {
        this.seckillName = seckillName;
    }

    public BigDecimal getSeckillPrice()
    {
        return seckillPrice;
    }

    public void setSeckillPrice(BigDecimal seckillPrice)
    {
        this.seckillPrice = seckillPrice;
    }

    @Override
    public String toString() {
        return "MemSeckillRecord{" +
            "recordId=" + recordId +
            ", seckillId=" + seckillId +
            ", memberId=" + memberId +
            ", memberNo='" + memberNo + '\'' +
            ", memberName='" + memberName + '\'' +
            ", seckillDate=" + seckillDate +
            ", shares=" + shares +
            ", claimedShares=" + claimedShares +
            ", remainingShares=" + remainingShares +
            ", totalAmount=" + totalAmount +
            ", status='" + status + '\'' +
            ", claimTime=" + claimTime +
            ", claimBy='" + claimBy + '\'' +
            '}';
    }
}
