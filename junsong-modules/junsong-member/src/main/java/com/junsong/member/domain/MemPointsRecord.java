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
 * 积分记录表对象 mem_points_record
 *
 * @author junsong
 */
public class MemPointsRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @Excel(name = "记录ID", cellType = ColumnType.NUMERIC)
    private Long recordId;

    /** 部门ID */
    private Long deptId;

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

    /** 类型(消费得积分/兑换扣积分/过期清零/手动调整) */
    @Excel(name = "类型", readConverterExp = "消费得积分=消费得积分,兑换扣积分=兑换扣积分,过期清零=过期清零,手动调整=手动调整")
    @NotBlank(message = "类型不能为空")
    @Size(max = 32, message = "类型长度不能超过32个字符")
    private String recordType;

    /** 消费金额 */
    @Excel(name = "消费金额", cellType = ColumnType.NUMERIC)
    private BigDecimal consumeAmount;

    /** 积分变动 */
    @Excel(name = "积分变动", cellType = ColumnType.NUMERIC)
    @NotNull(message = "积分变动不能为空")
    private BigDecimal points;

    /** 变动后余额 */
    @Excel(name = "变动后余额", cellType = ColumnType.NUMERIC)
    private BigDecimal balance;

    /** 使用的规则代码 */
    @Excel(name = "使用的规则代码")
    @Size(max = 64, message = "使用的规则代码长度不能超过64个字符")
    private String ruleCode;

    /** 积分过期日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "积分过期日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expireDate;

    /** 备注 */
    @Excel(name = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    /** 删除标志 */
    private String delFlag;

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

    public String getRecordType()
    {
        return recordType;
    }

    public void setRecordType(String recordType)
    {
        this.recordType = recordType;
    }

    public BigDecimal getConsumeAmount()
    {
        return consumeAmount;
    }

    public void setConsumeAmount(BigDecimal consumeAmount)
    {
        this.consumeAmount = consumeAmount;
    }

    public BigDecimal getPoints()
    {
        return points;
    }

    public void setPoints(BigDecimal points)
    {
        this.points = points;
    }

    public BigDecimal getBalance()
    {
        return balance;
    }

    public void setBalance(BigDecimal balance)
    {
        this.balance = balance;
    }

    public String getRuleCode()
    {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode)
    {
        this.ruleCode = ruleCode;
    }

    public Date getExpireDate()
    {
        return expireDate;
    }

    public void setExpireDate(Date expireDate)
    {
        this.expireDate = expireDate;
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
        return "MemPointsRecord{" +
            "recordId=" + recordId +
            ", memberId=" + memberId +
            ", memberNo='" + memberNo + '\'' +
            ", memberName='" + memberName + '\'' +
            ", recordType='" + recordType + '\'' +
            ", consumeAmount=" + consumeAmount +
            ", points=" + points +
            ", balance=" + balance +
            ", expireDate=" + expireDate +
            '}';
    }
}
