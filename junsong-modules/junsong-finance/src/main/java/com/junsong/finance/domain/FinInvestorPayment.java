package com.junsong.finance.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsong.common.core.annotation.Excel;
import com.junsong.common.core.annotation.Excel.ColumnType;
import com.junsong.common.core.web.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 投资人返款对象 fin_investor_payment
 * 
 * @author junsong
 */
public class FinInvestorPayment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 返款记录ID */
    @Excel(name = "返款记录ID", cellType = ColumnType.NUMERIC)
    private Long paymentId;

    /** 部门ID */
    private Long deptId;

    /** 周期ID */
    private Long periodId;

    /** 分润单ID */
    private Long shareId;

    /** 分润明细ID */
    private Long shareDetailId;

    /** 投资人ID */
    private Long investorId;

    /** 返款单号 */
    @Excel(name = "返款单号")
    private String paymentNo;

    /** 日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "日期", width = 30, dateFormat = "yyyy-MM-dd")
    @NotNull(message = "日期不能为空")
    private Date paymentDate;

    /** 类型(返款) */
    @Excel(name = "类型")
    @NotNull(message = "类型不能为空")
    private String paymentType;

    /** 投资人姓名 */
    @Excel(name = "投资人姓名")
    private String investorName;

    /** 金额 */
    @Excel(name = "金额", cellType = ColumnType.NUMERIC)
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    /** 来源类型（0手工返款 1结转分润自动返款） */
    @Excel(name = "来源类型", readConverterExp = "0=手工返款,1=结转分润自动返款")
    private String sourceType;

    /** 返款状态（0待返款 1已返款） */
    @Excel(name = "返款状态", readConverterExp = "0=待返款,1=已返款")
    private String paymentStatus;

    /** 本次投资占比 */
    @Excel(name = "本次投资占比", cellType = ColumnType.NUMERIC)
    private BigDecimal investRatio;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    public Long getPaymentId()
    {
        return paymentId;
    }

    public void setPaymentId(Long paymentId)
    {
        this.paymentId = paymentId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getPeriodId()
    {
        return periodId;
    }

    public void setPeriodId(Long periodId)
    {
        this.periodId = periodId;
    }

    public Long getShareId()
    {
        return shareId;
    }

    public void setShareId(Long shareId)
    {
        this.shareId = shareId;
    }

    public Long getShareDetailId()
    {
        return shareDetailId;
    }

    public void setShareDetailId(Long shareDetailId)
    {
        this.shareDetailId = shareDetailId;
    }

    public Long getInvestorId()
    {
        return investorId;
    }

    public void setInvestorId(Long investorId)
    {
        this.investorId = investorId;
    }

    @Size(min = 0, max = 64, message = "返款单号长度不能超过64个字符")
    public String getPaymentNo()
    {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo)
    {
        this.paymentNo = paymentNo;
    }

    public Date getPaymentDate()
    {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate)
    {
        this.paymentDate = paymentDate;
    }

    @Size(min = 0, max = 32, message = "类型长度不能超过32个字符")
    public String getPaymentType()
    {
        return paymentType;
    }

    public void setPaymentType(String paymentType)
    {
        this.paymentType = paymentType;
    }

    @Size(min = 0, max = 64, message = "投资人姓名长度不能超过64个字符")
    public String getInvestorName()
    {
        return investorName;
    }

    public void setInvestorName(String investorName)
    {
        this.investorName = investorName;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public String getPaymentStatus()
    {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus)
    {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getInvestRatio()
    {
        return investRatio;
    }

    public void setInvestRatio(BigDecimal investRatio)
    {
        this.investRatio = investRatio;
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
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("paymentId", getPaymentId())
            .append("deptId", getDeptId())
            .append("periodId", getPeriodId())
            .append("shareId", getShareId())
            .append("shareDetailId", getShareDetailId())
            .append("investorId", getInvestorId())
            .append("paymentNo", getPaymentNo())
            .append("paymentDate", getPaymentDate())
            .append("paymentType", getPaymentType())
            .append("investorName", getInvestorName())
            .append("amount", getAmount())
            .append("sourceType", getSourceType())
            .append("paymentStatus", getPaymentStatus())
            .append("investRatio", getInvestRatio())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
